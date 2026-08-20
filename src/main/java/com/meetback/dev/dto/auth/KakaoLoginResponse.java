package com.meetback.dev.dto.auth;

import lombok.Data;

@Data
public class KakaoLoginResponse {

    private String accessToken;
    private String refreshToken;
    private Long userId;
    private String role;

}
