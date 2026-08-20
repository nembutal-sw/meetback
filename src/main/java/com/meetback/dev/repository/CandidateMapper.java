package com.meetback.dev.repository;

import com.meetback.dev.domain.MeetingCandidate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CandidateMapper {

    int insertCandidate(MeetingCandidate candidate);

    int countCandidateByMeetingAndParticipant(
            @Param("meetingId") Long meetingId,
            @Param("participantId") Long participantId);


}
