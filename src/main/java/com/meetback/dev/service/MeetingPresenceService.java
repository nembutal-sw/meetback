package com.meetback.dev.service;

import com.meetback.dev.realtime.config.RedisRealtimeProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.time.Duration;

@Service
public class MeetingPresenceService {

    /*
     * Redis 연결 정보는 60초 동안 유효합니다.
     * 현재 연결 중인 세션은 20초마다 만료시간을 갱신합니다.
     */
    private static final long PRESENCE_TIMEOUT_MILLIS =
            60_000L;

    private static final long PRESENCE_REFRESH_MILLIS =
            20_000L;

    /*
     * 현재 서버가 직접 관리하는 WebSocket 세션입니다.
     *
     * disconnect 이벤트가 발생했을 때
     * meetingId와 userId를 찾기 위해 필요합니다.
     */
    private final Map<String, SessionPresence> sessions =
            new HashMap<>();

    /*
     * Redis를 사용하지 않는 환경의 기존 fallback입니다.
     */
    private final Map<PresenceKey, Integer> connectionCounts = new HashMap<>();
    private final Map<Long, ActiveRoomSession> activeSessions = new HashMap<>();

    private final ObjectProvider<StringRedisTemplate>
            redisTemplateProvider;

    private final RedisRealtimeProperties redisProperties;

    private final String serverInstanceId;

    public MeetingPresenceService(
            ObjectProvider<StringRedisTemplate> redisTemplateProvider,
            RedisRealtimeProperties redisProperties,
            @Qualifier("serverInstanceId")
            String serverInstanceId
    ) {
        this.redisTemplateProvider =
                redisTemplateProvider;

        this.redisProperties =
                redisProperties;

        this.serverInstanceId =
                serverInstanceId;
    }

    public synchronized PresenceChange connect(
            String sessionId,
            Long meetingId,
            Long userId
    ) {
        /*
         * 같은 WebSocket 세션에서 중복 SUBSCRIBE가 발생한 경우
         * 연결 개수를 다시 증가시키지 않습니다.
         */
        if (sessions.containsKey(sessionId)) {
            return null;
        }

        SessionPresence session =
                new SessionPresence(
                        meetingId,
                        userId
                );

        sessions.put(
                sessionId,
                session
        );

        if (redisProperties.isEnabled()) {
            boolean wasOnline =
                    isOnlineInRedis(
                            meetingId,
                            userId
                    );

            addRedisSession(
                    sessionId,
                    meetingId,
                    userId
            );

            /*
             * 다른 서버에도 기존 연결이 없었던 경우에만
             * OFF -> ON 이벤트를 발행합니다.
             */
            if (!wasOnline) {
                return new PresenceChange(
                        meetingId,
                        userId,
                        true
                );
            }

            return null;
        }

        return connectLocal(
                meetingId,
                userId
        );
    }

    public synchronized PresenceChange disconnect(
            String sessionId
    ) {
        SessionPresence session =
                sessions.remove(
                        sessionId
                );

        if (session == null) {
            return null;
        }

        if (redisProperties.isEnabled()) {
            StringRedisTemplate redisTemplate =
                    requiredRedisTemplate();

            String key =
                    presenceKey(
                            session.meetingId(),
                            session.userId()
                    );

            redisTemplate
                    .opsForZSet()
                    .remove(
                            key,
                            redisMember(sessionId)
                    );

            removeExpiredSessions(
                    redisTemplate,
                    key
            );

            Long remaining =
                    redisTemplate
                            .opsForZSet()
                            .size(key);

            /*
             * 모든 서버와 모든 브라우저 탭의 연결이 종료됨
             */
            if (
                    remaining == null
                            ||
                            remaining <= 0
            ) {
                redisTemplate.delete(key);

                return new PresenceChange(
                        session.meetingId(),
                        session.userId(),
                        false
                );
            }

            return null;
        }

        return disconnectLocal(
                session
        );
    }

    public synchronized boolean isOnline(
            Long meetingId,
            Long userId
    ) {
        if (redisProperties.isEnabled()) {
            return isOnlineInRedis(
                    meetingId,
                    userId
            );
        }

        PresenceKey key =
                new PresenceKey(
                        meetingId,
                        userId
                );

        return connectionCounts
                .getOrDefault(
                        key,
                        0
                )
                > 0;
    }

