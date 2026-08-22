package com.meetback.dev.oauth;

import lombok.Data;

@Data
public class GoogleUserInfo {

    // Google ID Token의 sub 값. 이메일 대신 변하지 않는 소셜 계정 식별자로 사용한다.
    private String providerId;
    private String email;
    private Boolean emailVerified;

    // Google의 name 클레임을 MeetBack에서는 별도 이름이 아닌 닉네임으로만 사용한다.
    private String nickname;
}
