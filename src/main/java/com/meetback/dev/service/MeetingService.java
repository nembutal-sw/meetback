package com.meetback.dev.service;

import com.meetback.dev.domain.*;
import com.meetback.dev.dto.*;
import com.meetback.dev.repository.CandidateMapper;
import com.meetback.dev.repository.MeetingMapper;
import com.meetback.dev.repository.ParticipantMapper;
import com.meetback.dev.repository.VoteMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MeetingService {

    private final MeetingMapper meetingMapper;
    private final ParticipantMapper participantMapper;
    private final CandidateMapper candidateMapper;
    private final ParticipantService participantService;
    private final CandidateService candidateService;
    private final VoteMapper voteMapper;

    @Transactional
    public MeetingCreateResponse createMeeting(
            Long hostUserId,
            MeetingCreateRequest request
    ){
        LocalDateTime desiredEndAt =
                request.getDesiredEndAt();

        if (desiredEndAt == null)
        {
            throw new IllegalArgumentException(
                    "희망 종료시간은 필수입니다."
            );
        }

        if (!desiredEndAt.isAfter(LocalDateTime.now()))
        {
            throw new IllegalArgumentException(
                    "희망 종료시간은 현재 시각 이후로 선택해주세요."
            );
        }

        String inviteCode = generateInviteCode();

        MeetingType meetingType =
                request.getMeetingType() == null
                ? MeetingType.FRIEND
                : request.getMeetingType();

        Meeting meeting = new Meeting();

        meeting.setHostUserId(hostUserId);
        meeting.setTitle(request.getTitle());
        meeting.setMeetingType(meetingType);
        meeting.setStatus(MeetingStatus.INPUT_OPEN);
        meeting.setDesiredEndAt(request.getDesiredEndAt());
        meeting.setCalculationVersion(0);
        meeting.setInviteCode(inviteCode);

        meetingMapper.insertMeeting(meeting);

        MeetingParticipant participant = new MeetingParticipant();

        participant.setMeetingId(meeting.getMeetingId());
        participant.setUserId(hostUserId);
        participant.setParticipantStatus(ParticipantStatus.ACTIVE);
        participant.setInputStatus(InputStatus.DRAFT);

        participantMapper.insertParticipant(participant);

        return new MeetingCreateResponse(
                meeting.getMeetingId(),
                inviteCode
        );
    }
    private String generateInviteCode(){
        return UUID.randomUUID()
                .toString()
                .replace("-","")
                .substring(0,8)
                .toUpperCase();
    }

    @Transactional
    public MeetingJoinResponse joinMeeting(
            Long userId,
            MeetingJoinRequest request
    )
    {
        // 1. 초대코드로 모임 찾기
        Meeting meeting = meetingMapper.selectByInviteCode(request.getInviteCode());

        // 2. 존재하지 않는 초대코드
        if(meeting == null)
        {
            throw new IllegalArgumentException(
                    "유효하지 않은 초대코드입니다."
            );
        }

        // 3. 기존 참가 이력 조회
        MeetingParticipant existingParticipant =
                participantMapper.findAnyByMeetingAndUser(
                        meeting.getMeetingId(),
                        userId
                );

        // 4. 강퇴된 참가자 재입장 차단
        /*
         * 강퇴된 참가자는 재입장 불가
         */
        if(
                existingParticipant != null
                &&
                existingParticipant.getParticipantStatus()
                    == ParticipantStatus.KICKED
        )
        {
            throw new IllegalStateException(
                    "강퇴된 모임에는 다시 참가할 수 없습니다."
            );
        }

        // 5. 이미 정상 참가 중인 사용자
        /*
         * 이미 정상 참가 중
         */
        if(existingParticipant != null
        && existingParticipant.getParticipantStatus() == ParticipantStatus.ACTIVE)
        {
            return new MeetingJoinResponse(
                    meeting.getMeetingId(),
                    false
            );
        }

        /*
         * 신규 참가와 LEFT 재입장은
         * 현재 INPUT_OPEN 단계에서만 허용
         */
        if(
                meeting.getStatus() != MeetingStatus.INPUT_OPEN
        )
        {
            throw new IllegalStateException(
                    "현재 새로운 참가자를 받고 있지 않습니다."
            );
        }

        /*
         * QUICK_VOTE에서 정상적으로 나갔던 사용자 재입장
         */
        if(
                existingParticipant != null
                &&
                existingParticipant.getParticipantStatus()
                == ParticipantStatus.LEFT
        )
        {
            if(
                    meeting.getMeetingType() != MeetingType.QUICK_VOTE
            )
            {
                throw new IllegalStateException(
                        "친구방의 참가 상태를 확인해주세요"
                );
            }

            int updatedRows =
                    participantMapper
                            .reactivateLeftParticipant(
                                    existingParticipant
                                            .getParticipantId()
                            );

            if(updatedRows != 1)
            {
                throw new IllegalStateException(
                  "모임 재입장 처리에 실패했습니다."
                );
            }

            return new MeetingJoinResponse(
                    meeting.getMeetingId(),
                    true
            );
        }

        // 6. 참가 이력이 없는 사용자 등록
        MeetingParticipant participant = new MeetingParticipant();

        participant.setMeetingId(meeting.getMeetingId());
        participant.setUserId(userId);
        participant.setParticipantStatus(ParticipantStatus.ACTIVE);
        participant.setInputStatus(InputStatus.DRAFT);
        participantMapper.insertParticipant(participant);

        // 7. 신규 참가 결과 반환
        return new MeetingJoinResponse(
                meeting.getMeetingId(),
                true
        );
    }

    @Transactional
    public void confirmFinalCandidate(
            Long meetingId,
            Long hostUserId,
            FinalCandidateRequest request
    ) {

        if (request.getCandidateId() == null) {
            throw new IllegalArgumentException(
                    "candidateId는 필수입니다."
            );
        }


        // 모임 조회
        Meeting meeting =
                meetingMapper.findById(meetingId);

        if (meeting == null) {
            throw new IllegalArgumentException(
                    "존재하지 않는 모임입니다."
            );
        }


        // 방장 확인
        if (!meeting.getHostUserId().equals(hostUserId)) {
            throw new IllegalStateException(
                    "방장만 최종 장소를 확정할 수 있습니다."
            );
        }


        // VOTING 상태인지
        if (meeting.getStatus() != MeetingStatus.VOTING) {
            throw new IllegalStateException(
                    "현재 최종 후보를 확정할 수 없는 상태입니다."
            );
        }

        // QUICK_VOTE는 전체 참가자의 과반수가 투표해야 확정 가능
        if (meeting.getMeetingType() == MeetingType.QUICK_VOTE) {

            int totalParticipants =
                    participantMapper.countParticipant(
                            meetingId
                    );

            int totalVotes =
                    voteMapper.countVotesByMeetingId(
                            meetingId
                    );

            int requiredVotes =
                    (totalParticipants / 2) + 1;

            if (
                    totalParticipants == 0
                            ||
                            totalVotes < requiredVotes
            ) {

                throw new IllegalStateException(
                        "과반수의 투표가 완료되어야 장소를 확정할 수 있습니다."
                );
            }
        }


        // 후보 확인
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

        // QUICK_VOTE는 단독 최다 득표 후보만 확정 가능
        if (meeting.getMeetingType() == MeetingType.QUICK_VOTE) {

            List<CandidateVoteResult> voteResults =
                    voteMapper.selectVoteResults(
                            meetingId
                    );

            if (
                    voteResults == null
                            ||
                            voteResults.isEmpty()
            ) {

                throw new IllegalStateException(
                        "투표 결과를 확인할 수 없습니다."
                );
            }


            int maxVoteCount =
                    voteResults.stream()
                            .mapToInt(
                                    CandidateVoteResult::getVoteCount
                            )
                            .max()
                            .orElse(0);


            // 후보에게 들어간 표가 하나도 없는 경우
            if (maxVoteCount == 0) {

                throw new IllegalStateException(
                        "득표한 후보가 없어 장소를 확정할 수 없습니다."
                );
            }


            List<CandidateVoteResult> topCandidates =
                    voteResults.stream()
                            .filter(
                                    result ->
                                            result.getVoteCount()
                                                    == maxVoteCount
                            )
                            .toList();


            // 최다 득표 후보가 여러 명이면 동점
            if (topCandidates.size() != 1) {

                throw new IllegalStateException(
                        "최다 득표 후보가 동점이라 장소를 확정할 수 없습니다."
                );
            }


            Long topCandidateId =
                    topCandidates.get(0)
                            .getCandidateId();


            // 요청으로 들어온 후보가 실제 1등인지 확인
            if (
                    !topCandidateId.equals(
                            request.getCandidateId()
                    )
            ) {

                throw new IllegalStateException(
                        "최다 득표한 장소만 확정할 수 있습니다."
                );
            }
        }


        // 최종 장소 확정
        int result =
                meetingMapper.updateFinalCandidate(
                        meetingId,
                        request.getCandidateId(),
                        MeetingStatus.CONFIRMED
                );


        if (result == 0) {
            throw new IllegalStateException(
                    "최종 장소 확정에 실패했습니다."
            );
        }
    }

    @Transactional
    public void startVoting(
            Long meetingId,
            Long hostUserId
    ) {

        // 1. 모임 존재 확인
        Meeting meeting =
                meetingMapper.findById(meetingId);

        if (meeting == null) {
            throw new IllegalArgumentException(
                    "존재하지 않는 모임입니다."
            );
        }

        // 2. 방장 확인
        if (!meeting.getHostUserId().equals(hostUserId)) {
            throw new IllegalStateException(
                    "방장만 투표를 시작할 수 있습니다."
            );
        }

        // 3. 현재 상태 확인
        if (meeting.getStatus() != MeetingStatus.INPUT_OPEN) {
            throw new IllegalStateException(
                    "투표를 시작할 수 없는 모임 상태입니다."
            );
        }

        // 4. 전원 위치 제출 확인
        boolean allSubmitted =
                participantService.isAllSubmitted(meetingId);

        if (!allSubmitted) {
            throw new IllegalStateException(
                    "모든 참가자의 위치 입력이 완료되지 않았습니다."
            );
        }

        // 5. 후보 존재 확인
        boolean candidateExists =
                candidateService.existsCandidate(meetingId);

        if (!candidateExists) {
            throw new IllegalStateException(
                    "등록된 후보지가 없습니다."
            );
        }

        // 6. VOTING으로 변경
        meetingMapper.updateMeetingStatus(
                meetingId,
                MeetingStatus.VOTING
        );
    }

    public MeetingRoomResponse getMeetingRoom(
            Long meetingId,
            Long userId
    )
    {
        Meeting meeting = meetingMapper.findById(
                meetingId
        );

        if(meeting == null)
        {
            throw new IllegalArgumentException(
                    "존재하지 않는 모임입니다."
            );
        }

        int participantCount = participantMapper.countParticipantByMeetingAndUser(
                meetingId,
                userId
        );

        if(participantCount == 0)
        {
            throw new IllegalArgumentException(
                    "해당 모임의 참가자가 아닙니다."
            );
        }

        return new MeetingRoomResponse(
                meeting.getMeetingId(),
                meeting.getHostUserId(),
                meeting.getTitle(),
                meeting.getInviteCode(),
                meeting.getDesiredEndAt(),
                meeting.getMeetingType(),
                meeting.getStatus(),
                meeting.getFinalCandidateId()
        );
    }

    public Meeting getMeeting(
            Long meetingId,
            Long userId
    ) {

        Meeting meeting =
                meetingMapper.findById(
                        meetingId
                );

        if (meeting == null) {
            throw new IllegalArgumentException(
                    "존재하지 않는 모임입니다."
            );
        }


        int participantCount =
                participantMapper
                        .countParticipantByMeetingAndUser(
                                meetingId,
                                userId
                        );


        if (participantCount == 0) {
            throw new IllegalStateException(
                    "해당 모임의 참가자가 아닙니다."
            );
        }


        return meeting;
    }

    public List<MyMeetingResponse> getMyMeetings(
            Long userId
    ) {
        return meetingMapper.selectMyMeetings(
                userId
        );
    }

    @Transactional
    public int deleteExpiredMeetings() {
        return meetingMapper.deleteExpiredMeetings();
    }

}
