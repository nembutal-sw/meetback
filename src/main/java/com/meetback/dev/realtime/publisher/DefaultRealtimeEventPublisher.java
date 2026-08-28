package com.meetback.dev.realtime.publisher;

import com.meetback.dev.realtime.config.RedisRealtimeProperties;
import com.meetback.dev.realtime.event.RealtimeEvent;
import com.meetback.dev.realtime.event.RealtimeEventEnvelope;
import com.meetback.dev.realtime.event.RealtimePublishResult;
import com.meetback.dev.realtime.gateway.LocalRealtimeDispatcher;
import com.meetback.dev.realtime.redis.LocalRealtimeEchoTracker;
import com.meetback.dev.realtime.redis.RealtimeEventCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Objects;

/**
 * Local-first publisher.
 *
 * <p>A Redis outage must not undo an already committed business transaction.
 * The result tells the caller whether cross-instance publication succeeded.</p>
 */
public class DefaultRealtimeEventPublisher
        implements RealtimeEventPublisher {

    private static final Logger log =
            LoggerFactory.getLogger(
                    DefaultRealtimeEventPublisher.class
            );

    private final LocalRealtimeDispatcher localDispatcher;
    private final ObjectProvider<StringRedisTemplate>
            redisTemplateProvider;
    private final RealtimeEventCodec eventCodec;
    private final LocalRealtimeEchoTracker echoTracker;
    private final RedisRealtimeProperties properties;
    private final String sourceInstanceId;

    public DefaultRealtimeEventPublisher(
            LocalRealtimeDispatcher localDispatcher,
            ObjectProvider<StringRedisTemplate>
                    redisTemplateProvider,
            RealtimeEventCodec eventCodec,
            LocalRealtimeEchoTracker echoTracker,
            RedisRealtimeProperties properties,
            String sourceInstanceId
    ) {
        this.localDispatcher =
                Objects.requireNonNull(
                        localDispatcher,
                        "localDispatcher는 필수입니다."
                );

        this.redisTemplateProvider =
                Objects.requireNonNull(
                        redisTemplateProvider,
                        "redisTemplateProvider는 필수입니다."
                );

        this.eventCodec =
                Objects.requireNonNull(
                        eventCodec,
                        "eventCodec은 필수입니다."
                );

        this.echoTracker =
                Objects.requireNonNull(
                        echoTracker,
                        "echoTracker는 필수입니다."
                );

        this.properties =
                Objects.requireNonNull(
                        properties,
                        "properties는 필수입니다."
                );

        if (
                sourceInstanceId == null
                        || sourceInstanceId.isBlank()
        ) {
            throw new IllegalArgumentException(
                    "sourceInstanceId는 필수입니다."
            );
        }

        this.sourceInstanceId = sourceInstanceId.trim();
    }

    @Override
    public RealtimePublishResult publish(
            RealtimeEvent event
    ) {
        RealtimeEventEnvelope envelope =
                RealtimeEventEnvelope.create(
                        event,
                        sourceInstanceId
                );

        boolean localDispatched = dispatchLocal(envelope);
        boolean redisPublished =
                publishRedis(
                        envelope,
                        localDispatched
                );

        return new RealtimePublishResult(
                envelope.eventId(),
                localDispatched,
                redisPublished
        );
    }

    private boolean dispatchLocal(
            RealtimeEventEnvelope envelope
    ) {
        try {
            localDispatcher.dispatch(envelope);
            return true;
        }
        catch (RuntimeException exception) {
            log.error(
                    "로컬 실시간 이벤트 전달에 실패했습니다. "
                            + "eventId={}, eventType={}",
                    envelope.eventId(),
                    envelope.eventType(),
                    exception
            );

            return false;
        }
    }

    private boolean publishRedis(
            RealtimeEventEnvelope envelope,
            boolean localDispatched
    ) {
        if (!properties.isEnabled()) {
            return false;
        }

        try {
            StringRedisTemplate redisTemplate =
                    redisTemplateProvider.getIfAvailable();

            if (redisTemplate == null) {
                log.error(
                        "Redis 실시간 모듈이 활성화됐지만 "
                                + "StringRedisTemplate이 없습니다."
                );

                return false;
            }

            String json = eventCodec.encode(envelope);

            if (localDispatched) {
                echoTracker.mark(envelope.eventId());
            }

            redisTemplate.convertAndSend(
                    properties.channelName(envelope.channel()),
                    json
            );

            return true;
        }
        catch (RuntimeException exception) {
            echoTracker.forget(envelope.eventId());

            log.error(
                    "Redis 실시간 이벤트 발행에 실패했습니다. "
                            + "eventId={}, eventType={}",
                    envelope.eventId(),
                    envelope.eventType(),
                    exception
            );

            return false;
        }
    }
}
