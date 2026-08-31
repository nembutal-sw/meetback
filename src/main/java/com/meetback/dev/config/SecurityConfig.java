package com.meetback.dev.config;

import com.meetback.dev.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        return http
                .csrf(
                        AbstractHttpConfigurer::disable
                )
                .sessionManagement(
                        session ->
                                session.sessionCreationPolicy(
                                        SessionCreationPolicy.STATELESS
                                )
                )
                .authorizeHttpRequests(
                        auth ->
                                auth

                                        // 관리자 API
                                        .requestMatchers(
                                                "/admin/api/**"
                                        )
                                        .hasAuthority(
                                                "ROLE_admin"
                                        )

                                        // 공개 페이지 / 공개 API
                                        .requestMatchers(
                                                "/",
                                                "/login",
                                                "/signup",
                                                "/find-email",
                                                "/forgot-password",
                                                "/reset-password",
                                                "/home",
                                                "/quick-meetings",
                                                "/quick-fixed",
                                                "/quick-fixed-check",
                                                "/favicon.ico",

                                                // 관리자 HTML 페이지
                                                "/admin/terms",

                                                "/auth/login",
                                                "/auth/signup",
                                                "/auth/kakao",
                                                "/auth/google",
                                                "/auth/refresh",
                                                "/auth/server-instance",
                                                "/auth/email/check",
                                                "/auth/nickname/check",
                                                "/auth/find-email",
                                                "/auth/password/reset/request",
                                                "/auth/password/reset/confirm",
                                                "/auth/social/complete",

                                                "/terms",

                                                "/oauth/**",

                                                "/ws",
                                                "/ws/**",

                                                "/css/**",
                                                "/js/**",
                                                "/images/**",
                                                "/uploads/feed/**",
                                                "/feed",
                                                "/feed/write",
                                                "/feed/**",

                                                "/meeting",
                                                "/meeting/location-test",
                                                "/meeting/location-test/**",
                                                "/meeting/location",
                                                "/meeting/vote",
                                                "/meeting/result"
                                        )
                                        .permitAll()

                                        // 로그인 사용자 전용
                                        .requestMatchers(
                                                "/auth/check",
                                                "/auth/logout",
                                                "/auth/withdraw",
                                                "/auth/withdraw/cancel"
                                        )
                                        .authenticated()

                                        .anyRequest()
                                        .authenticated()
                )
                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                )
                .build();
    }
}