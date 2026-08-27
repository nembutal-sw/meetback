package com.meetback.dev.service;

import com.meetback.dev.dto.admin.AdminDailyCount;
import com.meetback.dev.dto.admin.AdminDashboardResponse;
import com.meetback.dev.repository.AdminDashboardMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/** 대시보드 통계와 최근 현황을 조합한다. */
@Service
@RequiredArgsConstructor
public class AdminDashboardService {

    private static final int RECENT_SIZE = 5;
    private static final int TREND_DAYS = 7;

    private final AdminDashboardMapper adminDashboardMapper;

    @Transactional(readOnly = true)
    public AdminDashboardResponse getDashboard() {
        LocalDate start = LocalDate.now().minusDays(TREND_DAYS - 1L);

        AdminDashboardResponse response = new AdminDashboardResponse();
        response.setSummary(adminDashboardMapper.findSummary());
        response.setRecentUsers(adminDashboardMapper.findRecentUsers(RECENT_SIZE));
        response.setRecentMeetings(adminDashboardMapper.findRecentMeetings(RECENT_SIZE));
        response.setUserTrend(fillTrend(
                start,
                adminDashboardMapper.findUserTrend(start.atStartOfDay())
        ));
        response.setMeetingTrend(fillTrend(
                start,
                adminDashboardMapper.findMeetingTrend(start.atStartOfDay())
        ));
        return response;
    }

    private List<AdminDailyCount> fillTrend(
            LocalDate start,
            List<AdminDailyCount> trend
    ) {
        // 데이터가 없는 날짜도 차트에 0건으로 표시한다.
        Map<LocalDate, AdminDailyCount> byDate = trend.stream()
                .collect(Collectors.toMap(
                        AdminDailyCount::getDate,
                        Function.identity()
                ));

        return IntStream.range(0, TREND_DAYS)
                .mapToObj(day -> {
                    LocalDate date = start.plusDays(day);
                    AdminDailyCount item = byDate.get(date);
                    if (item != null) {
                        return item;
                    }

                    AdminDailyCount empty = new AdminDailyCount();
                    empty.setDate(date);
                    empty.setCount(0);
                    return empty;
                })
                .toList();
    }
}
