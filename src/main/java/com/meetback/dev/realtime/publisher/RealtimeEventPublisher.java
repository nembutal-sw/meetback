package com.meetback.dev.realtime.publisher;

import com.meetback.dev.realtime.event.RealtimeEvent;
import com.meetback.dev.realtime.event.RealtimePublishResult;

public interface RealtimeEventPublisher {

    /**
     * Delivers an event locally and, when enabled, through Redis.
     *
     * <p>Persistent business events must call this method only after their DB
     * transaction commits. The current module intentionally does not provide
     * an outbox or transaction synchronization.</p>
     */
    RealtimePublishResult publish(RealtimeEvent event);
}
