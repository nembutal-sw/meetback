package com.meetback.dev.controller;

import com.meetback.dev.dto.auth.KakaoLoginRequest;
import com.meetback.dev.dto.auth.SocialLoginResponse;
import com.meetback.dev.service.AuthService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriComponentsBuilder;

@Controller
@RequiredArgsConstructor
public class KakaoOAuthController {

    private final AuthService authService;

    @Value("${kakao.client-id}")
    private String clientId;

    @Value("${kakao.redirect-uri}")
    private String redirectUri;


    // ============================================================
    // 카카오 로그인 시작
    // ============================================================

    @GetMapping("/oauth/kakao/login")
    public String kakaoLogin(
            @RequestParam(
                    value = "redirect",
                    required = false
            ) String redirect,
            HttpSession session
    ) {

        /*
         * 초대 링크 등을 통해 로그인한 경우
         *
         * 예:
         * /oauth/kakao/login
         * ?redirect=/home?inviteCode=4687A215
         *
         * 카카오 사이트를 다녀오는 동안
         * redirect 값이 사라지지 않도록 Session에 임시 저장한다.
         */
        if (isValidRedirect(redirect)) {

            session.setAttribute(
                    "kakaoLoginRedirect",
                    redirect
            );

        } else {

            session.removeAttribute(
                    "kakaoLoginRedirect"
            );
        }


        String kakaoLoginUrl =
                UriComponentsBuilder
                        .fromUriString(
                                "https://kauth.kakao.com/oauth/authorize"
                        )
                        .queryParam(
                                "response_type",
                                "code"
                        )
                        .queryParam(
                                "client_id",
                                clientId
                        )
                        .queryParam(
                                "redirect_uri",
                                redirectUri
                        )
                        .queryParam(
                                "prompt",
                                "login"
                        )
                        .build()
                        .toUriString();


        System.out.println(
                "카카오 로그인 URL = "
                        + kakaoLoginUrl
        );


        return "redirect:"
                + kakaoLoginUrl;
    }


    // ============================================================
    // 카카오 로그인 Callback
    // ============================================================

    @GetMapping("/oauth/kakao/callback")
    public String kakaoCallback(
            @RequestParam("code") String code,
            HttpSession session,
            Model model
    ) {

        KakaoLoginRequest request =
                new KakaoLoginRequest();


        request.setCode(
                code
        );


        SocialLoginResponse response =
                authService.kakaoLogin(
                        request
                );


        model.addAttribute(
                "status",
                response.getStatus()
        );


        model.addAttribute(
                "signupToken",
                response.getSignupToken()
        );


        model.addAttribute(
                "accessToken",
                response.getAccessToken()
        );


        model.addAttribute(
                "refreshToken",
                response.getRefreshToken()
        );


        model.addAttribute(
                "userId",
                response.getUserId()
        );


        model.addAttribute(
                "role",
                response.getRole()
        );


        /*
         * 카카오 로그인 시작 전에 저장해둔
         * 원래 이동 목적지 복구
         */
        String redirect =
                (String) session.getAttribute(
                        "kakaoLoginRedirect"
                );


        /*
         * 한 번 사용했으므로 바로 제거
         */
        session.removeAttribute(
                "kakaoLoginRedirect"
        );


        if (!isValidRedirect(redirect)) {

            redirect =
                    "/home";
        }


        model.addAttribute(
                "redirect",
                redirect
        );


        return "auth/kakaologin";
    }


    // ============================================================
    // Redirect URL 검증
    //
    // 외부 URL로 이동시키는 Open Redirect 방지
    //
    // 허용:
    // /home
    // /home?inviteCode=ABC123
    // /meeting?meetingId=1
    //
    // 차단:
    // https://evil.com
    // //evil.com
    // ============================================================

    private boolean isValidRedirect(
            String redirect
    ) {

        if (
                redirect == null
                        ||
                        redirect.isBlank()
        ) {

            return false;
        }


        return redirect.startsWith("/")
                &&
                !redirect.startsWith("//");
    }
}