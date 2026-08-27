package com.meetback.dev.security;

import com.meetback.dev.domain.User;
import com.meetback.dev.domain.UserStatus;
import com.meetback.dev.repository.ParticipantMapper;
import com.meetback.dev.repository.UserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.access.AccessDeniedException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtChannelInterceptorTest {

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private ParticipantMapper participantMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private WebSocketSessionRegistry sessionRegistry;

    @InjectMocks
    private JwtChannelInterceptor interceptor;

    @Test
    void bindsSessionBeforeDbValidationAndUnregistersOnFailure() {
        User suspendedUser = new User();
        suspendedUser.setUserId(5L);
        suspendedUser.setRole("USER");
        suspendedUser.setStatus(UserStatus.SUSPENDED);
        suspendedUser.setTokenVersion(3);

        when(jwtProvider.validateToken("access-token")).thenReturn(true);
        when(jwtProvider.getTokenType("access-token")).thenReturn("ACCESS");
        when(jwtProvider.getUserId("access-token")).thenReturn(5L);
        when(jwtProvider.getRole("access-token")).thenReturn("USER");
        when(jwtProvider.getTokenVersion("access-token")).thenReturn(3);
        when(userMapper.selectById(5L)).thenReturn(suspendedUser);

        assertThatThrownBy(() -> interceptor.preSend(
                connectMessage(),
                null
        ))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("ACCOUNT_SUSPENDED");

        InOrder order = inOrder(sessionRegistry, userMapper);
        order.verify(sessionRegistry).bindUser("session-1", 5L);
        order.verify(userMapper).selectById(5L);
        order.verify(sessionRegistry).unregister("session-1");
    }

    private Message<byte[]> connectMessage() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(
                StompCommand.CONNECT
        );
        accessor.setSessionId("session-1");
        accessor.setNativeHeader(
                "Authorization",
                "Bearer access-token"
        );
        accessor.setLeaveMutable(true);

        return MessageBuilder.createMessage(
                new byte[0],
                accessor.getMessageHeaders()
        );
    }
}
