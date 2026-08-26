package com.meetback.dev.security;

import com.meetback.dev.event.UserSuspendedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class UserSuspendedWebSocketListener {

    private final WebSocketSessionRegistry sessionRegistry;

    // DB 정지가 커밋된 경우에만 실제 연결을 종료한다.
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void closeSuspendedUserSessions(UserSuspendedEvent event) {
        sessionRegistry.closeUserSessions(event.userId());
    }
}
