package com.meetback.dev.domain;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FeedImage {
    private Long feedImageId;
    private Long feedId;
    private String imageUrl;
    private String originalName;
    private String storedName;
    private Integer sortOrder;
    private LocalDateTime createdAt;
}
