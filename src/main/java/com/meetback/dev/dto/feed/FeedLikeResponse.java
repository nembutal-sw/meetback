package com.meetback.dev.dto.feed;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FeedLikeResponse {
    private Long feedId;
    private int likeCount;
    private  boolean liked;
}
