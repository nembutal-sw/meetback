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


    // 이메일 중복 검사
    @GetMapping("/email/check")
    public ResponseEntity<EmailCheckResponse> checkEmail(
            @RequestParam("email") String email
    ) {

        boolean available =
                authService.isEmailAvailable(
                        email
                );


        EmailCheckResponse response =
                new EmailCheckResponse(
                        available
                );


        return ResponseEntity.ok(
                response
        );
    }


    // 닉네임 중복 검사
    @GetMapping("/nickname/check")
    public ResponseEntity<NicknameCheckResponse> checkNickname(
            @RequestParam("nickname") String nickname
    ) {

        boolean available =
                authService.isNicknameAvailable(
                        nickname
                );


        NicknameCheckResponse response =
                new NicknameCheckResponse(
                        available
                );


        return ResponseEntity.ok(
                response
        );
    }


    // 아이디 찾기
    @PostMapping("/find-email")
    public ResponseEntity<FindEmailResponse> findEmail(
            @RequestBody FindEmailRequest request
    ) {

        String maskedEmail =
                authService.findEmail(
                        request.getNickname()
                );


        FindEmailResponse response =
                new FindEmailResponse(
                        maskedEmail
                );


        return ResponseEntity.ok(
                response
        );
    }


    // 비밀번호 재설정 이메일 요청
    @PostMapping("/password/reset/request")
    public ResponseEntity<Void> requestPasswordReset(
            @RequestBody PasswordResetRequest request
    ) {

        authService.requestPasswordReset(
                request.getEmail()
        );


        return ResponseEntity
                .ok()
                .build();
    }


    // 비밀번호 재설정 완료
    @PostMapping("/password/reset/confirm")
    public ResponseEntity<Void> confirmPasswordReset(
            @RequestBody PasswordResetConfirmRequest request
    ) {

        if (request.getNewPassword() == null
                || request.getNewPasswordConfirm() == null
                || !request.getNewPassword().equals(
                request.getNewPasswordConfirm()
        )) {

            throw new IllegalArgumentException(
                    "새 비밀번호가 일치하지 않습니다."
            );
        }


        authService.confirmPasswordReset(
                request.getToken(),
                request.getNewPassword()
        );


        return ResponseEntity
                .ok()
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
    public ResponseEntity<SocialLoginResponse> kakaoLogin(
            @RequestBody KakaoLoginRequest request
    ) {

        SocialLoginResponse response =
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
    public ResponseEntity<SocialLoginResponse> googleLogin(
            @RequestBody GoogleLoginRequest request
    ) {

        SocialLoginResponse response =
                authService.googleLogin(
                        request
                );


        return ResponseEntity.ok(
                response
        );
    }


    // 소셜 신규 회원가입 완료
    @PostMapping("/social/complete")
    public ResponseEntity<SocialLoginResponse> completeSocialSignup(
            @RequestBody SocialSignupCompleteRequest request
    ) {

        SocialLoginResponse response =
                authService.completeSocialSignup(
                        request.getSignupToken(),
                        request.getNickname(),
                        request.getAgreedTermIds()
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