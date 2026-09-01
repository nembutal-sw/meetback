package com.meetback.dev.WebSocket;

import com.meetback.dev.realtime.gateway.LocalSessionControl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.WebSocketHandlerDecorator;
import org.springframework.web.socket.handler.WebSocketHandlerDecoratorFactory;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class WebSocketSessionControl
        implements LocalSessionControl,
        WebSocketHandlerDecoratorFactory {

    public static final int AUTH_INVALIDATED_CLOSE_CODE = 4001;

    private static final Logger log =
            LoggerFactory.getLogger(
                    WebSocketSessionControl.class
            );

    private final Map<String, TrackedSession> sessions =
            new ConcurrentHashMap<>();

    @Override
    public WebSocketHandler decorate(
            WebSocketHandler handler
    ) {
        return new WebSocketHandlerDecorator(handler) {

            @Override
            public void afterConnectionEstablished(
                    WebSocketSession session
            ) throws Exception {
                sessions.put(
                        session.getId(),
                        new TrackedSession(session)
                );

                super.afterConnectionEstablished(session);
            }

            @Override
            public void afterConnectionClosed(
                    WebSocketSession session,
                    CloseStatus closeStatus
            ) throws Exception {
                try {
                    super.afterConnectionClosed(
                            session,
                            closeStatus
                    );
                }
                finally {
                    sessions.remove(session.getId());
                }
            }
        };
    }

    public void authenticate(
            String sessionId,
            Long userId,
            Integer tokenVersion
    ) {
        if (
                sessionId == null
                        || userId == null
                        || tokenVersion == null
        ) {
            return;
        }

        TrackedSession tracked = sessions.get(sessionId);

        if (tracked == null) {
            log.warn(
                    "인증할 WebSocket 세션을 찾을 수 없습니다. sessionId={}",
                    sessionId
            );
            return;
        }

        tracked.userId = userId;
        tracked.tokenVersion = tokenVersion;
    }

    public void subscribeMeeting(
            String sessionId,
            Long meetingId
    ) {
        if (sessionId == null || meetingId == null) {
            return;
        }

        TrackedSession tracked = sessions.get(sessionId);

        if (tracked != null) {
            tracked.meetingIds.add(meetingId);
        }
    }

    @Override
    public void disconnectMeetingUser(
            Long meetingId,
            Long userId,
            String reason
    ) {
        sessions.values().stream()
                .filter(session -> userId.equals(session.userId))
                .filter(session -> session.meetingIds.contains(meetingId))
                .forEach(session -> close(
                        session,
                        new CloseStatus(
                                4002,
                                "MEETING_ACCESS_REVOKED"
                        )
                ));
    }

    @Override
    public void disconnectUserBeforeTokenVersion(
            Long userId,
            Integer minimumValidTokenVersion,
            String reason
    ) {
        if (userId == null || minimumValidTokenVersion == null) {
            return;
        }

        CloseStatus closeStatus =
                new CloseStatus(
                        AUTH_INVALIDATED_CLOSE_CODE,
                        "AUTH_INVALIDATED:"
                                + safeReason(reason)
                );

        sessions.values().stream()
                .filter(session -> userId.equals(session.userId))
                .filter(session -> session.tokenVersion != null)
                .filter(session ->
                        session.tokenVersion
                                < minimumValidTokenVersion
                )
                .forEach(session -> close(session, closeStatus));
    }

    private void close(
            TrackedSession tracked,
            CloseStatus closeStatus
    ) {
        try {
            if (tracked.session.isOpen()) {
                tracked.session.close(closeStatus);
            }
        }
        catch (IOException exception) {
            log.warn(
                    "WebSocket 세션 종료에 실패했습니다. sessionId={}",
                    tracked.session.getId(),
                    exception
            );
        }
    }

    private String safeReason(
            String reason
    ) {
        if (reason == null || reason.isBlank()) {
            return "UNKNOWN";
        }

        return reason.replaceAll(
                "[^A-Z0-9_]",
                "_"
        );
    }

    private static final class TrackedSession {

        private final WebSocketSession session;
        private final Set<Long> meetingIds =
                ConcurrentHashMap.newKeySet();

        private volatile Long userId;
        private volatile Integer tokenVersion;

        private TrackedSession(
                WebSocketSession session
        ) {
            this.session = session;
        }
    }
}
