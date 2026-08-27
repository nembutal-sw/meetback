package com.meetback.dev.repository;

import com.meetback.dev.dto.admin.AdminCandidate;
import com.meetback.dev.dto.admin.AdminCandidateVote;
import com.meetback.dev.dto.admin.AdminChatMessage;
import com.meetback.dev.dto.admin.AdminMeetingDetail;
import com.meetback.dev.dto.admin.AdminMeetingListItem;
import com.meetback.dev.dto.admin.AdminParticipant;
import com.meetback.dev.dto.admin.AdminVoteSummary;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 관리자 모임 화면 조회 전용 Mapper. */
@Mapper
public interface AdminMeetingMapper {
    List<AdminMeetingListItem> findMeetings(
            @Param("query") String query,
            @Param("offset") int offset,
            @Param("size") int size
    );

    long countMeetings(@Param("query") String query);

    AdminMeetingDetail findMeetingDetail(@Param("meetingId") Long meetingId);

    List<AdminParticipant> findParticipants(@Param("meetingId") Long meetingId);

    List<AdminCandidate> findCandidates(@Param("meetingId") Long meetingId);

    AdminVoteSummary findVoteSummary(@Param("meetingId") Long meetingId);

    List<AdminCandidateVote> findCandidateVotes(@Param("meetingId") Long meetingId);

    List<AdminChatMessage> findChats(
            @Param("meetingId") Long meetingId,
            @Param("offset") int offset,
            @Param("size") int size
    );

    long countChats(@Param("meetingId") Long meetingId);
}
