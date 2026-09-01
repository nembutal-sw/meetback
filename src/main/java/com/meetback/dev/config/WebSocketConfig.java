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
        );
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
