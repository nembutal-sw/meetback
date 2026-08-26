package com.meetback.dev.security;

import com.meetback.dev.event.UserCredentialsChangedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class UserCredentialsChangedWebSocketListener {

    private final WebSocketSessionRegistry sessionRegistry;

    // DB 변경이 확정된 뒤 대상 사용자의 연결만 종료한다.
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void closeSessions(UserCredentialsChangedEvent event) {
        sessionRegistry.closeUserSessions(
                event.userId(),
                "ACCOUNT_CREDENTIALS_CHANGED"
        );
    }
}
