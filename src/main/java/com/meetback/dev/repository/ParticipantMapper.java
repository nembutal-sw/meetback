package com.meetback.dev.repository;

import com.meetback.dev.domain.MeetingParticipant;
import com.meetback.dev.dto.CurrentParticipantResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ParticipantMapper {
    int insertParticipant(MeetingParticipant meetingParticipant);

    // participant 조회
    MeetingParticipant findByMeetingAndUser(
            @Param("meetingId") Long meetingId,
            @Param("userId") Long userId
    );

    int countParticipantByMeetingAndUser(
            @Param("meetingId") Long meetingId,
            @Param("userId") Long userId
    );

    int countParticipantByIdAndMeeting(
            @Param("participantId") Long participantId,
            @Param("meetingId") Long meetingId
    );

    // 전체 투표 했는지 확인
    int countParticipant(@Param("meetingId") Long meetingId);
    int countSubmittedParticipant(@Param("meetingId") Long meetingId);

    CurrentParticipantResponse findCurrentParticipant(
            @Param("meetingId") Long meetingId,
            @Param("userId") Long userId
    );

    MeetingParticipant findAnyByMeetingAndUser(
            @Param("meetingId") Long meetingId,
            @Param("userId") Long userId
    );
}
