package com.meetback.dev.realtime.redis;

import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 로컬 전달에 성공한 eventId를 잠시 보관해
 * 같은 인스턴스로 돌아온 Redis echo를 한 번만 생략한다.
 *
 * <p>로컬 전달에 실패했거나 marker가 만료·정리된 경우에는
 * echo를 통과시킨다. 다른 인스턴스에서 반복 발행된 이벤트까지 제거하는
 * 일반 중복 방지 저장소는 아니다.</p>
 */
public class LocalRealtimeEchoTracker {

    private static final Duration DEFAULT_TTL =
            Duration.ofMinutes(2);

    private static final int DEFAULT_MAX_SIZE = 10_000;

    private final Map<UUID, Long> markers =
            new ConcurrentHashMap<>();

    private final Clock clock;
    private final long ttlMillis;
    private final int maxSize;

    public LocalRealtimeEchoTracker() {
        this(
                Clock.systemUTC(),
                DEFAULT_TTL,
                DEFAULT_MAX_SIZE
        );
    }

    LocalRealtimeEchoTracker(
            Clock clock,
            Duration ttl,
            int maxSize
    ) {
        this.clock = Objects.requireNonNull(
                clock,
                "clock은 필수입니다."
        );

        Objects.requireNonNull(
                ttl,
                "retention은 필수입니다."
        );

        if (ttl.isNegative() || ttl.isZero()) {
            throw new IllegalArgumentException(
                    "retention은 0보다 커야 합니다."
            );
        }

        if (maxSize <= 0) {
            throw new IllegalArgumentException(
                    "maxEntries는 0보다 커야 합니다."
            );
        }

        this.ttlMillis = ttl.toMillis();
        this.maxSize = maxSize;
    }

    /**
     * 로컬 전달에 성공한 이벤트를 만료 시각과 함께 표시한다.
     */
    public void mark(
            UUID eventId
    ) {
        Objects.requireNonNull(eventId, "eventId는 필수입니다.");

        // 정기 작업 대신 상한에 도달한 시점에 만료 marker를 지연 정리한다.
        if (markers.size() >= maxSize) {
            removeExpired();
        }

        markers.put(
                eventId,
                clock.millis() + ttlMillis
        );
    }

    /**
     * marker를 조회와 동시에 제거한다. 유효한 marker만 true를 반환하므로
     * 등록된 marker 하나당 자기 echo를 한 번만 생략한다.
     */
    public boolean consumeIfMarked(
            UUID eventId
    ) {
        Objects.requireNonNull(eventId, "eventId는 필수입니다.");

        Long expiresAt = markers.remove(eventId);

        return expiresAt != null
                && expiresAt >= clock.millis();
    }

    /**
     * Redis 발행에 실패한 이벤트의 marker를 제거한다.
     */
    public void forget(
            UUID eventId
    ) {
        Objects.requireNonNull(eventId, "eventId는 필수입니다.");
        markers.remove(eventId);
    }

    private void removeExpired() {
        long now = clock.millis();

        markers.entrySet().removeIf(
                entry -> entry.getValue() < now
        );

        // 만료 항목을 지운 뒤에도 상한 이상이면
        // 전체를 비워 크기 증가를 제한한다.
        if (markers.size() >= maxSize) {
            markers.clear();
        }
    }
}
