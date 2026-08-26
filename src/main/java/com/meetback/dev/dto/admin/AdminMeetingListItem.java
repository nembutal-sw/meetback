package com.meetback.dev.dto.admin;

import com.meetback.dev.domain.MeetingStatus;
import lombok.Data;

import java.time.LocalDateTime;

/** 관리자용 모임 목록 항목. */
@Data
public class AdminMeetingListItem {
    private Long meetingId;
    private String title;
    private MeetingStatus status;
    private String hostNickname;
    private long participantCount;
    private LocalDateTime createdAt;
}
