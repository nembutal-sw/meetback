package com.meetback.dev.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.UUID;

@Configuration
public class ServerInstanceConfig {

    @Bean
    public String serverInstanceId() {

        return UUID
                .randomUUID()
                .toString();
    }

}
