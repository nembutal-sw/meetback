package com.meetback.dev.repository;

import com.meetback.dev.dto.admin.AdminDailyCount;
import com.meetback.dev.dto.admin.AdminDashboardSummary;
import com.meetback.dev.dto.admin.AdminRecentMeeting;
import com.meetback.dev.dto.admin.AdminRecentUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/** 관리자 대시보드 조회 전용 Mapper. */
@Mapper
public interface AdminDashboardMapper {
    AdminDashboardSummary findSummary();

    List<AdminRecentUser> findRecentUsers(@Param("size") int size);

    List<AdminRecentMeeting> findRecentMeetings(@Param("size") int size);

    List<AdminDailyCount> findUserTrend(@Param("from") LocalDateTime from);

    List<AdminDailyCount> findMeetingTrend(@Param("from") LocalDateTime from);
}
