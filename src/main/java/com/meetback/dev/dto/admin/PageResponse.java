package com.meetback.dev.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

/** 목록 API가 공통으로 사용하는 페이지 응답. */
@Getter
@AllArgsConstructor
public class PageResponse<T> {
    private final List<T> items;
    private final long total;
    private final int page;
    private final int size;
}
