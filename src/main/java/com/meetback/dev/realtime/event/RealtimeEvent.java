package com.meetback.dev.realtime.event;

import java.util.Objects;

/**
 * Application-facing event contract.
 *
 * <p>Callers describe the business event only. Redis-specific metadata such as
 * event ID, source instance ID and occurrence time is added by the publisher.</p>
 */
public record RealtimeEvent(
        RealtimeChannel channel,
        RealtimeDeliveryKind deliveryKind,
        String eventType,
        String reason,
        Long meetingId,
        Long actorUserId,
        Long targetUserId,
        Long targetParticipantId,
        Integer minimumValidTokenVersion,
        Object clientPayload
) {

    public RealtimeEvent {
        Objects.requireNonNull(channel, "channel은 필수입니다.");
        Objects.requireNonNull(deliveryKind, "deliveryKind는 필수입니다.");

        eventType = requireText(eventType, "eventType");

        switch (deliveryKind) {
            case ROOM_BROADCAST -> {
                requireChannel(channel, RealtimeChannel.MEETING);
                requirePositive(meetingId, "meetingId");
                Objects.requireNonNull(clientPayload, "clientPayload는 필수입니다.");
                requireNull(reason, "reason");
                requireNull(
                        minimumValidTokenVersion,
                        "minimumValidTokenVersion"
                );
            }
            case ROOM_BROADCAST_AND_DISCONNECT_TARGET -> {
                requireChannel(channel, RealtimeChannel.MEETING);
                requirePositive(meetingId, "meetingId");
                requirePositive(targetUserId, "targetUserId");
                Objects.requireNonNull(clientPayload, "clientPayload는 필수입니다.");
                requireNull(reason, "reason");
                requireNull(
                        minimumValidTokenVersion,
                        "minimumValidTokenVersion"
                );
            }
            case DISCONNECT_USER_BEFORE_TOKEN_VERSION -> {
                requireChannel(channel, RealtimeChannel.AUTH);
                requirePositive(targetUserId, "targetUserId");

                if (!"AUTH_INVALIDATED".equals(eventType)) {
                    throw new IllegalArgumentException(
                            "인증 무효화 eventType은 AUTH_INVALIDATED여야 합니다."
                    );
                }

                reason = requireText(reason, "reason");

                try {
                    AuthInvalidationReason.valueOf(reason);
                }
                catch (IllegalArgumentException exception) {
                    throw new IllegalArgumentException(
                            "지원하지 않는 인증 무효화 reason입니다: "
                                    + reason,
                            exception
                    );
                }

                requireNull(meetingId, "meetingId");
                requireNull(actorUserId, "actorUserId");
                requireNull(
                        targetParticipantId,
                        "targetParticipantId"
                );
                requireNull(clientPayload, "clientPayload");

                if (
                        minimumValidTokenVersion == null
                                || minimumValidTokenVersion < 0
                ) {
                    throw new IllegalArgumentException(
                            "minimumValidTokenVersion은 0 이상이어야 합니다."
                    );
                }
            }
        }

        requireOptionalPositive(actorUserId, "actorUserId");
        requireOptionalPositive(targetUserId, "targetUserId");
        requireOptionalPositive(targetParticipantId, "targetParticipantId");
    }

    public static RealtimeEvent meetingBroadcast(
            String eventType,
            Long meetingId,
            Long actorUserId,
            Object clientPayload
    ) {
        return meetingBroadcast(
                eventType,
                meetingId,
                actorUserId,
                null,
                null,
                clientPayload
        );
    }

    public static RealtimeEvent meetingBroadcast(
            String eventType,
            Long meetingId,
            Long actorUserId,
            Long targetUserId,
            Long targetParticipantId,
            Object clientPayload
    ) {
        return new RealtimeEvent(
                RealtimeChannel.MEETING,
                RealtimeDeliveryKind.ROOM_BROADCAST,
                eventType,
                null,
                meetingId,
                actorUserId,
                targetUserId,
                targetParticipantId,
                null,
                clientPayload
        );
    }

    public static RealtimeEvent meetingBroadcastAndDisconnectTarget(
            String eventType,
            Long meetingId,
            Long actorUserId,
            Long targetUserId,
            Long targetParticipantId,
            Object clientPayload
    ) {
        return new RealtimeEvent(
                RealtimeChannel.MEETING,
                RealtimeDeliveryKind.ROOM_BROADCAST_AND_DISCONNECT_TARGET,
                eventType,
                null,
                meetingId,
                actorUserId,
                targetUserId,
                targetParticipantId,
                null,
                clientPayload
        );
    }

    public static RealtimeEvent authInvalidated(
            AuthInvalidationReason reason,
            Long targetUserId,
            Integer newTokenVersion
    ) {
        return new RealtimeEvent(
                RealtimeChannel.AUTH,
                RealtimeDeliveryKind.DISCONNECT_USER_BEFORE_TOKEN_VERSION,
                "AUTH_INVALIDATED",
                Objects.requireNonNull(
                        reason,
                        "reason은 필수입니다."
                ).name(),
                null,
                null,
                targetUserId,
                null,
                newTokenVersion,
                null
        );
    }

    private static String requireText(
            String value,
            String fieldName
    ) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    fieldName + "은(는) 필수입니다."
            );
        }

        return value.trim();
    }

    private static void requireChannel(
            RealtimeChannel actual,
            RealtimeChannel expected
    ) {
        if (actual != expected) {
            throw new IllegalArgumentException(
                    "deliveryKind와 channel 조합이 올바르지 않습니다."
            );
        }
    }

    private static void requirePositive(
            Long value,
            String fieldName
    ) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(
                    fieldName + "은(는) 양수여야 합니다."
            );
        }
    }

    private static void requireOptionalPositive(
            Long value,
            String fieldName
    ) {
        if (value != null) {
            requirePositive(value, fieldName);
        }
    }

    private static void requireNull(
            Object value,
            String fieldName
    ) {
        if (value != null) {
            throw new IllegalArgumentException(
                    fieldName + "은(는) 이 이벤트에서 사용할 수 없습니다."
            );
        }
    }
}
