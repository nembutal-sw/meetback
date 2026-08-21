package com.meetback.dev.repository;

import com.meetback.dev.domain.MeetingParticipant;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface MeetingParticipantMapper {

    MeetingParticipant findById(Long participantId);

    int updateLocation(MeetingParticipant participant);

    int submitInput(Long participantId);

    int countByMeetingId(Long meetingId);

    int countSubmittedByMeetingId(Long meetingId);

    List<MeetingParticipant> findByMeetingId(Long meetingId);

    int resetInputToDraft(Long participantId);
}