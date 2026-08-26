package com.meetback.dev.dto.auth;

import lombok.Data;

@Data
public class PasswordResetRequest {
    private String email;
}
