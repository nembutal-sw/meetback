package com.meetback.dev.realtime.event;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RealtimeEventTest {

    @Test
    void createsKickBroadcastWithExplicitTargetMetadata() {
        Map<String, Object> payload =
                Map.of(
                        "messageType", "EVENT",
                        "eventType", "PARTICIPANT_KICKED",
                        "userId", 30L
                );

        RealtimeEvent event =
                RealtimeEvent
                        .meetingBroadcastAndDisconnectTarget(
                                "PARTICIPANT_KICKED",
                                7L,
                                10L,
                                30L,
                                22L,
                                payload
                        );

        assertThat(event.channel())
                .isEqualTo(RealtimeChannel.MEETING);

        assertThat(event.deliveryKind())
                .isEqualTo(
                        RealtimeDeliveryKind
                                .ROOM_BROADCAST_AND_DISCONNECT_TARGET
                );

        assertThat(event.meetingId()).isEqualTo(7L);
        assertThat(event.actorUserId()).isEqualTo(10L);
        assertThat(event.targetUserId()).isEqualTo(30L);
        assertThat(event.targetParticipantId()).isEqualTo(22L);
        assertThat(event.clientPayload()).isSameAs(payload);
    }

    @Test
    void rejectsRoomBroadcastWithoutPositiveMeetingId() {
        assertThatThrownBy(() ->
                RealtimeEvent.meetingBroadcast(
                        "VOTE_UPDATED",
                        0L,
                        10L,
                        Map.of("eventType", "VOTE_UPDATED")
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("meetingId");
    }

    @Test
    void rejectsAuthInvalidationWithNegativeTokenVersion() {
        assertThatThrownBy(() ->
                RealtimeEvent.authInvalidated(
                        AuthInvalidationReason.LOGOUT,
                        30L,
                        -1
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("minimumValidTokenVersion");
    }

    @Test
    void rejectsUnknownAuthInvalidationReasonFromTransport() {
        assertThatThrownBy(() ->
                new RealtimeEvent(
                        RealtimeChannel.AUTH,
                        RealtimeDeliveryKind
                                .DISCONNECT_USER_BEFORE_TOKEN_VERSION,
                        "AUTH_INVALIDATED",
                        "UNKNOWN_REASON",
                        null,
                        null,
                        30L,
                        null,
                        2,
                        null
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("reason");
    }

    @Test
    void rejectsNegativeOptionalTargetUserId() {
        assertThatThrownBy(() ->
                RealtimeEvent.meetingBroadcast(
                        "VOTE_UPDATED",
                        7L,
                        10L,
                        -1L,
                        null,
                        Map.of("eventType", "VOTE_UPDATED")
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("targetUserId");
    }
}
