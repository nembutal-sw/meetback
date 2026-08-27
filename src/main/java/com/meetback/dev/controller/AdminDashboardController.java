package com.meetback.dev.controller;

import com.meetback.dev.dto.admin.AdminDashboardResponse;
import com.meetback.dev.service.AdminDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 관리자 대시보드 조회 API. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/dashboard")
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    @GetMapping
    public AdminDashboardResponse getDashboard() {
        // 관리자 화면에서 필요한 통계와 최근 현황을 한 번에 제공한다.
        return adminDashboardService.getDashboard();
    }
}
