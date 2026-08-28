package com.meetback.dev.realtime.redis;

import com.meetback.dev.realtime.event.RealtimeEvent;
import com.meetback.dev.realtime.event.RealtimeEventEnvelope;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class JacksonRealtimeEventCodecTest {

    @Test
    void roundTripsTypedEnvelopeWithoutLeakingTransportMetadataIntoPayload() {
        JacksonRealtimeEventCodec codec =
                new JacksonRealtimeEventCodec(
                        JsonMapper.builder()
                                .findAndAddModules()
                                .build()
                );

        RealtimeEvent event =
                RealtimeEvent
                        .meetingBroadcastAndDisconnectTarget(
                                "PARTICIPANT_KICKED",
                                7L,
                                10L,
                                30L,
                                22L,
                                Map.of(
                                        "messageType", "EVENT",
                                        "eventType", "PARTICIPANT_KICKED",
                                        "meetingId", 7L,
                                        "participantId", 22L,
                                        "userId", 30L,
                                        "nickname", "홍길동 🚀"
                                )
                        );

        RealtimeEventEnvelope original =
                RealtimeEventEnvelope.create(
                        event,
                        "instance-a"
                );

        String json = codec.encode(original);

        RealtimeEventEnvelope restored =
                codec.decode(json);

        assertThat(restored.schemaVersion())
                .isEqualTo(
                        RealtimeEventEnvelope
                                .CURRENT_SCHEMA_VERSION
                );

        assertThat(restored.eventId())
                .isEqualTo(original.eventId());

        assertThat(restored.occurredAt())
                .isEqualTo(original.occurredAt());

        assertThat(restored.clientPayload())
                .asString()
                .contains("PARTICIPANT_KICKED")
                .contains("홍길동 🚀")
                .doesNotContain("sourceInstanceId");
    }
}
