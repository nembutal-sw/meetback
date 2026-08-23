package com.meetback.dev.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    // Client ID는 공개 식별자이며, 브라우저의 Google 공식 버튼 초기화에 사용한다.
    private final String googleClientId;

    public PageController(
            @Value("${google.client-id:}") String googleClientId
    ) {

        this.googleClientId = googleClientId;
    }

    @GetMapping("/")
    public String home()
    {
        return "redirect:/login";
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

        return "login";
    }

    @GetMapping("/signup")
    public String signup()
    {
        return "signup";
    }

    @GetMapping("/home")
    public String homepage()
    {
        return "home";
    }
}
