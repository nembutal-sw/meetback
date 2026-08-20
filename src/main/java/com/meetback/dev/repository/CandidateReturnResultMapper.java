package com.meetback.dev.repository;

import com.meetback.dev.domain.CandidateReturnResult;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CandidateReturnResultMapper {

    int insert(CandidateReturnResult result);

    List<CandidateReturnResult> findByCandidateId(
            Long candidateId
    );
}