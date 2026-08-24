package com.meetback.dev.repository;

import com.meetback.dev.domain.CandidateEvaluation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CandidateEvaluationMapper {

    int insert(CandidateEvaluation evaluation);

    int update(CandidateEvaluation evaluation);

    CandidateEvaluation findByCandidateId(
            Long candidateId
    );

    int updateRecommendationRank(
            @Param("candidateId") Long candidateId,
            @Param("recommendationRank") Integer recommendationRank
    );

    CandidateEvaluation findTopRankedByMeetingId(
            Long meetingId
    );

    List<CandidateEvaluation> findRankingByMeetingId(
            Long meetingId
    );
}