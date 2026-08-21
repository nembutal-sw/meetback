package com.meetback.dev.controller;

import com.meetback.dev.dto.auth.*;
import com.meetback.dev.security.JwtProvider;
import com.meetback.dev.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtProvider jwtProvider;

    //회원가입

    @PostMapping("/signup")
    public ResponseEntity<Void> signup(
            @RequestBody SignupRequest request
    ) {

        authService.signup(request);

        return  ResponseEntity
                .status(HttpStatus.CREATED)
                .build();
    }

    // 일반로그인

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @RequestBody LoginRequest request
    ) {
        LoginResponse response =
                authService.login(request);

        return ResponseEntity.ok(
                response
        );
    }

    // 카카오 로그인

    @PostMapping("/kakao")
    public ResponseEntity<KakaoLoginResponse> kakaoLogin(
            @RequestBody KakaoLoginRequest request
    ) {

        KakaoLoginResponse response =
                authService.kakaoLogin(
                        request
                );

        return  ResponseEntity.ok(
                response
        );
    }

    // Refresh Token 재발급

    @PostMapping("/refresh")
    public ResponseEntity<TokenRefreshResponse> refresh(
            @RequestBody TokenRefreshRequest request
    ) {

        TokenRefreshResponse response =
                authService.refresh(
                        request
                );

        return ResponseEntity.ok(
                response
        );
    }

    // 로그아웃

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @RequestHeader("Authorization") String authorization
    ) {

        Long userId =
                getUserIdFromAuthorization(
                        authorization
                );

        authService.logout(
                userId
        );

        return ResponseEntity
                .ok()
                .build();
    }

    // 회원탈퇴 요청

    @DeleteMapping("/withdraw")
    public ResponseEntity<Void> withdraw(
            @RequestHeader("Authorization") String authorization
    ) {

        Long userId =
                getUserIdFromAuthorization(
                        authorization
                );

        authService.withdraw(
                userId
        );

        return ResponseEntity
                .ok()
                .build();
    }

    // 회원탈퇴 취소

    @PostMapping("/withdraw/cancel")
    public ResponseEntity<Void> cancelWithdrawal(
            @RequestHeader("Authorization") String authorization
    ) {

        Long userId =
                getUserIdFromAuthorization(
                        authorization
                );

        authService.cancelWithdrawal(
                userId
        );

        return ResponseEntity
                .ok()
                .build();
    }

    // 로그인 상태 확인
    // 로그인 상태 및 사용자 정보 확인

    @GetMapping("/check")
    public ResponseEntity<AuthCheckResponse> checkLogin(
            @RequestHeader("Authorization") String authorization
    ) {

        Long userId =
                getUserIdFromAuthorization(
                        authorization
                );


        AuthCheckResponse response =
                authService.getCurrentUser(
                        userId
                );


        return ResponseEntity.ok(
                response
        );
    }


    // Authorization Header에서 userId 추출

    private Long getUserIdFromAuthorization(
            String authorization
    ) {

        if (authorization == null
                || !authorization.startsWith("Bearer ")) {

            throw new IllegalArgumentException(
                    "Authorization Header가 올바르지 않습니다."
            );
        }


        String accessToken =
                authorization.substring(7);


        // Access Token 유효성 검사
        if (!jwtProvider.validateToken(
                accessToken
        )) {

            throw new IllegalArgumentException(
                    "유효하지 않은 Access Token입니다."
            );
        }


        // Refresh Token을 Access Token 자리에 사용하는 것 방지
        if (!"ACCESS".equals(
                jwtProvider.getTokenType(
                        accessToken
                )
        )) {

            throw new IllegalArgumentException(
                    "Access Token이 아닙니다."
            );
        }


        return jwtProvider.getUserId(
                accessToken
        );
    }
}