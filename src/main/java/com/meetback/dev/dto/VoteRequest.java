package com.meetback.dev.dto;

import com.meetback.dev.domain.VoteType;
import lombok.Data;

@Data
public class VoteRequest {
    private VoteType voteType;
    private Long candidateId;
}
