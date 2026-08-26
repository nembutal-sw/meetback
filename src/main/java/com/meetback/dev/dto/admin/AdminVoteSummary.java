package com.meetback.dev.dto.admin;

import lombok.Data;

import java.util.List;

/** 모임 참여자와 후보별 투표 요약. */
@Data
public class AdminVoteSummary {
    private long totalParticipants;
    private long votedParticipants;
    private long notVotedParticipants;
    private long abstainCount;
    private List<AdminCandidateVote> candidates;
}
