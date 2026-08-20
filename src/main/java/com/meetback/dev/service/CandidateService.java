package com.meetback.dev.service;

import com.meetback.dev.domain.MeetingCandidate;
import com.meetback.dev.dto.CandidateCreateRequest;
import com.meetback.dev.repository.CandidateMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CandidateService {

    private final CandidateMapper candidateMapper;

    @Transactional
    public Long createCandidate(
            Long meetingId,
            Long participantId,
            CandidateCreateRequest request
    )
    {
        int count = candidateMapper.countCandidateByMeetingAndParticipant(
                meetingId,
                participantId
        );

        if(count > 0)
        {
            throw new IllegalStateException(
                    "이미 후보자를 등록한 참가자입니다."
            );
        }

        MeetingCandidate candidate = new MeetingCandidate();

        candidate.setMeetingId(meetingId);
        candidate.setProposerParticipantId(participantId);

        candidate.setPlaceName(request.getPlaceName());
        candidate.setAddress(request.getAddress());
        candidate.setLatitude(request.getLatitude());
        candidate.setLongitude(request.getLongitude());

        candidate.setIsActive(true);

        candidateMapper.insertCandidate(candidate);

        return candidate.getCandidateId();
    }

}
