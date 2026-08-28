package com.meetback.dev.realtime.redis;

import com.meetback.dev.realtime.event.RealtimeEventEnvelope;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.Objects;

/**
 * 공용 {@link ObjectMapper}로 Redis 전송 envelope 전체를 JSON과 상호 변환한다.
 * Jackson 변환 오류는 {@link IllegalArgumentException}으로 감싸 발행·수신 경로가
 * 동일한 예외 형태로 처리하도록 한다.
 */
public class JacksonRealtimeEventCodec
        implements RealtimeEventCodec {

    private final ObjectMapper mapper;

    public JacksonRealtimeEventCodec(
            ObjectMapper mapper
    ) {
        this.mapper =
                Objects.requireNonNull(
                        mapper,
                        "objectMapper는 필수입니다."
                );
    }

    @Override
    public String encode(
            RealtimeEventEnvelope event
    ) {
        Objects.requireNonNull(event, "event는 필수입니다.");

        try {
            return mapper.writeValueAsString(event);
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
            return mapper.readValue(
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
