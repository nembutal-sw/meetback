package com.meetback.dev.domain;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CandidateEvaluation {

    private Long evaluationId;
    private Long candidateId;
    private Integer calculationVersion;
    private Boolean allReturnable;
    private LocalDateTime deadlineAt;
    private Integer goldenMarginMinutes;
    private Double averageReturnMinutes;
    private Integer fairnessGapMinutes;
    private Integer fairnessScore;
    private Double ruleScore;
    private Integer recommendationRank;
    private LocalDateTime updatedAt;
}