package com.meetback.dev.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class PageController {

    // Client ID는 공개 식별자이며, 브라우저의 Google 공식 버튼 초기화에 사용한다.
    private final String googleClientId;

    // NAVER Maps Dynamic Map에서 사용하는 공개 Client ID
    private final String naverMapsClientId;


    public PageController(
            @Value("${google.client-id:}") String googleClientId,
            @Value("${naver.maps.client-id:}") String naverMapsClientId
    ) {

        this.googleClientId = googleClientId;
        this.naverMapsClientId = naverMapsClientId;
    }


    @GetMapping("/")
    public String home()
    {
        return "redirect:/home";
    }


    @GetMapping("/quick-meetings")
    public String quickMeetings()
    {
        return "auth/quick-meetings";
    }

    @GetMapping("/login")
    public String login(
            Model model
    )
    {
        model.addAttribute(
                "googleClientId",
                googleClientId
        );

        model.addAttribute(
                "googleLoginEnabled",
                // 설정이 없는 개발 환경에서는 잘못된 Google 버튼을 렌더링하지 않는다.
                !googleClientId.isBlank()
        );

        return "auth/login";
    }


    @GetMapping("/signup")
    public String signup()
    {
        return "auth/signup";
    }


    // 아이디 찾기
    @GetMapping("/find-email")
    public String findEmail()
    {
        return "auth/findEmail";
    }


    // 비밀번호 찾기
    @GetMapping("/forgot-password")
    public String forgotPassword()
    {
        return "auth/forgotPassword";
    }


    // 비밀번호 재설정
    @GetMapping("/reset-password")
    public String resetPassword(
            @RequestParam("token") String token,
            Model model
    )
    {
        model.addAttribute(
                "token",
                token
        );

        return "auth/resetPassword";
    }


    @GetMapping("/home")
    public String homepage()
    {
        return "auth/home";
    }


    @GetMapping("/meeting/result")
    public String meetingResult(
            Model model
    )
    {
        model.addAttribute(
                "naverMapsClientId",
                naverMapsClientId
        );

        return "meeting/result";
    }
}