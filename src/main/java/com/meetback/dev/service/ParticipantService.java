package com.meetback.dev.service;

import com.meetback.dev.dto.CurrentParticipantResponse;
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

    public CurrentParticipantResponse getCurrentParticipant(
            Long meetingId,
            Long userId
    ) {
        CurrentParticipantResponse participant =
                participantMapper.findCurrentParticipant(
                        meetingId,
                        userId
                );
        if (participant == null) {

            throw new IllegalArgumentException(
                    "해당 모임의 참가자가 아닙니다."
            );
        }
        return participant;
    }

}
