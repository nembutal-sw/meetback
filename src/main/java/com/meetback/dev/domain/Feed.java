package com.meetback.dev.domain;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Feed {
    private Long feedId;
    private Long userId;
    private String title;
    private String content;
    private LocalDateTime deletedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updateAt;
}
