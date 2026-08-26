package com.meetback.dev.dto;

public record VoteVoterResponse(
        Long candidateId,
        Long participantId,
        Long userId,
        String nickname
) {
}