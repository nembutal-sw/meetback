package com.meetback.dev.config;

import com.meetback.dev.WebSocket.WebSocketSessionControl;
import com.meetback.dev.security.JwtChannelInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtChannelInterceptor jwtChannelInterceptor;
    private final WebSocketSessionControl webSocketSessionControl;

    @Override
    public void registerStompEndpoints(
            StompEndpointRegistry registry
    ){
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*");
    }

    @Override
    public void configureMessageBroker(
            MessageBrokerRegistry registry
    )
    {
        registry.setApplicationDestinationPrefixes(
                "/app"
        );

        registry.enableSimpleBroker(
                "/topic"
        )
        .setHeartbeatValue(
                new long[]{5000,5000}
        )
        .setTaskScheduler(
                stompHeartbeatTaskScheduler()
        );
    }

    @Bean
    public ThreadPoolTaskScheduler
    stompHeartbeatTaskScheduler()
    {
        ThreadPoolTaskScheduler scheduler =
                new ThreadPoolTaskScheduler();

        scheduler.setPoolSize(1);

        scheduler.setThreadNamePrefix(
                "stomp-heartbeat-"
        );

        scheduler.setRemoveOnCancelPolicy(
                true
        );

        return scheduler;
    }

    @Override
    public void configureClientInboundChannel(
            ChannelRegistration registration
    )
    {
        registration.interceptors(
                jwtChannelInterceptor
        );
    }

    @Override
    public void configureWebSocketTransport(
            WebSocketTransportRegistration registration
    ) {
        registration.addDecoratorFactory(
                webSocketSessionControl
        );
    }

}
