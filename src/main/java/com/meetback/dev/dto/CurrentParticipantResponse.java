package com.meetback.dev.dto;

public record CurrentParticipantResponse
    (
    Long participantId,
    Long userId,
    String nickname,
    String inputStatus
    )
{
}
