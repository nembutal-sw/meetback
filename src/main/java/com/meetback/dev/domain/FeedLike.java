package com.meetback.dev.domain;

import lombok.Data;
import org.springframework.cglib.core.Local;

import java.time.LocalDateTime;

@Data
public class FeedLike {
    private Long likeId;
    private Long feedId;
    private Long userId;
    private LocalDateTime createdAt;
}
