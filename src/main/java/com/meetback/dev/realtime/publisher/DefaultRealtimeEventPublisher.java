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
 * 업무 이벤트를 전송용 envelope로 만든 뒤 현재 인스턴스에 먼저 전달하고,
 * Redis가 활성화되어 있으면 동일한 envelope의 Redis 발행을 시도한다.
 *
 * <p>로컬 전달과 Redis 발행 경로의 예외는 각각 실패 결과로 변환한다.
 * {@code redisCommandSucceeded}는 {@code convertAndSend} 호출이
 * 예외 없이 끝났는지를 나타낸다. 구독자 수나 다른 인스턴스의
 * 실제 수신·처리 완료를 보장하지 않는다.</p>
 */
public class DefaultRealtimeEventPublisher
        implements RealtimeEventPublisher {

    private static final Logger log =
            LoggerFactory.getLogger(
                    DefaultRealtimeEventPublisher.class
            );

    private final LocalRealtimeDispatcher dispatcher;
    private final ObjectProvider<StringRedisTemplate>
            templateProvider;
    private final RealtimeEventCodec codec;
    private final LocalRealtimeEchoTracker echoTracker;
    private final RedisRealtimeProperties redisProps;
    private final String instanceId;

    public DefaultRealtimeEventPublisher(
            LocalRealtimeDispatcher dispatcher,
            ObjectProvider<StringRedisTemplate>
                    templateProvider,
            RealtimeEventCodec codec,
            LocalRealtimeEchoTracker echoTracker,
            RedisRealtimeProperties redisProps,
            String instanceId
    ) {
        this.dispatcher =
                Objects.requireNonNull(
                        dispatcher,
                        "localDispatcher는 필수입니다."
                );

        this.templateProvider =
                Objects.requireNonNull(
                        templateProvider,
                        "redisTemplateProvider는 필수입니다."
                );

        this.codec =
                Objects.requireNonNull(
                        codec,
                        "eventCodec은 필수입니다."
                );

        this.echoTracker =
                Objects.requireNonNull(
                        echoTracker,
                        "echoTracker는 필수입니다."
                );

        this.redisProps =
                Objects.requireNonNull(
                        redisProps,
                        "properties는 필수입니다."
                );

        if (
                instanceId == null
                        || instanceId.isBlank()
        ) {
            throw new IllegalArgumentException(
                    "sourceInstanceId는 필수입니다."
            );
        }

        this.instanceId = instanceId.trim();
    }

    @Override
    public RealtimePublishResult publish(
            RealtimeEvent event
    ) {
        RealtimeEventEnvelope envelope =
                RealtimeEventEnvelope.create(
                        event,
                        instanceId
                );

        // 같은 envelope를 로컬과 Redis에 사용해
        // eventId와 전송 메타데이터를 일치시킨다.
        boolean localDispatched = handleLocal(envelope);
        boolean redisCommandOk =
                publishRedis(
                        envelope,
                        localDispatched
                );

        return new RealtimePublishResult(
                envelope.eventId(),
                localDispatched,
                redisCommandOk
        );
    }

    private boolean handleLocal(
            RealtimeEventEnvelope envelope
    ) {
        try {
            dispatcher.dispatch(envelope);
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

            // false를 넘기면 Redis 발행 전에 marker를 남기지 않는다.
            // 발행이 성공하면 되돌아온 자기 echo가 로컬 전달을 한 번 재시도한다.
            return false;
        }
    }

    private boolean publishRedis(
            RealtimeEventEnvelope envelope,
            boolean localDispatched
    ) {
        if (!redisProps.isEnabled()) {
            return false;
        }

        try {
            // 선택적 의존성인 StringRedisTemplate은
            // Redis 발행을 시도하는 시점에 조회한다.
            StringRedisTemplate redisTemplate =
                    templateProvider.getIfAvailable();

            if (redisTemplate == null) {
                log.error(
                        "Redis 실시간 모듈이 활성화됐지만 "
                                + "StringRedisTemplate이 없습니다."
                );

                return false;
            }

            String json = codec.encode(envelope);

            /*
             * 발행 직후 자기 메시지를 수신할 수 있으므로
             * Redis 명령 전에 표시한다. 로컬 전달이 실패했다면 표시하지 않아
             * 자기 echo가 로컬 재시도를 수행한다.
             */
            if (localDispatched) {
                echoTracker.mark(envelope.eventId());
            }

            redisTemplate.convertAndSend(
                    redisProps.channelName(envelope.channel()),
                    json
            );

            return true;
        }
        catch (RuntimeException exception) {
            // 발행 경로 실패 시 등록되었을 수 있는 marker가
            // 이후 처리를 막지 않도록 제거한다.
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
