package com.meetback.dev.dto;

import lombok.Data;

@Data
public class CandidateVoteResult {

    private Long candidateId;
    private String placeName;
    private int voteCount;

}
