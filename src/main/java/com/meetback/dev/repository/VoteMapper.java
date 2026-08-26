package com.meetback.dev.repository;

import com.meetback.dev.domain.PlaceVote;
import com.meetback.dev.dto.CandidateVoteResult;
import com.meetback.dev.dto.VoteVoterResponse;
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

    List<CandidateVoteResult> selectVoteResults(@Param("meetingId") Long meetingId);

    List<VoteVoterResponse> selectVoteVoters(
            @Param("meetingId") Long meetingId
    );

    int countVotesByMeetingId(
            @Param("meetingId") Long meetingId
    );

    int countAbstainVotesByMeetingId(
            @Param("meetingId") Long meetingId
    );


}
