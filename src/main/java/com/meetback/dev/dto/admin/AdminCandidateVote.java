package com.meetback.dev.dto.admin;

import lombok.Data;

/** 후보별 투표 집계 항목. */
@Data
public class AdminCandidateVote {
    private Long candidateId;
    private String placeName;
    private long voteCount;
}
