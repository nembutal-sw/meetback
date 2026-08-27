package com.meetback.dev.dto.admin;

/** 관리자 계정 변경 요청. */
public record AdminAccountUpdateRequest(
        String currentPassword,
        String loginId,
        String nickname,
        String newPassword,
        String newPasswordConfirm
) {
}
