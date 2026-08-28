package com.meetback.dev.dto.feed;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
public class FeedResponse {
    private Long feedId;
    private Long userId;
    private String nickname;
    private boolean mine;
    private String title;
    private String content;
    private List<FeedImageResponse> images;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}