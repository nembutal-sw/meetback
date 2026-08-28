package com.meetback.dev.realtime.event;

import java.util.UUID;

public record RealtimePublishResult(
        UUID eventId,
        boolean localDispatched,
        boolean redisCommandSucceeded
) {
}
