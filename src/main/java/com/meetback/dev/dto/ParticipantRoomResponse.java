package com.meetback.dev.dto;

import lombok.Data;

@Data
public class ParticipantRoomResponse {
    private Long participantId;
    private Long meetingId;
    private Long userId;
    private String nickname;
    private String inputStatus;
    private String departureName;
    private String returnName;
    private boolean online;
}
