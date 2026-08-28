package com.meetback.dev.realtime.redis;

import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks events already delivered locally before Redis publication.
 *
 * <p>The source instance consumes its Redis echo only when local delivery was
 * successful. If local delivery failed, the echo is allowed through as one
 * retry path.</p>
 */
public class LocalRealtimeEchoTracker {

    private static final Duration DEFAULT_RETENTION =
            Duration.ofMinutes(2);

    private static final int DEFAULT_MAX_ENTRIES = 10_000;

    private final Map<UUID, Long> markedEvents =
            new ConcurrentHashMap<>();

    private final Clock clock;
    private final long retentionMillis;
    private final int maxEntries;

    public LocalRealtimeEchoTracker() {
        this(
                Clock.systemUTC(),
                DEFAULT_RETENTION,
                DEFAULT_MAX_ENTRIES
        );
    }

    LocalRealtimeEchoTracker(
            Clock clock,
            Duration retention,
            int maxEntries
    ) {
        this.clock = Objects.requireNonNull(
                clock,
                "clock은 필수입니다."
        );

        Objects.requireNonNull(
                retention,
                "retention은 필수입니다."
        );

        if (retention.isNegative() || retention.isZero()) {
            throw new IllegalArgumentException(
                    "retention은 0보다 커야 합니다."
            );
        }

        if (maxEntries <= 0) {
            throw new IllegalArgumentException(
                    "maxEntries는 0보다 커야 합니다."
            );
        }

        this.retentionMillis = retention.toMillis();
        this.maxEntries = maxEntries;
    }

    public void mark(
            UUID eventId
    ) {
        Objects.requireNonNull(eventId, "eventId는 필수입니다.");

        if (markedEvents.size() >= maxEntries) {
            removeExpired();
        }

        markedEvents.put(
                eventId,
                clock.millis() + retentionMillis
        );
    }

    public boolean consumeIfMarked(
            UUID eventId
    ) {
        Objects.requireNonNull(eventId, "eventId는 필수입니다.");

        Long expiresAt = markedEvents.remove(eventId);

        return expiresAt != null
                && expiresAt >= clock.millis();
    }

    public void forget(
            UUID eventId
    ) {
        Objects.requireNonNull(eventId, "eventId는 필수입니다.");
        markedEvents.remove(eventId);
    }

    private void removeExpired() {
        long now = clock.millis();

        markedEvents.entrySet().removeIf(
                entry -> entry.getValue() < now
        );

        if (markedEvents.size() >= maxEntries) {
            markedEvents.clear();
        }
    }
}
