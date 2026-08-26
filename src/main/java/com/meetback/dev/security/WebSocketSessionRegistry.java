package com.meetback.dev.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
@Component
public class WebSocketSessionRegistry {

    private final ConcurrentMap<String, WebSocketSession> sessions =
            new ConcurrentHashMap<>();

    private final ConcurrentMap<String, Long> sessionUsers =
            new ConcurrentHashMap<>();

    private final ConcurrentMap<Long, Set<String>> userSessions =
            new ConcurrentHashMap<>();

    public void register(WebSocketSession session) {
        sessions.put(session.getId(), session);
    }

    public void bindUser(String sessionId, Long userId) {
        if (sessionId == null || userId == null) {
            return;
        }

        Long previousUserId = sessionUsers.put(sessionId, userId);
        if (previousUserId != null && !previousUserId.equals(userId)) {
            removeUserSession(previousUserId, sessionId);
        }

        userSessions
                .computeIfAbsent(
                        userId,
                        ignored -> ConcurrentHashMap.newKeySet()
                )
                .add(sessionId);
    }

    public void unregister(String sessionId) {
        sessions.remove(sessionId);

        Long userId = sessionUsers.remove(sessionId);
        if (userId != null) {
            removeUserSession(userId, sessionId);
        }
    }

    // 정지된 사용자의 현재 연결을 서버에서 즉시 종료한다.
    public void closeUserSessions(Long userId) {
        closeUserSessions(userId, "ACCOUNT_SUSPENDED");
    }

    public void closeUserSessions(Long userId, String reason) {
        Set<String> sessionIds = userSessions.remove(userId);
        if (sessionIds == null) {
            return;
        }

        for (String sessionId : Set.copyOf(sessionIds)) {
            WebSocketSession session = sessions.get(sessionId);

            try {
                if (session != null && session.isOpen()) {
                    session.close(
                            CloseStatus.POLICY_VIOLATION.withReason(
                                    reason
                            )
                    );
                }
            } catch (Exception e) {
                log.warn("WebSocket 세션 종료 실패: {}", sessionId, e);
            } finally {
                unregister(sessionId);
            }
        }
    }

    private void removeUserSession(Long userId, String sessionId) {
        userSessions.computeIfPresent(
                userId,
                (ignored, sessionIds) -> {
                    sessionIds.remove(sessionId);
                    return sessionIds.isEmpty() ? null : sessionIds;
                }
        );
    }
}
