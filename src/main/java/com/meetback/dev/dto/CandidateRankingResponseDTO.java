package com.meetback.dev.dto;

import java.time.LocalDateTime;
import java.util.List;

public record CandidateRankingResponseDTO(

        Long candidateId,
        String placeName,
        String address,

        Integer recommendationRank,
        Double ruleScore,

        boolean allReturnable,

        LocalDateTime deadlineAt,
        Integer goldenMarginMinutes,

        Double averageReturnMinutes,

        Integer fairnessGapMinutes,
        Integer fairnessScore,

        boolean fairPlace,

        List<ParticipantReturnSummaryDTO> participantResults

) {
}