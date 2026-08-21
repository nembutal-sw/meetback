package com.meetback.dev.service;

import com.meetback.dev.repository.ParticipantMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class ParticipantService {

    private final ParticipantMapper participantMapper;

    public boolean isAllSubmitted(Long meetingId)
    {
        int totalCount = participantMapper.countParticipant(meetingId);

        int submittedCount = participantMapper.countSubmittedParticipant(meetingId);

        return totalCount > 0
                && totalCount == submittedCount;
    }

}
