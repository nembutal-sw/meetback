package com.meetback.dev.domain;

public record ParticipantKickResult(
            Long meetingId,
            Long participantId,
            Long userId,
            String nickname
) {
}
