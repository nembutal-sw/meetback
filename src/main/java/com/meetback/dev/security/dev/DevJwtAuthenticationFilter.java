package com.meetback.dev.security.dev;

import com.meetback.dev.security.JwtProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

// ============================================================
// [TEMP-BKW-DEBUG]
// POST /meetings 403 원인 확인을 위한 임시 로그.
// 인증 문제 해결 후 System.out.println 부분 제거.
// 범석 Security 코드 병합 시 DevJwtAuthenticationFilter 전체 삭제.
// ============================================================

@Component
@RequiredArgsConstructor
public class DevJwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String authorization =
                request.getHeader("Authorization");

        System.out.println();
        System.out.println("==========================================");
        System.out.println("[DEV JWT FILTER]");
        System.out.println("METHOD = " + request.getMethod());
        System.out.println("URI = " + request.getRequestURI());
        System.out.println(
                "Authorization 존재 = "
                        + (authorization != null)
        );

        if (authorization != null
                && authorization.startsWith("Bearer ")) {

            String token =
                    authorization.substring(7);

            boolean valid =
                    jwtProvider.validateToken(token);

            System.out.println(
                    "JWT validate = " + valid
            );

            if (valid) {

                String tokenType =
                        jwtProvider.getTokenType(token);

                System.out.println(
                        "tokenType = " + tokenType
                );

                if ("ACCESS".equals(tokenType)) {

                    Long userId =
                            jwtProvider.getUserId(token);

                    String role =
                            jwtProvider.getRole(token);

                    System.out.println(
                            "userId = " + userId
                    );

                    System.out.println(
                            "role = " + role
                    );

                    DevAuthenticatedUser user =
                            new DevAuthenticatedUser(
                                    userId,
                                    role
                            );

                    String authority =
                            role != null
                                    && role.startsWith("ROLE_")
                                    ? role
                                    : "ROLE_" + role;

                    UsernamePasswordAuthenticationToken authenticationToken =
                            new UsernamePasswordAuthenticationToken(
                                    user,
                                    null,
                                    List.of(
                                            new SimpleGrantedAuthority(
                                                    authority
                                            )
                                    )
                            );

                    SecurityContextHolder
                            .getContext()
                            .setAuthentication(
                                    authenticationToken
                            );

                    System.out.println(
                            "Authentication 설정 완료 = "
                                    + SecurityContextHolder
                                    .getContext()
                                    .getAuthentication()
                    );
                }
                else {

                    System.out.println(
                            "ACCESS 토큰이 아님"
                    );
                }
            }
            else {

                System.out.println(
                        "JWT 검증 실패"
                );
            }
        }
        else {

            System.out.println(
                    "Bearer Authorization 헤더 없음"
            );
        }

        System.out.println(
                "FilterChain으로 다음 단계 진행"
        );
        System.out.println("==========================================");

        filterChain.doFilter(
                request,
                response
        );
    }
}
