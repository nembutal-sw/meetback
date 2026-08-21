package com.meetback.dev.repository;

import com.meetback.dev.domain.MeetingCandidate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MeetingCandidateMapper {

    int insert(MeetingCandidate candidate);

    int update(MeetingCandidate candidate);

    List<MeetingCandidate> findByMeetingId(Long meetingId);

    MeetingCandidate findById(Long candidateId);

    MeetingCandidate findByMeetingIdAndParticipantId(
            @Param("meetingId") Long meetingId,
            @Param("participantId") Long participantId
    );
}