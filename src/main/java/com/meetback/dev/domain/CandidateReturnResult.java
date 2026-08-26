package com.meetback.dev.domain;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CandidateReturnResult {
    private Long resultId;
    private Long meetingId;
    private Long candidateId;
    private Long participantId;
    private String nickname;
    private Integer calculationVersion;
    private Integer returnMinutes;
    private Integer transferCount;
    private LocalDateTime lastTrainDepartureAt;
    private LocalDateTime lastTrainArrivalAt;
    private LocalDateTime lastSafeDepartureAt;
    private Boolean canReturn;
    private LocalDateTime calculatedAt;
}