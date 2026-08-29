package com.meetback.dev.dto;

import com.meetback.dev.domain.MeetingType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MeetingCreateRequest {
    private String title;
    private MeetingType meetingType;
    private LocalDateTime desiredEndAt;
    private LocalDateTime meetingStartAt;
    private CandidateRequestDTO fixedPlace;
}
