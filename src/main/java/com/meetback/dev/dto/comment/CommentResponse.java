package com.meetback.dev.dto.comment;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class CommentResponse {
    private Long commentId;
    private Long FeedId;
    private Long userId;
    private String nickname;
    private boolean mine;
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
