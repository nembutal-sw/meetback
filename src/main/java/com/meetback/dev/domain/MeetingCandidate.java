package com.meetback.dev.domain;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class MeetingCandidate {

    private Long candidateId;

    private Long meetingId;
    private Long proposerParticipantId;

    private String placeName;
    private String address;

    private BigDecimal latitude;
    private BigDecimal longitude;

    private Boolean isActive;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}
