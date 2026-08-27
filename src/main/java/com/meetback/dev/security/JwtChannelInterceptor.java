package com.meetback.dev.security;

import com.meetback.dev.domain.User;
import com.meetback.dev.domain.UserStatus;
import com.meetback.dev.repository.ParticipantMapper;
import com.meetback.dev.repository.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class JwtChannelInterceptor implements ChannelInterceptor {

    private static final String TOKEN_VERSION_ATTRIBUTE =
            "authenticatedTokenVersion";

    private final JwtProvider jwtProvider;
    private final ParticipantMapper participantMapper;
    private final UserMapper userMapper;
    private final WebSocketSessionRegistry sessionRegistry;

    @Override
    public Message<?> preSend(
            Message<?> message,
            MessageChannel channel
    ) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(
                message,
                StompHeaderAccessor.class
        );

        if (accessor == null) {
            return message;
        }

        StompCommand command = accessor.getCommand();
        if (StompCommand.CONNECT.equals(command)) {
            authenticateConnection(accessor);
        }

        // 연결 후에도 매 요청에서 현재 계정 상태를 다시 확인한다.
        if (StompCommand.SEND.equals(command)
                || StompCommand.SUBSCRIBE.equals(command)) {
            validateConnectedUser(accessor);
        }

        if (StompCommand.SUBSCRIBE.equals(command)) {
            validateChatSubscription(accessor);
        }

        return message;
    }

    private void authenticateConnection(
            StompHeaderAccessor accessor
    ) {
        String authorization = accessor.getFirstNativeHeader(
                "Authorization"
        );

        if (authorization == null
                || !authorization.startsWith("Bearer ")) {
            throw new BadCredentialsException("Access Token이 필요합니다.");
        }

        String token = authorization.substring(7);
        if (!jwtProvider.validateToken(token)) {
            throw new BadCredentialsException("유효하지 않은 토큰입니다.");
        }
        if (!"ACCESS".equals(jwtProvider.getTokenType(token))) {
            throw new BadCredentialsException("Access Token만 사용할 수 있습니다.");
        }

        Long userId = jwtProvider.getUserId(token);
        String tokenRole = jwtProvider.getRole(token);
        Integer tokenVersion = jwtProvider.getTokenVersion(token);
        if (userId == null
                || tokenRole == null
                || tokenRole.isBlank()
                || tokenVersion == null) {
            throw new BadCredentialsException("사용자 정보를 확인할 수 없습니다.");
        }

        String sessionId = accessor.getSessionId();
        if (sessionId == null || sessionId.isBlank()) {
            throw new BadCredentialsException("WebSocket 세션을 확인할 수 없습니다.");
        }

        // DB 검사 전에 연결을 묶어 정지 커밋과의 경쟁 구간을 없앤다.
        sessionRegistry.bindUser(sessionId, userId);

        try {
            User user = userMapper.selectById(userId);
            validateUserState(user, tokenVersion, tokenRole);

            AuthenticatedUser principal = new AuthenticatedUser(
                    user.getUserId(),
                    user.getRole()
            );
            String authority = toAuthority(user.getRole());
            Authentication authentication =
                    new UsernamePasswordAuthenticationToken(
                            principal,
                            null,
                            List.of(new SimpleGrantedAuthority(authority))
                    );

            accessor.setUser(authentication);
            sessionAttributes(accessor).put(
                    TOKEN_VERSION_ATTRIBUTE,
                    tokenVersion
            );
        } catch (RuntimeException | Error e) {
            sessionRegistry.unregister(sessionId);
            throw e;
        }
    }

    private void validateConnectedUser(
            StompHeaderAccessor accessor
    ) {
        AuthenticatedUser principal = authenticatedUser(accessor);
        Object versionValue = sessionAttributes(accessor).get(
                TOKEN_VERSION_ATTRIBUTE
        );

        if (!(versionValue instanceof Number number)) {
            throw new BadCredentialsException("WebSocket 인증 정보를 확인할 수 없습니다.");
        }

        User user = userMapper.selectById(principal.userId());
        validateUserState(
                user,
                number.intValue(),
                principal.role()
        );
    }

    private void validateUserState(
            User user,
            Integer authenticatedVersion,
            String authenticatedRole
    ) {
        if (user == null) {
            throw new BadCredentialsException("사용자 정보를 확인할 수 없습니다.");
        }

        // 정지는 버전 불일치보다 먼저 판별해 명확한 사유를 전달한다.
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new AccessDeniedException("ACCOUNT_SUSPENDED");
        }

        if (user.getTokenVersion() == null
                || !authenticatedVersion.equals(user.getTokenVersion())) {
            throw new BadCredentialsException("현재 로그인 세션이 만료되었습니다.");
        }

        if (user.getRole() == null
                || user.getRole().isBlank()
                || !authenticatedRole.equals(user.getRole())) {
            throw new BadCredentialsException("사용자 권한 정보가 변경되었습니다.");
        }

        if (isWithdrawalExpired(user)) {
            throw new BadCredentialsException("탈퇴 처리된 계정입니다.");
        }
    }

    private void validateChatSubscription(
            StompHeaderAccessor accessor
    ) {
        String destination = accessor.getDestination();
        if (destination == null
                || !destination.startsWith("/topic/meetings")) {
            return;
        }

        Long meetingId = extractMeetingId(destination);
        AuthenticatedUser user = authenticatedUser(accessor);
        int participantCount = participantMapper
                .countParticipantByMeetingAndUser(
                        meetingId,
                        user.userId()
                );

        if (participantCount == 0) {
            throw new AccessDeniedException(
                    "해당 모임의 참가자만 채팅을 구독할 수 있습니다."
            );
        }
    }

    private AuthenticatedUser authenticatedUser(
            StompHeaderAccessor accessor
    ) {
        if (!(accessor.getUser() instanceof Authentication authentication)) {
            throw new AccessDeniedException("WebSocket 인증 정보를 확인할 수 없습니다.");
        }

        if (!(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            throw new AccessDeniedException("인증 사용자 정보를 확인할 수 없습니다.");
        }

        return user;
    }

    private Map<String, Object> sessionAttributes(
            StompHeaderAccessor accessor
    ) {
        Map<String, Object> attributes = accessor.getSessionAttributes();
        if (attributes == null) {
            attributes = new HashMap<>();
            accessor.setSessionAttributes(attributes);
        }
        return attributes;
    }

    private boolean isWithdrawalExpired(User user) {
        if (user.getDeletedAt() == null) {
            return false;
        }

        return !LocalDateTime.now().isBefore(
                user.getDeletedAt().plusDays(7)
        );
    }

    private String toAuthority(String role) {
        return role.startsWith("ROLE_") ? role : "ROLE_" + role;
    }

    // /topic/meetings/{meetingId}/chat 경로에서 모임 ID를 읽는다.
    private Long extractMeetingId(String destination) {
        String prefix = "/topic/meetings/";
        String suffix = "/chat";

        if (!destination.startsWith(prefix)
                || !destination.endsWith(suffix)) {
            throw new AccessDeniedException("올바르지 않은 채팅 구독 경로입니다.");
        }

        String meetingIdText = destination.substring(
                prefix.length(),
                destination.length() - suffix.length()
        );

        try {
            return Long.valueOf(meetingIdText);
        } catch (NumberFormatException e) {
            throw new AccessDeniedException("올바르지 않은 모임 ID입니다.");
        }
    }
}
