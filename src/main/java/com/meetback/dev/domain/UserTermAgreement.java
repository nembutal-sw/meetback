package com.meetback.dev.domain;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserTermAgreement {

    private Long agreementId;
    private Long userId;
    private Long termId;
    private Boolean agreed;
    private LocalDateTime agreedAt;
    private LocalDateTime revokedAt;
    private LocalDateTime createdAt;
}
