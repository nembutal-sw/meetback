package com.meetback.dev.domain;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Meeting {
    private Long meetingId;
    private Long hostUserId;
    private String title;
    private MeetingType meetingType;
    private MeetingStatus status;
    private LocalDateTime desiredEndAt;
    private Integer calculationVersion;
    private String inviteCode;
    private Long finalCandidateId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private MeetingType meetingType;
}