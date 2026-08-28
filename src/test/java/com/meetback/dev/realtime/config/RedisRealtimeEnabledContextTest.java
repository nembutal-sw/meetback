package com.meetback.dev.realtime.config;

import com.meetback.dev.realtime.publisher.RealtimeEventPublisher;
import com.meetback.dev.realtime.redis.RedisRealtimeMessageListener;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        properties = {
                "meetback.realtime.redis.enabled=true",
                "spring.data.redis.listener.auto-startup=false"
        }
)
class RedisRealtimeEnabledContextTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void createsOneConfiguredListenerContainerWithoutConnectingToRedis() {
        assertThat(
                applicationContext.getBeansOfType(
                        RedisMessageListenerContainer.class
                )
        )
                .containsOnlyKeys(
                        "redisMessageListenerContainer"
                );

        assertThat(
                applicationContext.getBean(
                        RedisRealtimeMessageListener.class
                )
        ).isNotNull();

        assertThat(
                applicationContext.getBean(
                        RealtimeEventPublisher.class
                )
        ).isNotNull();
    }
}
