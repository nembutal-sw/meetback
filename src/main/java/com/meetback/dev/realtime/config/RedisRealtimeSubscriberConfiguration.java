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

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(
        prefix = "meetback.realtime.redis",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = false
)
public class RedisRealtimeSubscriberConfiguration {

    @Bean
    RedisRealtimeMessageListener
    redisRealtimeMessageListener(
            RealtimeEventCodec eventCodec,
            LocalRealtimeDispatcher localDispatcher,
            LocalRealtimeEchoTracker echoTracker,
            RedisRealtimeProperties properties,
            @Qualifier("serverInstanceId")
            String serverInstanceId
    ) {
        return new RedisRealtimeMessageListener(
                eventCodec,
                localDispatcher,
                echoTracker,
                properties,
                serverInstanceId
        );
    }

    @Bean(destroyMethod = "shutdown")
    ExecutorService realtimeRedisListenerExecutor() {
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
    realtimeRedisMessageListenerContainer(
            RedisMessageListenerContainerConfigurer configurer,
            RedisConnectionFactory connectionFactory,
            RedisRealtimeMessageListener listener,
            ExecutorService realtimeRedisListenerExecutor,
            RedisRealtimeProperties properties
    ) {
        if (
                properties.getMeetingChannel()
                        .equals(properties.getAuthChannel())
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

        // One listener thread preserves publication order for the first MVP.
        container.setTaskExecutor(
                realtimeRedisListenerExecutor
        );

        container.addMessageListener(
                listener,
                new ChannelTopic(
                        properties.getMeetingChannel()
                )
        );

        container.addMessageListener(
                listener,
                new ChannelTopic(
                        properties.getAuthChannel()
                )
        );

        return container;
    }
}
