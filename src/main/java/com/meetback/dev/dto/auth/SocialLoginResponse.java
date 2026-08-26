package com.meetback.dev.dto.auth;

import lombok.Data;

@Data
public class SocialLoginResponse {
    private String status;
    private String signupToken;
    private String accessToken;
    private String refreshToken;
    private Long userId;
    private String role;
}
