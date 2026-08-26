package com.meetback.dev.dto.auth;

import lombok.Data;

@Data
public class SocialSignupCompleteRequest {
    private String signupToken;
    private String nickname;
}
