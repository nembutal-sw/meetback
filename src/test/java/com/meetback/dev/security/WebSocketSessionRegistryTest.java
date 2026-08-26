package com.meetback.dev.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WebSocketSessionRegistryTest {

    @Mock
    private WebSocketSession firstSession;

    @Mock
    private WebSocketSession secondSession;

    @Mock
    private WebSocketSession otherSession;

    @Test
    void closesEverySessionBoundToSuspendedUser() throws IOException {
        WebSocketSessionRegistry registry = new WebSocketSessionRegistry();
        register(registry, firstSession, "session-1", 10L);
        register(registry, secondSession, "session-2", 10L);
        register(registry, otherSession, "session-3", 20L);
        when(firstSession.isOpen()).thenReturn(true);
        when(secondSession.isOpen()).thenReturn(true);

        registry.closeUserSessions(10L);

        assertSuspensionClose(firstSession);
        assertSuspensionClose(secondSession);
        verify(otherSession, never()).close();
    }

    @Test
    void continuesClosingSessionsAfterRuntimeFailure() throws IOException {
        WebSocketSessionRegistry registry = new WebSocketSessionRegistry();
        register(registry, firstSession, "session-1", 10L);
        register(registry, secondSession, "session-2", 10L);
        when(firstSession.isOpen()).thenReturn(true);
        when(secondSession.isOpen()).thenReturn(true);
        doThrow(new IllegalStateException("close failed"))
                .when(firstSession)
                .close(any(CloseStatus.class));

        registry.closeUserSessions(10L);

        verify(firstSession).close(any(CloseStatus.class));
        verify(secondSession).close(any(CloseStatus.class));
    }

    private void register(
            WebSocketSessionRegistry registry,
            WebSocketSession session,
            String sessionId,
            Long userId
    ) {
        when(session.getId()).thenReturn(sessionId);
        registry.register(session);
        registry.bindUser(sessionId, userId);
    }

    private void assertSuspensionClose(WebSocketSession session)
            throws IOException {
        ArgumentCaptor<CloseStatus> statusCaptor =
                ArgumentCaptor.forClass(CloseStatus.class);
        verify(session).close(statusCaptor.capture());

        CloseStatus status = statusCaptor.getValue();
        assertThat(status.getCode()).isEqualTo(1008);
        assertThat(status.getReason()).isEqualTo("ACCOUNT_SUSPENDED");
    }
}
