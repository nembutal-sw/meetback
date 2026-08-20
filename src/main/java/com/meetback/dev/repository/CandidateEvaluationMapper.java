package com.meetback.dev.repository;

import com.meetback.dev.domain.CandidateEvaluation;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CandidateEvaluationMapper {

    int insert(CandidateEvaluation evaluation);

    int update(CandidateEvaluation evaluation);

    CandidateEvaluation findByCandidateId(Long candidateId);
}