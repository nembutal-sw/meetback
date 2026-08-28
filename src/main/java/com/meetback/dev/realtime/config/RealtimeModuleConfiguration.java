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

/**
 * 실시간 이벤트의 로컬 전달과 Redis 발행에 필요한 기본 Bean을 조립한다.
 *
 * <p>{@link ConditionalOnMissingBean}을 사용하므로 각 경계의 별도 구현이
 * 등록되어 있으면 해당 구현을 유지한다. Redis 구독 활성화 여부와 관계없이
 * 로컬 전달에 필요한 publisher와 dispatcher는 구성된다.</p>
 */
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

    /**
     * 로컬 dispatcher와 선택적인 Redis 전송 경로를
     * 하나의 발행 진입점으로 묶는다.
     * Redis template은 publisher가 발행 시점에 선택적으로 조회하고,
     * 없으면 Redis 발행 실패 결과로 처리하도록 {@link ObjectProvider}로 전달한다.
     */
    @Bean
    @ConditionalOnMissingBean(RealtimeEventPublisher.class)
    RealtimeEventPublisher realtimeEventPublisher(
            LocalRealtimeDispatcher dispatcher,
            ObjectProvider<StringRedisTemplate>
                    templateProvider,
            RealtimeEventCodec codec,
            LocalRealtimeEchoTracker echoTracker,
            RedisRealtimeProperties redisProps,
            @Qualifier("serverInstanceId")
            String instanceId
    ) {
        return new DefaultRealtimeEventPublisher(
                dispatcher,
                templateProvider,
                codec,
                echoTracker,
                redisProps,
                instanceId
        );
    }
}
