package com.meetback.dev.controller;

import com.meetback.dev.dto.auth.*;
import com.meetback.dev.security.AuthenticatedUser;
import com.meetback.dev.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;


    // 회원가입

    @PostMapping("/signup")
    public ResponseEntity<Void> signup(
            @RequestBody SignupRequest request
    ) {

        authService.signup(
                request
        );


        return ResponseEntity
                .status(
                        HttpStatus.CREATED
                )
                .build();
    }


    // 일반 로그인

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @RequestBody LoginRequest request
    ) {

        LoginResponse response =
                authService.login(
                        request
                );


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


        return ResponseEntity.ok(
                response
        );
    }


    // 구글 로그인 / 간편 회원가입

    @PostMapping(
            value = "/google",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<LoginResponse> googleLogin(
            @RequestBody GoogleLoginRequest request
    ) {

        LoginResponse response =
                authService.googleLogin(
                        request
                );


        return ResponseEntity.ok(
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
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {

        authService.logout(
                authenticatedUser.userId()
        );


        return ResponseEntity
                .ok()
                .build();
    }


    // 회원탈퇴 요청

    @DeleteMapping("/withdraw")
    public ResponseEntity<Void> withdraw(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {

        authService.withdraw(
                authenticatedUser.userId()
        );


        return ResponseEntity
                .ok()
                .build();
    }


    // 회원탈퇴 취소

    @PostMapping("/withdraw/cancel")
    public ResponseEntity<Void> cancelWithdrawal(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {

        authService.cancelWithdrawal(
                authenticatedUser.userId()
        );


        return ResponseEntity
                .ok()
                .build();
    }


    // 로그인 상태 및 사용자 정보 확인

    @GetMapping("/check")
    public ResponseEntity<AuthCheckResponse> checkLogin(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {

        AuthCheckResponse response =
                authService.getCurrentUser(
                        authenticatedUser.userId()
                );


        return ResponseEntity.ok(
                response
        );
    }
}