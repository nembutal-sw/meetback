package com.meetback.dev.dto.auth;

import lombok.Data;

import java.util.List;

@Data
public class SignupRequest {
    private String email;
    private String nickname;
    private String password;
    private String passwordConfirm;
    private List<Long> agreedTermIds;
}
