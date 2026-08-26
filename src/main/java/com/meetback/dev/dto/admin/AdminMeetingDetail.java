package com.meetback.dev.dto.admin;

import com.meetback.dev.domain.MeetingStatus;
import lombok.Data;

import java.time.LocalDateTime;

/** 관리자용 모임 상세 정보. */
@Data
public class AdminMeetingDetail {
    private Long meetingId;
    private Long hostUserId;
    private String hostNickname;
    private String title;
    private MeetingStatus status;
    private String inviteCode;
    private LocalDateTime desiredEndAt;
    private long participantCount;
    private Long finalCandidateId;
    private String finalPlaceName;
    private String finalAddress;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
