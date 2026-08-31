package com.meetback.dev.repository;

import com.meetback.dev.domain.MeetingParticipant;
import com.meetback.dev.domain.ParticipantStatus;
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
            @Param("participantId") Long participantId,
            @Param("nextStatus")ParticipantStatus nextStatus
            );

    int leaveBeforeVoting(
            @Param("participantId") Long participantId
    );

    MeetingParticipant findByMeetingIdAndUserId(
            @Param("meetingId") Long meetingId,
            @Param("userId") Long userId
    );

    int updateReturnLocation(
            MeetingParticipant participant
    );

    int countActiveByMeetingId(
            @Param("meetingId") Long meetingId
    );

    int deleteByMeetingId(
            @Param("meetingId") Long meetingId
    );
}