package com.meetback.dev.realtime.redis;

import com.meetback.dev.realtime.event.RealtimeEventEnvelope;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.Objects;

public class JacksonRealtimeEventCodec
        implements RealtimeEventCodec {

    private final ObjectMapper objectMapper;

    public JacksonRealtimeEventCodec(
            ObjectMapper objectMapper
    ) {
        this.objectMapper =
                Objects.requireNonNull(
                        objectMapper,
                        "objectMapper는 필수입니다."
                );
    }

    @Override
    public String encode(
            RealtimeEventEnvelope event
    ) {
        Objects.requireNonNull(event, "event는 필수입니다.");

        try {
            return objectMapper.writeValueAsString(event);
        }
        catch (JacksonException exception) {
            throw new IllegalArgumentException(
                    "실시간 이벤트 JSON 직렬화에 실패했습니다.",
                    exception
            );
        }
    }

    @Override
    public RealtimeEventEnvelope decode(
            String json
    ) {
        if (json == null || json.isBlank()) {
            throw new IllegalArgumentException(
                    "실시간 이벤트 JSON은 필수입니다."
            );
        }

        try {
            return objectMapper.readValue(
                    json,
                    RealtimeEventEnvelope.class
            );
        }
        catch (JacksonException exception) {
            throw new IllegalArgumentException(
                    "실시간 이벤트 JSON 역직렬화에 실패했습니다.",
                    exception
            );
        }
    }
}
