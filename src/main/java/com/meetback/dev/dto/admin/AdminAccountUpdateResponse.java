package com.meetback.dev.dto.admin;

/** 계정 변경 후 현재 관리자 재로그인 필요 여부. */
public record AdminAccountUpdateResponse(
        String message,
        boolean reLoginRequired
) {
}
