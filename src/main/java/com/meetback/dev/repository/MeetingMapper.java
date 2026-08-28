package com.meetback.dev.repository;

import com.meetback.dev.domain.Meeting;
import com.meetback.dev.domain.MeetingStatus;
import com.meetback.dev.dto.MyMeetingResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MeetingMapper {
    int insertMeeting(Meeting meeting);

    Meeting selectByInviteCode(String inviteCode);

    int updateMeetingStatus(
            @Param("meetingId") Long meetingId,
            @Param("status") MeetingStatus status
    );

    Meeting findById(Long meetingId);

    int updateCalculationVersion(
            @Param("meetingId") Long meetingId,
            @Param("calculationVersion") Integer calculationVersion
    );

    int updateFinalCandidate(
            @Param("meetingId") Long meetingId,
            @Param("candidateId") Long candidateId,
            @Param("status") MeetingStatus status
    );

    List<MyMeetingResponse> selectMyMeetings(Long userId);

    int deleteExpiredMeetings();

    List<Meeting> selectQuickVoteMeetings(
            @Param("keyword") String keyword
    );
}
