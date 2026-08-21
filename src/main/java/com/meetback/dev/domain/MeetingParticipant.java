package com.meetback.dev.domain;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class MeetingParticipant {
    private Long participantId;
    private Long meetingId;
    private Long userId;
    private InputStatus inputStatus;
    private String departureName;
    private String departureAddress;
    private BigDecimal departureLatitude;
    private BigDecimal departureLongitude;
    private String returnName;
    private String returnAddress;
    private BigDecimal returnLatitude;
    private BigDecimal returnLongitude;
    private LocalDateTime submittedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}