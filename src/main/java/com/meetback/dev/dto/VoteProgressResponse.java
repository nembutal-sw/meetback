package com.meetback.dev.dto;

public record VoteProgressResponse(
        int totalParticipants,
        int totalVotes,
        int candidateVotes,
        int abstainVotes,
        boolean allVoted
) {
}
