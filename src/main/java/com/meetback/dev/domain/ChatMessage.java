package com.meetback.dev.domain;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChatMessage {
    private Long messageId;
    private Long meetingId;
    private Long participantId;
    private String messageType;
    private String eventType;
    private String content;
    private LocalDateTime createdAt;
}