    /*
     * 가장 최근에 모임을 구독한 WebSocket 세션을
     * 활성 세션으로 지정합니다.
     */
    public synchronized ActiveRoomSession claimActiveSession(
            String sessionId,
            Long meetingId,
            Long userId
    ) {
        if (
                sessionId == null
                        ||
                        meetingId == null
                        ||
                        userId == null
        ) {
            throw new IllegalArgumentException(
                    "활성 세션 정보가 올바르지 않습니다."
            );
        }

        String sessionMember =
                redisMember(
                        sessionId
                );

        ActiveRoomSession newSession =
                new ActiveRoomSession(
                        meetingId,
                        sessionMember
                );

        if (redisProperties.isEnabled()) {
            StringRedisTemplate redisTemplate =
                    requiredRedisTemplate();

            String previousValue =
                    redisTemplate
                            .opsForValue()
                            .getAndSet(
                                    activeSessionKey(
                                            userId
                                    ),
                                    activeSessionValue(
                                            newSession
                                    )
                            );

            redisTemplate.expire(
                    activeSessionKey(
                            userId
                    ),
                    Duration.ofMillis(
                            PRESENCE_TIMEOUT_MILLIS
                    )
            );

            ActiveRoomSession previousSession =
                    parseActiveSession(
                            previousValue
                    );

            if (
                    newSession.equals(
                            previousSession
                    )
            ) {
                return null;
            }

            return previousSession;
        }

        ActiveRoomSession previousSession =
                activeSessions.put(
                        userId,
                        newSession
                );

        if (
                newSession.equals(
                        previousSession
                )
        ) {
            return null;
        }

        return previousSession;
    }

    /*
     * 메시지를 전송한 세션이 가장 최근 탭인지 확인합니다.
     */
    public synchronized boolean isActiveSession(
            String sessionId,
            Long meetingId,
            Long userId
    ) {
        if (
                sessionId == null
                        ||
                        meetingId == null
                        ||
                        userId == null
        ) {
            return false;
        }

        ActiveRoomSession requestedSession =
                new ActiveRoomSession(
                        meetingId,
                        redisMember(
                                sessionId
                        )
                );

        if (redisProperties.isEnabled()) {
            String savedValue =
                    requiredRedisTemplate()
                            .opsForValue()
                            .get(
                                    activeSessionKey(
                                            userId
                                    )
                            );

            return activeSessionValue(
                    requestedSession
            ).equals(
                    savedValue
            );
        }

        return requestedSession.equals(
                activeSessions.get(
                        userId
                )
        );
    }

    private String activeSessionValue(
            ActiveRoomSession session
    ) {
        return session.meetingId()
                + "|"
                + session.sessionMember();
    }

    private ActiveRoomSession parseActiveSession(
            String value
    ) {
        if (
                value == null
                        ||
                        value.isBlank()
        ) {
            return null;
        }

        int separatorIndex =
                value.indexOf("|");

        if (
                separatorIndex <= 0
                        ||
                        separatorIndex >= value.length() - 1
        ) {
            return null;
        }

        try {
            Long meetingId =
                    Long.valueOf(
                            value.substring(
                                    0,
                                    separatorIndex
                            )
                    );

            String sessionMember =
                    value.substring(
                            separatorIndex + 1
                    );

            return new ActiveRoomSession(
                    meetingId,
                    sessionMember
            );
        }
        catch (NumberFormatException exception) {
            return null;
        }
    }

    /*
     * 현재 서버에 연결된 WebSocket 세션의 Redis 만료시간을
     * 20초마다 연장합니다.
     *
     * 서버가 비정상 종료되면 더 이상 갱신되지 않으므로
     * 최대 60초 뒤에는 오래된 세션으로 판단됩니다.
     */
    @Scheduled(
            fixedDelay = PRESENCE_REFRESH_MILLIS
    )
    public synchronized void refreshRedisPresence() {
        if (
                !redisProperties.isEnabled()
                        ||
                        sessions.isEmpty()
        ) {
            return;
        }

        StringRedisTemplate redisTemplate =
                requiredRedisTemplate();

        double expiresAt =
                System.currentTimeMillis()
                        + PRESENCE_TIMEOUT_MILLIS;

        sessions.forEach(
                (sessionId, session) ->
                {
                    redisTemplate
                            .opsForZSet()
                            .add(
                                    presenceKey(
                                            session.meetingId(),
                                            session.userId()
                                    ),
                                    redisMember(sessionId),
                                    expiresAt
                            );

                    String activeKey =
                            activeSessionKey(
                                    session.userId()
                            );

                    String currentActiveValue =
                            activeSessionValue(
                                    new ActiveRoomSession(
                                            session.meetingId(),
                                            redisMember(
                                                    sessionId
                                            )
                                    )
                            );

                    String savedActiveValue =
                            redisTemplate
                                    .opsForValue()
                                    .get(
                                            activeKey
                                    );

                    if (
                            currentActiveValue.equals(
                                    savedActiveValue
                            )
                    ) {
                        redisTemplate.expire(
                                activeKey,
                                Duration.ofMillis(
                                        PRESENCE_TIMEOUT_MILLIS
                                )
                        );
                    }
                }
        );
    }

