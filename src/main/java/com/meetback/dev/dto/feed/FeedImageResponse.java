package com.meetback.dev.dto.feed;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FeedImageResponse {
    private Long feedImageId;
    private String imageUrl;
    private String originalName;
    private Integer sortOrder;
}
