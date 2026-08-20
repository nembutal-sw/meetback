package com.meetback.dev.domain;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RefreshToken {

    private Long refreshTokenId;
    private Long userId;
    private String tokenHash;
    private LocalDateTime expiresAt;
    private LocalDateTime createAt;
    private LocalDateTime updateAt;

}
