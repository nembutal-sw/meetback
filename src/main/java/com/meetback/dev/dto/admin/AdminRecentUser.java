package com.meetback.dev.dto.admin;

import com.meetback.dev.domain.UserStatus;
import lombok.Data;

import java.time.LocalDateTime;

/** 대시보드의 최근 가입 회원 항목. */
@Data
public class AdminRecentUser {
    private Long userId;
    private String email;
    private String nickname;
    private String role;
    private UserStatus status;
    private LocalDateTime createdAt;
}
