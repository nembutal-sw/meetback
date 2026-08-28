package com.meetback.dev.realtime.config;

import com.meetback.dev.realtime.gateway.LocalRealtimeDispatcher;
import com.meetback.dev.realtime.redis.RealtimeEventCodec;
import com.meetback.dev.realtime.redis.LocalRealtimeEchoTracker;
import com.meetback.dev.realtime.redis.RedisRealtimeMessageListener;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.data.redis.autoconfigure.RedisMessageListenerContainerConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Redis 실시간 기능이 활성화된 경우에만 수신 listener와 실행기,
 * 구독 container를 구성한다. MEETING과 AUTH Redis 채널에서 받은 메시지는
 * 동일한 listener의 검증 경로를 거치며,
 * 검증을 통과한 메시지만 로컬 dispatcher로 전달된다.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(
        prefix = "meetback.realtime.redis",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = false
)
public class RedisRealtimeSubscriberConfiguration {

    @Bean(name = "redisRealtimeMessageListener")
    RedisRealtimeMessageListener
    redisListener(
            RealtimeEventCodec codec,
            LocalRealtimeDispatcher dispatcher,
            LocalRealtimeEchoTracker echoTracker,
            RedisRealtimeProperties redisProps,
            @Qualifier("serverInstanceId")
            String instanceId
    ) {
        return new RedisRealtimeMessageListener(
                codec,
                dispatcher,
                echoTracker,
                redisProps,
                instanceId
        );
    }

    @Bean(
            name = "realtimeRedisListenerExecutor",
            destroyMethod = "shutdown"
    )
    ExecutorService redisListenerExecutor() {
        AtomicInteger threadNumber =
                new AtomicInteger();

        return Executors.newSingleThreadExecutor(
                runnable -> {
                    Thread thread =
                            new Thread(
                                    runnable,
                                    "redis-realtime-listener-"
                                            + threadNumber.incrementAndGet()
                            );

                    thread.setDaemon(true);
                    return thread;
                }
        );
    }

    @Bean(name = "redisMessageListenerContainer")
    RedisMessageListenerContainer
    redisListenerContainer(
            RedisMessageListenerContainerConfigurer configurer,
            RedisConnectionFactory connectionFactory,
            RedisRealtimeMessageListener listener,
            @Qualifier("realtimeRedisListenerExecutor")
            ExecutorService listenerExecutor,
            RedisRealtimeProperties redisProps
    ) {
        // 두 실제 채널명이 같으면 MEETING과 AUTH를 구분할 수 없어
        // envelope와 수신 채널의 일치 검증이 무의미해진다.
        if (
                redisProps.getMeetingChannel()
                        .equals(redisProps.getAuthChannel())
        ) {
            throw new IllegalStateException(
                    "meeting-channel과 auth-channel은 달라야 합니다."
            );
        }

        RedisMessageListenerContainer container =
                new RedisMessageListenerContainer();

        configurer.configure(
                container,
                connectionFactory
        );

        /*
         * MEETING과 AUTH 콜백을 하나의 전용 스레드에서 순차 실행한다.
         * 서로 다른 발행자와 채널 전체의 전역 순서를 보장하는 것은 아니다.
         */
        container.setTaskExecutor(
                listenerExecutor
        );

        container.addMessageListener(
                listener,
                new ChannelTopic(
                        redisProps.getMeetingChannel()
                )
        );

        container.addMessageListener(
                listener,
                new ChannelTopic(
                        redisProps.getAuthChannel()
                )
        );

        return container;
    }
}
