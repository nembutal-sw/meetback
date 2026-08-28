package com.meetback.dev.realtime.publisher;

import com.meetback.dev.realtime.event.RealtimeEvent;
import com.meetback.dev.realtime.event.RealtimePublishResult;

public interface RealtimeEventPublisher {

    /**
     * 이벤트를 현재 인스턴스에 전달하고, 활성화된 경우
     * 같은 이벤트를 Redis로 발행한다.
     *
     * <p>DB 상태가 바뀌는 이벤트는 트랜잭션 커밋 후 호출해야 한다.
     * 현재 구현에는 outbox나 트랜잭션 동기화가 포함되어 있지 않다.</p>
     */
    RealtimePublishResult publish(RealtimeEvent event);
}
