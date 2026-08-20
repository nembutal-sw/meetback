package com.meetback.dev.service;

import com.meetback.dev.domain.InputStatus;
import com.meetback.dev.domain.Meeting;
import com.meetback.dev.domain.MeetingStatus;
import com.meetback.dev.dto.MeetingJoinRequest;
import com.meetback.dev.repository.MeetingMapper;
import com.meetback.dev.domain.MeetingParticipant;
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
        int count = participantMapper.countParticipantByMeetingAndUser(
                meeting.getMeetingId(),
                userId
        );

        if(count>0)
        {
            throw new IllegalStateException(
                    "이미 참여한 모임입니다."
            );
        }

        // 4. 참가자 등록

        MeetingParticipant participant = new MeetingParticipant();

        participant.setMeetingId(meeting.getMeetingId());
        participant.setUserId(userId);
        participant.setInputStatus(InputStatus.DRAFT);

        participantMapper.insertParticipant(participant);

        return meeting.getMeetingId();
    }

}
