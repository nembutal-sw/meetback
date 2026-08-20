package com.meetback.dev.repository;

import com.meetback.dev.domain.MeetingParticipant;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface MeetingParticipantMapper {
    MeetingParticipant findById(Long participantId);

    int updateLocation(MeetingParticipant participant);

    int completeInput(Long participantId);

    int countByMeetingId(Long meetingId);

    int countCompleteByMeetingId(Long meetingId);

    List<MeetingParticipant> findByMeetingId(Long meetingId);
}
