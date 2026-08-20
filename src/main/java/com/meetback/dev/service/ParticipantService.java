package com.meetback.dev.service;

import com.meetback.dev.domain.InputStatus;
import com.meetback.dev.domain.MeetingParticipant;
import com.meetback.dev.dto.LocationSubmitRequest;
import com.meetback.dev.repository.ParticipantMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ParticipantService {

    private final ParticipantMapper participantMapper;

    @Transactional
    public void submitLocation(
            Long participantId,
            LocationSubmitRequest request
    )
    {
        MeetingParticipant participant = new MeetingParticipant();

        participant.setParticipantId(participantId);

        participant.setDepartureName((request.getDepartureName()));
        participant.setDepartureAddress(request.getDepartureAddress());
        participant.setDepartureLatitude(request.getDepartureLatitude());
        participant.setDepartureLongitude(request.getDepartureLongitude());

        participant.setReturnName(request.getReturnName());
        participant.setReturnAddress(request.getReturnAddress());
        participant.setReturnLatitude(request.getReturnLatitude());
        participant.setReturnLongitude(request.getReturnLongitude());

        participant.setInputStatus(InputStatus.SUBMITTED);

        int result = participantMapper.updateLocation(participant);

        if(result == 0)
        {
            throw new IllegalArgumentException(
                    "존재하지 않는 참가자입니다."
            );
        }
    }

    public boolean isAllSubmitted(Long meetingId)
    {
        int totalCount = participantMapper.countParticipant(meetingId);

        int submittedCount = participantMapper.countSubmittedParticipant(meetingId);

        return totalCount > 0
                && totalCount == submittedCount;
    }

}
