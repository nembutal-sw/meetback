package com.meetback.dev.dto.admin;

import com.meetback.dev.domain.InputStatus;
import lombok.Data;

import java.time.LocalDateTime;

/** 관리자용 모임 참여자 정보. */
@Data
public class AdminParticipant {
    private Long participantId;
    private Long userId;
    private String nickname;
    private InputStatus inputStatus;
    private String departureName;
    private String departureAddress;
    private String returnName;
    private String returnAddress;
    private LocalDateTime submittedAt;
    private LocalDateTime createdAt;
}
