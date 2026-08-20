package com.meetback.dev.domain;

import lombok.Data;

import java.time.LocalDateTime;


@Data
public class MeetingParticipant {
    private Long participantId;
    private Long meetingId;
    private Long userId;
    private String inputStatus;
    private String departureName;
    private String departureAddress;
    private Double departureLatitude;
    private Double departureLongitude;
    private String returnName;
    private String returnAddress;
    private Double returnLatitude;
    private Double returnLongitude;
    private LocalDateTime submittedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
