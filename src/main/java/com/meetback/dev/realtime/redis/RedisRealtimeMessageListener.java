package com.meetback.dev.realtime.redis;

import com.meetback.dev.realtime.config.RedisRealtimeProperties;
import com.meetback.dev.realtime.event.RealtimeEventEnvelope;
import com.meetback.dev.realtime.gateway.LocalRealtimeDispatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * Redis 메시지의 채널과 본문을 수신해 envelope로 복원하고
 * 현재 인스턴스에 전달한다.
 *
 * <p>수신 Redis 채널 검증과 자기 echo 판정을 통과한 이벤트만
 * {@link LocalRealtimeDispatcher}로 넘긴다.</p>
 */
public class RedisRealtimeMessageListener
        implements MessageListener {

    private static final Logger log =
            LoggerFactory.getLogger(
                    RedisRealtimeMessageListener.class
            );

    private final RealtimeEventCodec codec;
    private final LocalRealtimeDispatcher dispatcher;
    private final LocalRealtimeEchoTracker echoTracker;
    private final RedisRealtimeProperties redisProps;
    private final String instanceId;

    public RedisRealtimeMessageListener(
            RealtimeEventCodec codec,
            LocalRealtimeDispatcher dispatcher,
            LocalRealtimeEchoTracker echoTracker,
            RedisRealtimeProperties redisProps,
            String instanceId
    ) {
        this.codec =
                Objects.requireNonNull(
                        codec,
                        "eventCodec은 필수입니다."
                );

        this.dispatcher =
                Objects.requireNonNull(
                        dispatcher,
                        "localDispatcher는 필수입니다."
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
                    "localInstanceId는 필수입니다."
            );
        }

        this.instanceId = instanceId.trim();
    }

    @Override
    public void onMessage(
            Message message,
            byte[] pattern
    ) {
        String redisChannel =
                new String(
                        message.getChannel(),
                        StandardCharsets.UTF_8
                );

        String json =
                new String(
                        message.getBody(),
                        StandardCharsets.UTF_8
                );

        receive(redisChannel, json);
    }

    void receive(
            String redisChannel,
            String json
    ) {
        try {
            RealtimeEventEnvelope event =
                    codec.decode(json);

            // envelope의 논리 채널과 실제 수신 Redis 채널이 다르면
            // 잘못 유입된 메시지로 폐기한다.
            String expectedChannel =
                    redisProps.channelName(
                            event.channel()
                    );

            if (!expectedChannel.equals(redisChannel)) {
                log.warn(
                        "Redis 실시간 이벤트의 채널이 envelope과 다릅니다. "
                                + "eventId={}, expected={}, actual={}",
                        event.eventId(),
                        expectedChannel,
                        redisChannel
                );

                return;
            }

            /*
             * 같은 인스턴스가 발행했고 유효한 marker가 있으면
             * 이미 로컬 전달된 echo다. marker가 없으면 로컬 실패·만료·정리
             * 가능성이 있으므로 아래에서 다시 전달한다.
             */
            if (
                    instanceId
                            .equals(event.sourceInstanceId())
                            && echoTracker.consumeIfMarked(
                                    event.eventId()
                            )
            ) {
                return;
            }

            dispatcher.dispatch(event);
        }
        catch (RuntimeException exception) {
            // 현재 메시지의 역직렬화·검증·로컬 처리 실패를
            // listener 밖으로 전파하지 않는다.
            log.error(
                    "Redis 실시간 이벤트 수신 처리에 실패했습니다. "
                            + "channel={}",
                    redisChannel,
                    exception
            );
        }
    }
}
