package com.meetback.dev.dto.auth;

import lombok.Data;

import java.util.List;

@Data
public class SocialSignupCompleteRequest {
    private String signupToken;
    private String nickname;
    private List<Long> agreedTermIds;
}
