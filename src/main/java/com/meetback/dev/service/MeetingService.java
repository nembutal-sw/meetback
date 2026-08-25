package com.meetback.dev.service;

import com.meetback.dev.domain.*;
import com.meetback.dev.dto.FinalCandidateRequest;
import com.meetback.dev.dto.MeetingJoinRequest;
import com.meetback.dev.repository.CandidateMapper;
import com.meetback.dev.repository.MeetingMapper;
import com.meetback.dev.dto.MeetingCreateRequest;
import com.meetback.dev.dto.MeetingCreateResponse;
import com.meetback.dev.repository.ParticipantMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MeetingService {

    private final MeetingMapper meetingMapper;
    private final ParticipantMapper participantMapper;
    private final CandidateMapper candidateMapper;
    private final ParticipantService participantService;
    private final CandidateService candidateService;

    @Transactional
    public MeetingCreateResponse createMeeting(
            Long hostUserId,
            MeetingCreateRequest request
    ){

        String inviteCode = generateInviteCode();

        Meeting meeting = new Meeting();

        meeting.setHostUserId(hostUserId);
        meeting.setTitle(request.getTitle());
        meeting.setStatus(MeetingStatus.INPUT_OPEN);
        meeting.setDesiredEndAt(request.getDesiredEndAt());
        meeting.setCalculationVersion(0);
        meeting.setInviteCode(inviteCode);

        meetingMapper.insertMeeting(meeting);

        MeetingParticipant participant = new MeetingParticipant();

        participant.setMeetingId(meeting.getMeetingId());
        participant.setUserId(hostUserId);
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
    public Long joinMeeting(
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

        // 3. 이미 참가 중인지 확인
        if (
                participantMapper.countParticipantByMeetingAndUser(
                        meeting.getMeetingId(),
                        userId
                ) > 0
        ) {

            return meeting.getMeetingId();
        }

        // 4. 참가자 등록

        MeetingParticipant participant = new MeetingParticipant();

        participant.setMeetingId(meeting.getMeetingId());
        participant.setUserId(userId);
        participant.setInputStatus(InputStatus.DRAFT);

        participantMapper.insertParticipant(participant);

        return meeting.getMeetingId();
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
                meeting.getStatus()
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

}
