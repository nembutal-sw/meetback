package com.meetback.dev.repository;

import com.meetback.dev.domain.MeetingParticipant;
import com.meetback.dev.dto.ParticipantRoomResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

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

    List<ParticipantRoomResponse> findRoomParticipants(
            @Param("meetingId") Long meetingId
    );

    List<ParticipantRoomResponse> findKickedRoomParticipants(
            @Param("meetingId") Long meetingId
    );

    int kickParticipant(
            @Param("participantId") Long participantId
    );

    int cancelKick(
            @Param("participantId") Long participantId
    );

    int leaveBeforeVoting(
            @Param("participantId") Long participantId
    );
}