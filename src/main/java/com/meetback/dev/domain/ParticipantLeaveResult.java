package com.meetback.dev.domain;

public record ParticipantLeaveResult(
        Long meetingId,
        Long participantId,
        Long userId,
        String nickname
) {
}
