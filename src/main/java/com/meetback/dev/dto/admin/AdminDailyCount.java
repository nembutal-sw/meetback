package com.meetback.dev.dto.admin;

import lombok.Data;

import java.time.LocalDate;

/** 대시보드의 날짜별 집계 항목. */
@Data
public class AdminDailyCount {
    private LocalDate date;
    private long count;
}
