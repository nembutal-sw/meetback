package com.meetback.dev.dto.feed;

import lombok.Data;

@Data
public class FeedUpdateRequest {
    private String title;
    private String content;
}