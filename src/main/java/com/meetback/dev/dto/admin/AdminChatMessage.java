package com.meetback.dev.dto.admin;

import lombok.Data;

import java.time.LocalDateTime;

/** 관리자용 채팅 조회 항목. */
@Data
public class AdminChatMessage {
    private Long messageId;
    private Long meetingId;
    private Long participantId;
    private Long userId;
    private String nickname;
    private String messageType;
    private String eventType;
    private String content;
    private LocalDateTime createdAt;
}
