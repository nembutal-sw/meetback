package com.meetback.dev.repository;

import com.meetback.dev.domain.PlaceVote;
import com.meetback.dev.dto.CandidateVoteResult;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface VoteMapper {

    PlaceVote selectVoteByMeetingAndParticipant(
            @Param("meetingId") Long meetingId,
            @Param("participantId") Long participantId
    );

    int insertVote(PlaceVote vote);
    int updateVote(PlaceVote vote);

    List<CandidateVoteResult> selectVoteResults(Long meetingId);

}
