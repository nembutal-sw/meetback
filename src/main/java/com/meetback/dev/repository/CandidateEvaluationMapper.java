package com.meetback.dev.repository;

import com.meetback.dev.domain.CandidateEvaluation;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CandidateEvaluationMapper {

    int insert(CandidateEvaluation evaluation);

    int update(CandidateEvaluation evaluation);

    CandidateEvaluation findByCandidateId(Long candidateId);

    int updateRecommendationRank(
            Long candidateId,
            Integer recommendationRank
    );

    CandidateEvaluation findTopRankedByMeetingId(
            Long meetingId
    );

    List<CandidateEvaluation> findRankingByMeetingId(
            Long meetingId
    );
}