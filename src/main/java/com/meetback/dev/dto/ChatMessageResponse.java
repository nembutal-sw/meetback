package com.meetback.dev.dto;

import java.time.LocalDateTime;

public record ChatMessageResponse
    (
    Long messageId,
    Long meetingId,
    Long participantId,
    Long userId,
    String nickname,
    String messageType,
    String eventType,
    String content,
    LocalDateTime createdAt
    )
{
}
