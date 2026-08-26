package com.meetback.dev.dto.admin;

import com.meetback.dev.domain.UserStatus;
import lombok.Data;

import java.time.LocalDateTime;

/** 관리자용 회원 상세 정보. */
@Data
public class AdminUserDetail {
    private Long userId;
    private String email;
    private String nickname;
    private String role;
    private UserStatus status;
    private LocalDateTime deletedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