    private void addRedisSession(
            String sessionId,
            Long meetingId,
            Long userId
    ) {
        double expiresAt =
                System.currentTimeMillis()
                        + PRESENCE_TIMEOUT_MILLIS;

        requiredRedisTemplate()
                .opsForZSet()
                .add(
                        presenceKey(
                                meetingId,
                                userId
                        ),
                        redisMember(sessionId),
                        expiresAt
                );
    }

    private boolean isOnlineInRedis(
            Long meetingId,
            Long userId
    ) {
        StringRedisTemplate redisTemplate =
                requiredRedisTemplate();

        String key =
                presenceKey(
                        meetingId,
                        userId
                );

        removeExpiredSessions(
                redisTemplate,
                key
        );

        Long size =
                redisTemplate
                        .opsForZSet()
                        .size(key);

        return size != null && size > 0;
    }

    private void removeExpiredSessions(
            StringRedisTemplate redisTemplate,
            String key
    ) {
        redisTemplate
                .opsForZSet()
                .removeRangeByScore(
                        key,
                        0,
                        System.currentTimeMillis()
                );
    }

    private String presenceKey(
            Long meetingId,
            Long userId
    ) {
        return "meetback:presence:meeting:"
                + meetingId
                + ":user:"
                + userId
                + ":sessions";
    }

    private String activeSessionKey(
            Long userId
    ) {
        return "meetback:active-room-session:user:"
                + userId;
    }

    /*
     * 서로 다른 서버에서 sessionId가 우연히 같아지는 상황을 막기 위해
     * 서버 인스턴스 ID를 함께 저장합니다.
     */
    private String redisMember(
            String sessionId
    ) {
        return serverInstanceId
                + ":"
                + sessionId;
    }

    private StringRedisTemplate requiredRedisTemplate() {
        StringRedisTemplate redisTemplate =
                redisTemplateProvider
                        .getIfAvailable();

        if (redisTemplate == null) {
            throw new IllegalStateException(
                    "Redis Presence가 활성화됐지만 "
                            + "StringRedisTemplate이 없습니다."
            );
        }

        return redisTemplate;
    }

    /*
     * Redis 비활성화 환경의 기존 로컬 처리입니다.
     */
    private PresenceChange connectLocal(
            Long meetingId,
            Long userId
    ) {
        PresenceKey key =
                new PresenceKey(
                        meetingId,
                        userId
                );

        int nextCount =
                connectionCounts
                        .getOrDefault(
                                key,
                                0
                        )
                        + 1;

        connectionCounts.put(
                key,
                nextCount
        );

        if (nextCount == 1) {
            return new PresenceChange(
                    meetingId,
                    userId,
                    true
            );
        }

        return null;
    }

    private PresenceChange disconnectLocal(
            SessionPresence session
    ) {
        PresenceKey key =
                new PresenceKey(
                        session.meetingId(),
                        session.userId()
                );

        int nextCount =
                connectionCounts
                        .getOrDefault(
                                key,
                                0
                        )
                        - 1;

        if (nextCount <= 0) {
            connectionCounts.remove(key);

            return new PresenceChange(
                    session.meetingId(),
                    session.userId(),
                    false
            );
        }

        connectionCounts.put(
                key,
                nextCount
        );

        return null;
    }

    public record ActiveRoomSession(
            Long meetingId,
            String sessionMember
    ) {
    }

    private record PresenceKey(
            Long meetingId,
            Long userId
    ) {
    }

    private record SessionPresence(
            Long meetingId,
            Long userId
    ) {
    }

    public record PresenceChange(
            Long meetingId,
            Long userId,
            boolean online
    ) {
    }
}