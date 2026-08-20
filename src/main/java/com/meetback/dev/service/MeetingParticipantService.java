package com.meetback.dev.service;

import com.meetback.dev.domain.MeetingParticipant;
import com.meetback.dev.dto.ParticipantLocationRequestDTO;
import com.meetback.dev.place.client.KakaoLocalClient;
import com.meetback.dev.place.dto.PlaceDTO;
import com.meetback.dev.repository.MeetingParticipantMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MeetingParticipantService {

    private final MeetingParticipantMapper meetingParticipantMapper;
    private final KakaoLocalClient kakaoLocalClient;

    public MeetingParticipant findById(Long participantId) {
        return meetingParticipantMapper.findById(participantId);
    }

    public void updateLocation(
            Long participantId,
            ParticipantLocationRequestDTO request
    ) {

        List<PlaceDTO> departureResults =
                kakaoLocalClient.search(request.departureQuery());

        List<PlaceDTO> returnResults =
                kakaoLocalClient.search(request.returnQuery());


        if (departureResults.isEmpty()) {
            throw new IllegalArgumentException(
                    "출발 장소를 찾을 수 없습니다."
            );
        }

        if (returnResults.isEmpty()) {
            throw new IllegalArgumentException(
                    "귀가 장소를 찾을 수 없습니다."
            );
        }


        // 검색 결과 중 첫 번째 장소 사용
        PlaceDTO departurePlace =
                departureResults.get(0);

        PlaceDTO returnPlace =
                returnResults.get(0);


        MeetingParticipant participant =
                meetingParticipantMapper.findById(participantId);


        if (participant == null) {
            throw new IllegalArgumentException(
                    "참가자를 찾을 수 없습니다."
            );
        }


        participant.setDepartureName(
                departurePlace.name()
        );

        participant.setDepartureAddress(
                departurePlace.roadAddress() == null
                        || departurePlace.roadAddress().isBlank()
                        ? departurePlace.address()
                        : departurePlace.roadAddress()
        );

        participant.setDepartureLatitude(
                departurePlace.latitude()
        );

        participant.setDepartureLongitude(
                departurePlace.longitude()
        );


        participant.setReturnName(
                returnPlace.name()
        );

        participant.setReturnAddress(
                returnPlace.roadAddress() == null
                        || returnPlace.roadAddress().isBlank()
                        ? returnPlace.address()
                        : returnPlace.roadAddress()
        );

        participant.setReturnLatitude(
                returnPlace.latitude()
        );

        participant.setReturnLongitude(
                returnPlace.longitude()
        );


        meetingParticipantMapper.updateLocation(
                participant
        );
    }


    public boolean isAllComplete(Long meetingId) {

        int totalCount =
                meetingParticipantMapper.countByMeetingId(meetingId);

        int completeCount =
                meetingParticipantMapper.countCompleteByMeetingId(meetingId);

        return totalCount > 0
                && totalCount == completeCount;
    }


    public List<MeetingParticipant> findByMeetingId(Long meetingId) {
        return meetingParticipantMapper.findByMeetingId(meetingId);
    }
}