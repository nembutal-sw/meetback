package com.meetback.dev.domain;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class User {
    private Long userId;
    private String email;
    private String nickname;
    private String passwordHash;
    private String role;
    private UserStatus status;
    private Integer tokenVersion;
    private LocalDateTime deletedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
