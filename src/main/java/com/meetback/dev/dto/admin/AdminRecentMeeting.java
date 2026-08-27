package com.meetback.dev.dto.admin;

import com.meetback.dev.domain.MeetingStatus;
import lombok.Data;

import java.time.LocalDateTime;

/** 대시보드의 최근 생성 모임 항목. */
@Data
public class AdminRecentMeeting {
    private Long meetingId;
    private String title;
    private MeetingStatus status;
    private String hostNickname;
    private LocalDateTime createdAt;
}
