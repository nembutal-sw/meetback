package com.meetback.dev.realtime.redis;

import com.meetback.dev.realtime.event.RealtimeEventEnvelope;

public interface RealtimeEventCodec {

    String encode(RealtimeEventEnvelope event);

    RealtimeEventEnvelope decode(String json);
}
