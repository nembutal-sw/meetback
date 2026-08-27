package com.meetback.dev.dto.admin;

import lombok.Data;

/** 관리자 대시보드 요약 통계. */
@Data
public class AdminDashboardSummary {
    private long totalUsers;
    private long activeUsers;
    private long suspendedUsers;
    private long deletedUsers;
    private long totalMeetings;
    private long ongoingMeetings;
    private long totalChatMessages;
    private long totalCandidates;
    private long totalVotes;
}
