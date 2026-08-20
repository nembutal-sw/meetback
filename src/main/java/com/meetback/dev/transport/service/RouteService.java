package com.meetback.dev.transport.service;


import com.meetback.dev.domain.MeetingCandidate;
import com.meetback.dev.domain.MeetingParticipant;
import com.meetback.dev.place.dto.PlaceDTO;
import com.meetback.dev.repository.MeetingCandidateMapper;
import com.meetback.dev.repository.MeetingParticipantMapper;
import com.meetback.dev.transport.client.OdsayTransitClient;
import com.meetback.dev.transport.dto.TransitRouteDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RouteService {

    private final MeetingParticipantMapper meetingParticipantMapper;
    private final MeetingCandidateMapper meetingCandidateMapper;
    private final OdsayTransitClient odsayTransitClient;

    public TransitRouteDTO testRoute(
            Long participantId,
            Long candidateId) {

        MeetingParticipant participant =
                meetingParticipantMapper.findById(participantId);

        MeetingCandidate candidate =
                meetingCandidateMapper.findById(candidateId);


        if (participant == null) {
            throw new IllegalArgumentException(
                    "참가자를 찾을 수 없습니다."
            );
        }

        if (candidate == null) {
            throw new IllegalArgumentException(
                    "후보 장소를 찾을 수 없습니다."
            );
        }


        /*
         * DB에 저장되어 있는 참가자 출발 위치
         */
        PlaceDTO departurePlace =
                new PlaceDTO(
                        null,
                        participant.getDepartureName(),
                        participant.getDepartureAddress(),
                        participant.getDepartureAddress(),
                        participant.getDepartureLongitude(),
                        participant.getDepartureLatitude(),
                        null,
                        null
                );


        /*
         * DB에 저장되어 있는 후보 장소
         */
        PlaceDTO meetingPlace =
                new PlaceDTO(
                        null,
                        candidate.getPlaceName(),
                        candidate.getAddress(),
                        candidate.getAddress(),
                        candidate.getLongitude(),
                        candidate.getLatitude(),
                        null,
                        null
                );


        return odsayTransitClient.findSubwayRoute(
                departurePlace,
                meetingPlace
        );
    }
    public TransitRouteDTO searchReturnRoute(
            Long participantId,
            Long candidateId
    ) {

        MeetingParticipant participant =
                meetingParticipantMapper.findById(participantId);

        if (participant == null) {
            throw new IllegalArgumentException("참가자를 찾을 수 없습니다.");
        }

        MeetingCandidate candidate =
                meetingCandidateMapper.findById(candidateId);

        if (candidate == null) {
            throw new IllegalArgumentException("후보 장소를 찾을 수 없습니다.");
        }

        PlaceDTO start = new PlaceDTO(
                null,
                candidate.getPlaceName(),
                candidate.getAddress(),
                candidate.getAddress(),
                candidate.getLongitude(),
                candidate.getLatitude(),
                null,
                null
        );

        PlaceDTO end = new PlaceDTO(
                null,
                participant.getReturnName(),
                participant.getReturnAddress(),
                participant.getReturnAddress(),
                participant.getReturnLongitude(),
                participant.getReturnLatitude(),
                null,
                null
        );

        return odsayTransitClient.findSubwayRoute(
                start,
                end
        );
    }
}