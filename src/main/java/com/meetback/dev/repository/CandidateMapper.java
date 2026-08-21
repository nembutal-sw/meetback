package com.meetback.dev.repository;

import com.meetback.dev.domain.MeetingCandidate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CandidateMapper {

    int countActiveCandidate(
            Long meetingId
    );

    MeetingCandidate selectActiveCandidate(
            @Param("meetingId") Long meetingId,
            @Param("candidateId") Long candidateId
    );


}
