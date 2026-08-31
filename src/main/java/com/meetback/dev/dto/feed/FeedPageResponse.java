package com.meetback.dev.dto.feed;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class FeedPageResponse {
    private List<FeedResponse> feeds;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
}