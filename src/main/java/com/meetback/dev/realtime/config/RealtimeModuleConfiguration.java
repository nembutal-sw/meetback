package com.meetback.dev.realtime.config;

import com.meetback.dev.realtime.gateway.LocalRealtimeDispatcher;
import com.meetback.dev.realtime.gateway.LocalRealtimeGateway;
import com.meetback.dev.realtime.gateway.LocalSessionControl;
import com.meetback.dev.realtime.gateway.SimpLocalRealtimeGateway;
import com.meetback.dev.realtime.publisher.DefaultRealtimeEventPublisher;
import com.meetback.dev.realtime.publisher.RealtimeEventPublisher;
import com.meetback.dev.realtime.redis.JacksonRealtimeEventCodec;
import com.meetback.dev.realtime.redis.LocalRealtimeEchoTracker;
import com.meetback.dev.realtime.redis.RealtimeEventCodec;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import tools.jackson.databind.ObjectMapper;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(
        RedisRealtimeProperties.class
)
public class RealtimeModuleConfiguration {

    @Bean
    @ConditionalOnMissingBean(RealtimeEventCodec.class)
    RealtimeEventCodec realtimeEventCodec(
            ObjectMapper objectMapper
    ) {
        return new JacksonRealtimeEventCodec(
                objectMapper
        );
    }

    @Bean
    @ConditionalOnMissingBean(LocalRealtimeGateway.class)
    LocalRealtimeGateway localRealtimeGateway(
            SimpMessagingTemplate messagingTemplate
    ) {
        return new SimpLocalRealtimeGateway(
                messagingTemplate
        );
    }

    @Bean
    @ConditionalOnMissingBean(LocalRealtimeDispatcher.class)
    LocalRealtimeDispatcher localRealtimeDispatcher(
            LocalRealtimeGateway realtimeGateway,
            ObjectProvider<LocalSessionControl>
                    sessionControlProvider
    ) {
        return new LocalRealtimeDispatcher(
                realtimeGateway,
                sessionControlProvider
        );
    }

    @Bean
    @ConditionalOnMissingBean(LocalRealtimeEchoTracker.class)
    LocalRealtimeEchoTracker localRealtimeEchoTracker() {
        return new LocalRealtimeEchoTracker();
    }

    @Bean
    @ConditionalOnMissingBean(RealtimeEventPublisher.class)
    RealtimeEventPublisher realtimeEventPublisher(
            LocalRealtimeDispatcher localDispatcher,
            ObjectProvider<StringRedisTemplate>
                    redisTemplateProvider,
            RealtimeEventCodec eventCodec,
            LocalRealtimeEchoTracker echoTracker,
            RedisRealtimeProperties properties,
            @Qualifier("serverInstanceId")
            String serverInstanceId
    ) {
        return new DefaultRealtimeEventPublisher(
                localDispatcher,
                redisTemplateProvider,
                eventCodec,
                echoTracker,
                properties,
                serverInstanceId
        );
    }
}
