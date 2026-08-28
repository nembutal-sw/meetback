package com.meetback.dev.domain;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ParticipantKickHistory {
    private Long kickHistoryId;
    private Long meetingId;
    private Long participantId;
    private Long kickedUserId;
    private Long kickedByUserId;
    private LocalDateTime kickedAt;
    private Long canceledByUserId;
    private LocalDateTime canceledAt;
}
