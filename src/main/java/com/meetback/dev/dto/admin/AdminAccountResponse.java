package com.meetback.dev.dto.admin;

/** 관리자 계정 편집 화면에 필요한 정보. */
public record AdminAccountResponse(
        Long userId,
        String loginId,
        String nickname
) {
}
