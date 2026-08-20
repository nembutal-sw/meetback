package com.meetback.dev.repository;

import com.meetback.dev.domain.MeetingParticipant;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ParticipantMapper {
    int insertParticipant(MeetingParticipant meetingParticipant);

    int countParticipantByMeetingAndUser(
            @Param("meetingId") Long meetingId,
            @Param("userId") Long userId
    );

    int updateLocation(MeetingParticipant participant);

    // 전체 투표 했는지 확인
    int countParticipant(Long meetingId);
    int countSubmittedParticipant(Long meetingId);
}
