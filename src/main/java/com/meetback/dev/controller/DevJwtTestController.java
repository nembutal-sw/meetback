package com.meetback.dev.controller;


import com.meetback.dev.security.JwtProvider;
import com.meetback.dev.security.dev.DevAuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/dev/jwt")
@RequiredArgsConstructor
public class DevJwtTestController {

    private final JwtProvider jwtProvider;

    @PostMapping("/issue")
    public Map<String,String> issueToken()
    {
        String accessToken = jwtProvider.createAccessToken(
                1L,
                "USER"
        );

        String refreshToken = jwtProvider.createRefreshToken(
                1L,
                "USER"
        );

        return Map.of(
                "accessToken", accessToken,
                "refreshToken", refreshToken
        );
    }

    @GetMapping("/me")
    public DevAuthenticatedUser me(
            @AuthenticationPrincipal DevAuthenticatedUser user
    ) {
        System.out.println("===== /dev/jwt/me =====");
        System.out.println("user = " + user);

        if (user != null) {
            System.out.println("userId = " + user.userId());
            System.out.println("role = " + user.role());
            System.out.println("class = " + user.getClass().getName());
        }

        return user;
    }

}
