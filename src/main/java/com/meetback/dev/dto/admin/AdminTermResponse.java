package com.meetback.dev.dto.admin;

import java.time.LocalDateTime;

/** 관리자 약관 버전 응답. */
public record AdminTermResponse(
        Long termId,
        String termCode,
        String termName,
        String version,
        Boolean required,
        String content,
        Boolean active,
        LocalDateTime effectiveAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
