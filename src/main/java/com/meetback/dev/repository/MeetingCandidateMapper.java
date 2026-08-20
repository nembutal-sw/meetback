package com.meetback.dev.repository;

import com.meetback.dev.domain.MeetingCandidate;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface MeetingCandidateMapper {

    int insert(MeetingCandidate candidate);

    List<MeetingCandidate> findByMeetingId(Long meetingId);

    MeetingCandidate findById(Long candidateId);
}
