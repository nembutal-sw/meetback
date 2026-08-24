package com.meetback.dev.security.dev;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
public class DevSecurityConfig {

    private final DevJwtAuthenticationFilter devJwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    )throws Exception {
        return http

                .csrf(csrf ->
                        csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )
                .authorizeHttpRequests(auth -> auth

                        // =========================================
                        // 브라우저가 직접 들어가는 페이지
                        // Authorization Header 자동 첨부가 안 되므로
                        // HTML 자체는 허용
                        // =========================================

                        .requestMatchers(
                                "/",
                                "/login",
                                "/signup",
                                "/home",
                                "/meeting",
                                "/location-test"
                        )
                        .permitAll()


                        // =========================================
                        // WebSocket HTTP Handshake
                        //
                        // 실제 STOMP 인증은
                        // JwtChannelInterceptor에서 처리
                        // =========================================

                        .requestMatchers(
                                "/ws",
                                "/ws/**"
                        )
                        .permitAll()


                        // =========================================
                        // 정적 리소스
                        // =========================================

                        .requestMatchers(
                                "/css/**",
                                "/js/**",
                                "/images/**",
                                "/chat-dev.html",
                                "/stomp-test.html"
                        )
                        .permitAll()


                        // =========================================
                        // 로그인/OAuth
                        // =========================================

                        .requestMatchers(
                                "/auth/**",
                                "/oauth/**"
                        )
                        .permitAll()


                        // =========================================
                        // 나머지 API는 JWT 필요
                        // =========================================

                        .anyRequest()
                        .authenticated()
                )
                .addFilterBefore(
                        devJwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                )
                .build();

    }

}
