package com.meetback.dev.dto.auth;

import lombok.Data;

@Data
public class SignupRequest {

    private String email;
    private String nickname;
    private String password;
}
