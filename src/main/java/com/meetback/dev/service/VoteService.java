package com.meetback.dev.service;

import com.meetback.dev.domain.*;

import com.meetback.dev.dto.CandidateVoteResult;
import com.meetback.dev.dto.VoteProgressResponse;
import com.meetback.dev.dto.VoteRequest;
import com.meetback.dev.dto.VoteVoterResponse;

import com.meetback.dev.repository.CandidateMapper;
import com.meetback.dev.repository.MeetingMapper;
import com.meetback.dev.repository.ParticipantMapper;
import com.meetback.dev.repository.VoteMapper;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;


@Service
@RequiredArgsConstructor
public class VoteService {


    private final VoteMapper voteMapper;

    private final MeetingMapper meetingMapper;

    private final ParticipantMapper participantMapper;

    private final CandidateMapper candidateMapper;



    // =============================================================
    // 투표 / 재투표
    // =============================================================

    @Transactional
    public void vote(
            Long meetingId,
            Long userId,
            VoteRequest request
    ) {

        // =========================================================
        // 1. voteType 확인
        // =========================================================

        VoteType voteType =
                request.getVoteType();


        if (voteType == null) {

            throw new IllegalArgumentException(
                    "voteType은 필수입니다."
            );
        }


        Long candidateId =
                request.getCandidateId();



        // =========================================================
        // 2. 모임 확인
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
        // 3. 투표 상태 확인
        // =========================================================

        if (
                meeting.getStatus()
                        != MeetingStatus.VOTING
        ) {

            throw new IllegalStateException(
                    "현재 투표 가능한 상태가 아닙니다."
            );
        }



        // =========================================================
        // 4. JWT userId + meetingId
        //    → participantId 획득
        //
        // 네 기존 helper 그대로 사용
        // =========================================================

        MeetingParticipant participant =
                getParticipantOrThrow(
                        meetingId,
                        userId
                );


        Long participantId =
                participant.getParticipantId();



        // =========================================================
        // 5. 투표 종류별 검증
        // =========================================================

        if (
                voteType
                        == VoteType.CANDIDATE
        ) {

            /*
             * 후보 투표는 candidateId 필수
             */
            if (candidateId == null) {

                throw new IllegalArgumentException(
                        "후보 투표에는 candidateId가 필요합니다."
                );
            }


            /*
             * 실제 이 모임의 활성 후보인지 확인
             */
            MeetingCandidate candidate =
                    candidateMapper.selectActiveCandidate(
                            meetingId,
                            candidateId
                    );


            if (candidate == null) {

                throw new IllegalArgumentException(
                        "유효하지 않은 후보지입니다."
                );
            }
        }

        else if (
                voteType
                        == VoteType.ABSTAIN
        ) {

            /*
             * 기권은 후보가 없어야 함
             */
            if (candidateId != null) {

                throw new IllegalArgumentException(
                        "기권 시 candidateId는 null이어야 합니다."
                );
            }
        }

        else {

            throw new IllegalArgumentException(
                    "지원하지 않는 투표 타입입니다."
            );
        }



        // =========================================================
        // 6. 기존 투표 조회
        // =========================================================

        PlaceVote existingVote =
                voteMapper
                        .selectVoteByMeetingAndParticipant(
                                meetingId,
                                participantId
                        );



        // =========================================================
        // 7. 최초 투표
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


            /*
             * 후보 투표 → 실제 candidateId
             * 기권      → null
             */
            vote.setCandidateId(
                    candidateId
            );


            vote.setVoteType(
                    voteType
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
        // 8. 같은 투표를 또 누른 경우
        //
        // candidateId가 null일 수 있으므로
        // .equals() 직접 호출하면 안 됨.
        // =========================================================

        boolean sameVoteType =
                existingVote.getVoteType()
                        == voteType;


        boolean sameCandidate =
                Objects.equals(
                        existingVote.getCandidateId(),
                        candidateId
                );


        if (
                sameVoteType
                        &&
                        sameCandidate
        ) {

            return;
        }



        // =========================================================
        // 9. 재투표 / 기권 변경
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
                candidateId
        );


        vote.setVoteType(
                voteType
        );


        voteMapper.updateVote(
                vote
        );
    }



    // =============================================================
    // 후보별 득표수 조회
    // =============================================================

    public List<CandidateVoteResult> getVoteResults(

            Long meetingId,

            Long userId

    ) {


        /*
         * 해당 모임 참가자만
         * 투표 현황을 볼 수 있다.
         */
        getParticipantOrThrow(
                meetingId,
                userId
        );


        return voteMapper.selectVoteResults(
                meetingId
        );
    }



    // =============================================================
    // 후보별 투표자 조회
    // =============================================================

    public List<VoteVoterResponse> getVoteVoters(

            Long meetingId,

            Long userId

    ) {


        /*
         * 해당 모임 참가자만
         * 투표자 목록을 볼 수 있다.
         */
        getParticipantOrThrow(
                meetingId,
                userId
        );


        return voteMapper.selectVoteVoters(
                meetingId
        );
    }



    // =============================================================
    // 공통
    //
    // meetingId + JWT userId
    // → MeetingParticipant 조회
    // =============================================================

    private MeetingParticipant getParticipantOrThrow(

            Long meetingId,

            Long userId

    ) {


        MeetingParticipant participant =
                participantMapper
                        .findByMeetingAndUser(
                                meetingId,
                                userId
                        );


        if (
                participant
                        == null
        ) {

            throw new IllegalArgumentException(
                    "해당 모임의 참가자가 아닙니다."
            );
        }


        return participant;
    }

    // =============================================================
// 투표 진행 상태 조회
// =============================================================

    public VoteProgressResponse getVoteProgress(

            Long meetingId,

            Long userId

    ) {

        // =========================================================
        // 1. 해당 모임 참가자인지 검증
        //
        // vote()에서 사용하는 것과 동일한 helper를 재사용한다.
        // =========================================================

        getParticipantOrThrow(
                meetingId,
                userId
        );



        // =========================================================
        // 2. 전체 참가자 수
        // =========================================================

        int totalParticipants =
                participantMapper.countParticipant(
                        meetingId
                );



        // =========================================================
        // 3. 전체 투표 완료 인원
        //
        // place_votes row 수
        //
        // CANDIDATE + ABSTAIN 모두 포함한다.
        // =========================================================

        int totalVotes =
                voteMapper.countVotesByMeetingId(
                        meetingId
                );



        // =========================================================
        // 4. 기권표
        // =========================================================

        int abstainVotes =
                voteMapper.countAbstainVotesByMeetingId(
                        meetingId
                );



        // =========================================================
        // 5. 실제 후보 투표 수
        //
        // vote_type은 DB CHECK로
        // CANDIDATE / ABSTAIN만 허용하고 있으므로:
        //
        // 전체 투표 - 기권 = 후보 투표
        // =========================================================

        int candidateVotes =
                totalVotes
                        - abstainVotes;



        // =========================================================
        // 6. 전원 투표 완료 여부
        // =========================================================

        boolean allVoted =
                totalParticipants > 0
                        &&
                        totalParticipants == totalVotes;



        // =========================================================
        // 7. 결과 반환
        // =========================================================

        return new VoteProgressResponse(

                totalParticipants,

                totalVotes,

                candidateVotes,

                abstainVotes,

                allVoted

        );
    }
}