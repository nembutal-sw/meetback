package com.meetback.dev.dto.auth;


import lombok.Data;

@Data
public class LoginResponse {
    private String accessToken;
    private String refreshToken;
    private Long userId;
    private String role;
}
