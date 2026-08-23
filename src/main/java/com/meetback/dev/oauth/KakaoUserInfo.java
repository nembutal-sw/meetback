package com.meetback.dev.oauth;

import lombok.Data;

@Data
public class KakaoUserInfo {

    private String providerId;
    private String email;
    private Boolean emailVerified;
    private String name;
}
