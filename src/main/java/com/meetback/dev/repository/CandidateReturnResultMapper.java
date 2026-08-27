package com.meetback.dev.repository;

import com.meetback.dev.domain.CandidateReturnResult;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CandidateReturnResultMapper {

    int insert(CandidateReturnResult result);

    List<CandidateReturnResult> findByCandidateId(
            Long candidateId
    );

    List<CandidateReturnResult> findByCandidateIdAndVersion(
            @Param("candidateId") Long candidateId,
            @Param("calculationVersion") Integer calculationVersion
    );

    void deleteByCandidateId(Long candidateId);

    CandidateReturnResult findByCandidateAndParticipantAndVersion(
            @Param("candidateId") Long candidateId,
            @Param("participantId") Long participantId,
            @Param("calculationVersion") Integer calculationVersion
    );

    void deleteByParticipantId(Long participantId);

    void updateRouteMapData(
            @Param("resultId") Long resultId,
            @Param("routeMapData") String routeMapData
    );
}