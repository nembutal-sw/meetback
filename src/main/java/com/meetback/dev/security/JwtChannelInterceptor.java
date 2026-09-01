package com.meetback.dev.security;

import com.meetback.dev.WebSocket.WebSocketSessionControl;
import com.meetback.dev.repository.ParticipantMapper;
import com.meetback.dev.security.AuthenticatedUser;
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
import com.meetback.dev.domain.User;
import com.meetback.dev.repository.UserMapper;

import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtChannelInterceptor implements ChannelInterceptor {

    private final JwtProvider jwtProvider;
    private final ParticipantMapper participantMapper;
    private final UserMapper userMapper;
    private final WebSocketSessionControl webSocketSessionControl;

    @Override
    public Message<?> preSend(
            Message<?> message,
            MessageChannel channel
    )
    {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(
                message,
                StompHeaderAccessor.class
        );

        if(accessor == null)
        {
            return message;
        }

        // =========================================================
        // 1. WebSocket CONNECT 인증
        // =========================================================
        if(StompCommand.CONNECT.equals(
                accessor.getCommand()
        ))
        {
            String authorization = accessor.getFirstNativeHeader(
                    "Authorization"
            );

            if(authorization == null
                || !authorization.startsWith("Bearer "))
            {
                throw new BadCredentialsException(
                        "Access Token이 필요합니다."
                );
            }

            String token = authorization.substring(7);

            if(!jwtProvider.validateToken(token))
            {
                throw new BadCredentialsException(
                        "유효하지 않은 토큰입니다."
                );
            }

            if(!"ACCESS".equals(
                    jwtProvider.getTokenType(token)
            ))
            {
                throw new BadCredentialsException(
                        "Access Token만 사용할 수 있습니다."
                );
            }

            Long userId = jwtProvider.getUserId(token);

            String role = jwtProvider.getRole(token);

            Integer tokenVersion = jwtProvider.getTokenVersion(token);

            if(userId == null
                    || role == null
                    || role.isBlank()
                    || tokenVersion == null)
            {
                throw new BadCredentialsException(
                        "사용자 정보를 확인할 수 없습니다."
                );
            }

            User currentUser =
                    userMapper.selectById(userId);

            if(currentUser == null
                    || currentUser.getTokenVersion() == null
                    || !tokenVersion.equals(
                    currentUser.getTokenVersion()
            ))
            {
                throw new BadCredentialsException(
                        "이미 무효화된 로그인입니다."
                );
            }

            AuthenticatedUser user = new AuthenticatedUser(
                    userId,
                    role,
                    tokenVersion
            );

            String authority = role.startsWith("ROLE_") ? role : "ROLE_" + role;

            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    user,
                    null,
                    List.of(
                            new SimpleGrantedAuthority(
                                    authority
                            )
                    )
            );

            accessor.setUser(authentication);

            webSocketSessionControl.authenticate(
                    accessor.getSessionId(),
                    userId,
                    tokenVersion
            );
        }

        // =========================================================
        // 2. ★ 채팅방 SUBSCRIBE 권한 확인
        //
        // /topic/meetings/{meetingId}/chat
        //
        // 로그인했다고 아무 모임 채팅이나 구독할 수 있는 게 아니라
        // 해당 모임 참가자여야 한다.
        // =========================================================

        if(StompCommand.SUBSCRIBE.equals(
                accessor.getCommand()
        ))
        {
            String destination = accessor.getDestination();

           /*
            * MeetBack 모임 채팅 Topic만 검사
            */

            if(
                    destination != null
                    && destination.startsWith("/topic/meetings")
            )
            {
                Long meetingId = extractMeetingId(
                        destination
                );

                Authentication authentication;

                if(
                        accessor.getUser()
                        instanceof Authentication auth
                )
                {
                    authentication = auth;
                }
                else
                {
                    throw new AccessDeniedException(
                            "WebSocket 인증 정보를 확인할 수 없습니다."
                    );
                }

                Object principalObject =
                        authentication.getPrincipal();


                if (!(
                        principalObject
                                instanceof AuthenticatedUser user
                ))
                {
                    throw new AccessDeniedException(
                            "인증 사용자 정보를 확인할 수 없습니다."
                    );
                }

                int participantCount =
                        participantMapper
                                .countParticipantByMeetingAndUser(
                                        meetingId,
                                        user.userId()
                                );


                if (participantCount == 0)
                {
                    throw new AccessDeniedException(
                            "해당 모임의 참가자만 채팅을 구독할 수 있습니다."
                    );
                }

                webSocketSessionControl.subscribeMeeting(
                        accessor.getSessionId(),
                        meetingId
                );

            }
        }

        return message;
    }
    // =============================================================
    // /topic/meetings/{meetingId}/chat
    //
    // 여기에서 meetingId 추출
    // =============================================================
    private Long extractMeetingId(
            String destination
    )
    {
        String prefix =
                "/topic/meetings/";

        String suffix =
                "/chat";


        if (
                !destination.startsWith(
                        prefix
                )
                        || !destination.endsWith(
                        suffix
                )
        )
        {
            throw new AccessDeniedException(
                    "올바르지 않은 채팅 구독 경로입니다."
            );
        }


        String meetingIdText =
                destination.substring(
                        prefix.length(),
                        destination.length()
                                - suffix.length()
                );


        try
        {
            return Long.valueOf(
                    meetingIdText
            );
        }
        catch (NumberFormatException e)
        {
            throw new AccessDeniedException(
                    "올바르지 않은 모임 ID입니다."
            );
        }
    }

}
