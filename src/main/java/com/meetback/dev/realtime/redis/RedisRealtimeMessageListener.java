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

public class RedisRealtimeMessageListener
        implements MessageListener {

    private static final Logger log =
            LoggerFactory.getLogger(
                    RedisRealtimeMessageListener.class
            );

    private final RealtimeEventCodec eventCodec;
    private final LocalRealtimeDispatcher localDispatcher;
    private final LocalRealtimeEchoTracker echoTracker;
    private final RedisRealtimeProperties properties;
    private final String localInstanceId;

    public RedisRealtimeMessageListener(
            RealtimeEventCodec eventCodec,
            LocalRealtimeDispatcher localDispatcher,
            LocalRealtimeEchoTracker echoTracker,
            RedisRealtimeProperties properties,
            String localInstanceId
    ) {
        this.eventCodec =
                Objects.requireNonNull(
                        eventCodec,
                        "eventCodec은 필수입니다."
                );

        this.localDispatcher =
                Objects.requireNonNull(
                        localDispatcher,
                        "localDispatcher는 필수입니다."
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
                localInstanceId == null
                        || localInstanceId.isBlank()
        ) {
            throw new IllegalArgumentException(
                    "localInstanceId는 필수입니다."
            );
        }

        this.localInstanceId = localInstanceId.trim();
    }

    @Override
    public void onMessage(
            Message message,
            byte[] pattern
    ) {
        String channel =
                new String(
                        message.getChannel(),
                        StandardCharsets.UTF_8
                );

        String json =
                new String(
                        message.getBody(),
                        StandardCharsets.UTF_8
                );

        receive(channel, json);
    }

    void receive(
            String channel,
            String json
    ) {
        try {
            RealtimeEventEnvelope event =
                    eventCodec.decode(json);

            String expectedChannel =
                    properties.channelName(
                            event.channel()
                    );

            if (!expectedChannel.equals(channel)) {
                log.warn(
                        "Redis 실시간 이벤트의 채널이 envelope과 다릅니다. "
                                + "eventId={}, expected={}, actual={}",
                        event.eventId(),
                        expectedChannel,
                        channel
                );

                return;
            }

            if (
                    localInstanceId
                            .equals(event.sourceInstanceId())
                            && echoTracker.consumeIfMarked(
                                    event.eventId()
                            )
            ) {
                return;
            }

            localDispatcher.dispatch(event);
        }
        catch (RuntimeException exception) {
            log.error(
                    "Redis 실시간 이벤트 수신 처리에 실패했습니다. "
                            + "channel={}",
                    channel,
                    exception
            );
        }
    }
}
