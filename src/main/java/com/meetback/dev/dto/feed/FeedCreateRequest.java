package com.meetback.dev.dto.feed;

import lombok.Data;

@Data
public class FeedCreateRequest {
    private String title;
    private String content;
}
