package com.meetback.dev.controller;

import com.meetback.dev.dto.auth.KakaoLoginRequest;
import com.meetback.dev.dto.auth.KakaoLoginResponse;
import com.meetback.dev.service.AuthService;
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


    // 카카오 로그인 시작
    @GetMapping("/oauth/kakao/login")
    public String kakaoLogin()
    {

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

        System.out.println("카카오 로그인 URL = " + kakaoLoginUrl);
        return "redirect:"
                + kakaoLoginUrl;
    }


    // 카카오 로그인 Callback
    @GetMapping("/oauth/kakao/callback")
    public String kakaoCallback(
            @RequestParam("code") String code,
            Model model
    )
    {

        KakaoLoginRequest request =
                new KakaoLoginRequest();


        request.setCode(
                code
        );


        KakaoLoginResponse response =
                authService.kakaoLogin(
                        request
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


        return "kakaologin";
    }
}