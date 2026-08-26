package com.meetback.dev.dto.admin;

/** 관리자 약관 버전 등록 요청. */
public record AdminTermCreateRequest(
        String termCode,
        String termName,
        String version,
        Boolean required,
        String content
) {
}
