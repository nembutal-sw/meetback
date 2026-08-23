package com.meetback.dev.dto.auth;

import lombok.Data;

@Data
public class TokenRefreshResponse {

    private String accessToken;
    private String refreshToken;
}
