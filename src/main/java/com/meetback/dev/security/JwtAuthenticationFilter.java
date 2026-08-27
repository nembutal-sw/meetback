package com.meetback.dev.security;

import com.meetback.dev.domain.User;
import com.meetback.dev.domain.UserStatus;
import com.meetback.dev.repository.UserMapper;
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
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final UserMapper userMapper;


    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String authorization =
                request.getHeader(
                        "Authorization"
                );


        // ---------------------------------------------------------
        // Authorization Header가 없는 경우
        // 로그인/회원가입/토큰 재발급 같은 공개 API도 있으므로
        // 그냥 다음 필터로 넘긴다.
        // ---------------------------------------------------------
        if (authorization == null
                || !authorization.startsWith(
                "Bearer "
        )) {

            filterChain.doFilter(
                    request,
                    response
            );

            return;
        }


        // ---------------------------------------------------------
        // Bearer 뒤 JWT 추출
        // ---------------------------------------------------------
        String token =
                authorization.substring(
                        7
                );


        // ---------------------------------------------------------
        // JWT 자체 검증
        // 서명 오류 / 만료 / 형식 오류 등
        // ---------------------------------------------------------
        if (!jwtProvider.validateToken(
                token
        )) {

            rejectUnauthorized(
                    response,
                    "유효하지 않거나 만료된 Access Token입니다."
            );

            return;
        }


        // ---------------------------------------------------------
        // Access Token인지 확인
        // Refresh Token으로 API 접근하는 것 방지
        // ---------------------------------------------------------
        String tokenType =
                jwtProvider.getTokenType(
                        token
                );


        if (!"ACCESS".equals(
                tokenType
        )) {

            rejectUnauthorized(
                    response,
                    "Access Token이 아닙니다."
            );

            return;
        }


        // ---------------------------------------------------------
        // JWT Claim 조회
        // ---------------------------------------------------------
        Long userId =
                jwtProvider.getUserId(
                        token
                );


        String tokenRole =
                jwtProvider.getRole(
                        token
                );


        Integer tokenVersion =
                jwtProvider.getTokenVersion(
                        token
                );


        if (userId == null
                || tokenRole == null
                || tokenRole.isBlank()
                || tokenVersion == null) {

            rejectUnauthorized(
                    response,
                    "Access Token 사용자 정보가 올바르지 않습니다."
            );

            return;
        }


        // ---------------------------------------------------------
        // DB 사용자 조회
        // ---------------------------------------------------------
        User user =
                userMapper.selectById(
                        userId
                );


        if (user == null) {

            rejectUnauthorized(
                    response,
                    "사용자 정보를 확인할 수 없습니다."
            );

            return;
        }


        // 상태가 없거나 ACTIVE가 아니면 인증 객체를 만들지 않는다.
        if (user.getStatus() != UserStatus.ACTIVE) {

            rejectForbidden(
                    response,
                    "ACCOUNT_SUSPENDED",
                    "이용이 정지된 계정입니다."
            );

            return;
        }


        if (user.getTokenVersion() == null) {

            rejectUnauthorized(
                    response,
                    "사용자 정보를 확인할 수 없습니다."
            );

            return;
        }


        // =========================================================
        // ★ 핵심 1 : Token Version 확인
        //
        // A 로그인
        // A JWT version = 5
        //
        // B가 동일 계정 로그인
        // DB version = 6
        // B JWT version = 6
        //
        // A가 다시 요청
        // JWT 5 != DB 6
        // → 여기서 바로 401
        //
        // 즉 기존 기기 A 강제 로그아웃
        // =========================================================
        if (!tokenVersion.equals(
                user.getTokenVersion()
        )) {

            rejectUnauthorized(
                    response,
                    "다른 기기에서 로그인되어 현재 로그인 세션이 만료되었습니다."
            );

            return;
        }


        // ---------------------------------------------------------
        // Role 확인
        //
        // JWT 안의 role과
        // 현재 DB role이 달라졌다면 기존 토큰 차단
        // ---------------------------------------------------------
        if (user.getRole() == null
                || user.getRole().isBlank()
                || !tokenRole.equals(
                user.getRole()
        )) {

            rejectUnauthorized(
                    response,
                    "사용자 권한 정보가 변경되었습니다."
            );

            return;
        }


        // ---------------------------------------------------------
        // 회원탈퇴 유예기간 확인
        // ---------------------------------------------------------
        if (user.getDeletedAt() != null) {

            LocalDateTime withdrawalDeadline =
                    user.getDeletedAt()
                            .plusDays(
                                    7
                            );


            if (!LocalDateTime.now()
                    .isBefore(
                            withdrawalDeadline
                    )) {

                rejectUnauthorized(
                        response,
                        "탈퇴 처리된 계정입니다."
                );

                return;
            }
        }


        // ---------------------------------------------------------
        // Spring Security Principal 생성
        // ---------------------------------------------------------
        AuthenticatedUser authenticatedUser =
                new AuthenticatedUser(
                        user.getUserId(),
                        user.getRole()
                );


        // ---------------------------------------------------------
        // ROLE_ prefix 처리
        // ---------------------------------------------------------
        String authority =
                user.getRole()
                        .startsWith(
                                "ROLE_"
                        )
                        ? user.getRole()
                        : "ROLE_"
                        + user.getRole();


        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(
                        authenticatedUser,
                        null,
                        List.of(
                                new SimpleGrantedAuthority(
                                        authority
                                )
                        )
                );


        // ---------------------------------------------------------
        // 인증 성공
        // SecurityContext 등록
        // ---------------------------------------------------------
        SecurityContextHolder
                .getContext()
                .setAuthentication(
                        authenticationToken
                );


        filterChain.doFilter(
                request,
                response
        );
    }


    // =========================================================
    // 인증 실패 처리
    // =========================================================
    private void rejectUnauthorized(
            HttpServletResponse response,
            String message
    ) throws IOException {

        SecurityContextHolder.clearContext();

        response.setStatus(
                HttpServletResponse.SC_UNAUTHORIZED
        );

        response.setContentType(
                "application/json;charset=UTF-8"
        );

        response.getWriter().write(
                "{\"message\":\""
                        + message
                        + "\"}"
        );
    }


    private void rejectForbidden(
            HttpServletResponse response,
            String code,
            String message
    ) throws IOException {

        SecurityContextHolder.clearContext();
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(
                "{\"code\":\""
                        + code
                        + "\",\"message\":\""
                        + message
                        + "\"}"
        );
    }
}
