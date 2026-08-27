package com.meetback.dev.dto.admin;

import lombok.Data;

import java.util.List;

/** 관리자 대시보드 전체 응답. */
@Data
public class AdminDashboardResponse {
    private AdminDashboardSummary summary;
    private List<AdminRecentUser> recentUsers;
    private List<AdminRecentMeeting> recentMeetings;
    private List<AdminDailyCount> userTrend;
    private List<AdminDailyCount> meetingTrend;
}
