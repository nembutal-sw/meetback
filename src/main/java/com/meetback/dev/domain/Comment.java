package com.meetback.dev.domain;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Comment {
    private Long commentId;
    private Long feedId;
    private Long userId;
    private String nickname;
    private String content;
    private LocalDateTime deletedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}