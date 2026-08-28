package com.meetback.dev.realtime.event;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Redis transport envelope. The nested clientPayload is the only value that
 * should be forwarded to the existing STOMP clients.
 */
public record RealtimeEventEnvelope(
        int schemaVersion,
        UUID eventId,
        String sourceInstanceId,
        RealtimeChannel channel,
        RealtimeDeliveryKind deliveryKind,
        String eventType,
        String reason,
        Long meetingId,
        Long actorUserId,
        Long targetUserId,
        Long targetParticipantId,
        Integer minimumValidTokenVersion,
        Object clientPayload,
        Instant occurredAt
) {

    public static final int CURRENT_SCHEMA_VERSION = 1;

    public RealtimeEventEnvelope {
        if (schemaVersion != CURRENT_SCHEMA_VERSION) {
            throw new IllegalArgumentException(
                    "지원하지 않는 realtime schemaVersion입니다: "
                            + schemaVersion
            );
        }

        Objects.requireNonNull(eventId, "eventId는 필수입니다.");
        Objects.requireNonNull(occurredAt, "occurredAt은 필수입니다.");

        if (sourceInstanceId == null || sourceInstanceId.isBlank()) {
            throw new IllegalArgumentException(
                    "sourceInstanceId는 필수입니다."
            );
        }

        sourceInstanceId = sourceInstanceId.trim();

        RealtimeEvent validatedEvent =
                new RealtimeEvent(
                channel,
                deliveryKind,
                eventType,
                reason,
                meetingId,
                actorUserId,
                targetUserId,
                targetParticipantId,
                minimumValidTokenVersion,
                clientPayload
        );

        eventType = validatedEvent.eventType();
        reason = validatedEvent.reason();
    }

    public static RealtimeEventEnvelope create(
            RealtimeEvent event,
            String sourceInstanceId
    ) {
        Objects.requireNonNull(event, "event는 필수입니다.");

        return new RealtimeEventEnvelope(
                CURRENT_SCHEMA_VERSION,
                UUID.randomUUID(),
                sourceInstanceId,
                event.channel(),
                event.deliveryKind(),
                event.eventType(),
                event.reason(),
                event.meetingId(),
                event.actorUserId(),
                event.targetUserId(),
                event.targetParticipantId(),
                event.minimumValidTokenVersion(),
                event.clientPayload(),
                Instant.now()
        );
    }
}
