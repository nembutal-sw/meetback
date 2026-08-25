package com.meetback.dev.service;

import com.meetback.dev.domain.Meeting;
import com.meetback.dev.domain.MeetingCandidate;
import com.meetback.dev.domain.MeetingParticipant;
import com.meetback.dev.domain.MeetingStatus;
import com.meetback.dev.domain.PlaceVote;
import com.meetback.dev.dto.CandidateVoteResult;
import com.meetback.dev.dto.VoteRequest;
import com.meetback.dev.repository.CandidateMapper;
import com.meetback.dev.repository.MeetingMapper;
import com.meetback.dev.repository.ParticipantMapper;
import com.meetback.dev.repository.VoteMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VoteService {

    private final VoteMapper voteMapper;
    private final MeetingMapper meetingMapper;
    private final ParticipantMapper participantMapper;
    private final CandidateMapper candidateMapper;


    @Transactional
    public void vote(
            Long meetingId,
            Long userId,
            VoteRequest request
    ) {

        // =========================================================
        // 1. candidateId null 방지
        // =========================================================
        if (request.getCandidateId() == null) {

            throw new IllegalArgumentException(
                    "candidateId는 필수입니다."
            );
        }


        // =========================================================
        // 2. 모임 존재 확인
        // =========================================================
        Meeting meeting =
                meetingMapper.findById(
                        meetingId
                );

        if (meeting == null) {

            throw new IllegalArgumentException(
                    "존재하지 않는 모임입니다."
            );
        }


        // =========================================================
        // 3. 현재 투표 가능한 상태인지 확인
        // =========================================================
        if (meeting.getStatus() != MeetingStatus.VOTING) {

            throw new IllegalStateException(
                    "현재 투표 가능한 상태가 아닙니다."
            );
        }


        // =========================================================
        // 4. JWT userId + meetingId로 현재 참가자 조회
        //
        // 프론트에서 participantId를 받지 않는다.
        //
        // JWT
        // userId
        //      +
        // meetingId
        //      ↓
        // meeting_participants 조회
        //      ↓
        // participantId 획득
        // =========================================================
        MeetingParticipant participant =
                participantMapper.findByMeetingAndUser(
                        meetingId,
                        userId
                );


        if (participant == null) {

            throw new IllegalArgumentException(
                    "해당 모임의 참가자가 아닙니다."
            );
        }


        Long participantId =
                participant.getParticipantId();


        // =========================================================
        // 5. 해당 모임의 활성 후보인지 확인
        // =========================================================
        MeetingCandidate candidate =
                candidateMapper.selectActiveCandidate(
                        meetingId,
                        request.getCandidateId()
                );


        if (candidate == null) {

            throw new IllegalArgumentException(
                    "유효하지 않은 후보지입니다."
            );
        }


        // =========================================================
        // 6. 기존 투표 확인
        // =========================================================
        PlaceVote existingVote =
                voteMapper.selectVoteByMeetingAndParticipant(
                        meetingId,
                        participantId
                );


        // =========================================================
        // 7. 첫 투표
        // =========================================================
        if (existingVote == null) {

            PlaceVote vote =
                    new PlaceVote();


            vote.setMeetingId(
                    meetingId
            );


            vote.setParticipantId(
                    participantId
            );


            vote.setCandidateId(
                    request.getCandidateId()
            );


            vote.setVoteChangeCount(
                    0
            );


            voteMapper.insertVote(
                    vote
            );


            return;
        }


        // =========================================================
        // 8. 같은 후보를 다시 선택한 경우
        //
        // 이미 같은 곳에 투표했으므로 아무것도 하지 않는다.
        // =========================================================
        if (
                existingVote.getCandidateId()
                        .equals(
                                request.getCandidateId()
                        )
        ) {

            return;
        }


        // =========================================================
        // 9. 다른 후보로 재투표
        // =========================================================
        PlaceVote vote =
                new PlaceVote();


        vote.setMeetingId(
                meetingId
        );


        vote.setParticipantId(
                participantId
        );


        vote.setCandidateId(
                request.getCandidateId()
        );


        voteMapper.updateVote(
                vote
        );
    }


    // =============================================================
    // 후보별 득표수 조회
    // =============================================================
    public List<CandidateVoteResult> getVoteResults(
            Long meetingId
    ) {

        return voteMapper.selectVoteResults(
                meetingId
        );
    }
}