package com.meetback.dev.realtime.redis;

import com.meetback.dev.realtime.event.RealtimeEventEnvelope;

/**
 * Redis로 전달하는 envelope와 발행할 JSON 문자열 사이의 변환 경계다.
 */
public interface RealtimeEventCodec {

    String encode(RealtimeEventEnvelope event);

    RealtimeEventEnvelope decode(String json);
}
