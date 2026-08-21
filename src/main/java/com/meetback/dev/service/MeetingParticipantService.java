package com.meetback.dev.service;

import com.meetback.dev.domain.MeetingParticipant;
import com.meetback.dev.dto.ParticipantLocationRequestDTO;
import com.meetback.dev.place.client.KakaoLocalClient;
import com.meetback.dev.place.dto.PlaceDTO;
import com.meetback.dev.repository.MeetingParticipantMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MeetingParticipantService {

    private final MeetingParticipantMapper meetingParticipantMapper;
    private final KakaoLocalClient kakaoLocalClient;


    public MeetingParticipant findById(
            Long participantId
    ) {

        return meetingParticipantMapper.findById(
                participantId
        );
    }


    public void updateLocation(
            Long participantId,
            ParticipantLocationRequestDTO request
    ) {

        List<PlaceDTO> departureResults =
                kakaoLocalClient.search(
                        request.departureQuery()
                );

        List<PlaceDTO> returnResults =
                kakaoLocalClient.search(
                        request.returnQuery()
                );


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
                meetingParticipantMapper.findById(
                        participantId
                );


        if (participant == null) {
            throw new IllegalArgumentException(
                    "참가자를 찾을 수 없습니다."
            );
        }


        // 출발 장소
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
                BigDecimal.valueOf(
                        departurePlace.latitude()
                )
        );

        participant.setDepartureLongitude(
                BigDecimal.valueOf(
                        departurePlace.longitude()
                )
        );


        // 귀가 장소
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
                BigDecimal.valueOf(
                        returnPlace.latitude()
                )
        );

        participant.setReturnLongitude(
                BigDecimal.valueOf(
                        returnPlace.longitude()
                )
        );


        // 출발지 + 귀가지 저장
        meetingParticipantMapper.updateLocation(
                participant
        );


        /*
         * 출발지 + 귀가지 등록 완료
         *
         * DRAFT -> SUBMITTED
         *
         * 희망 장소는 선택사항이므로
         * 희망 장소 등록 여부와 상관없이 SUBMITTED 처리
         */
        meetingParticipantMapper.submitInput(
                participantId
        );
    }


    /*
     * 모임 참가자 전원이
     * 출발지 + 귀가지 등록을 완료했는지 확인
     */
    public boolean isAllSubmitted(
            Long meetingId
    ) {

        int totalCount =
                meetingParticipantMapper
                        .countByMeetingId(
                                meetingId
                        );

        int submittedCount =
                meetingParticipantMapper
                        .countSubmittedByMeetingId(
                                meetingId
                        );

        return totalCount > 0
                && totalCount == submittedCount;
    }


    public List<MeetingParticipant> findByMeetingId(
            Long meetingId
    ) {

        return meetingParticipantMapper
                .findByMeetingId(
                        meetingId
                );
    }


    public void startEdit(
            Long participantId
    ) {

        MeetingParticipant participant =
                meetingParticipantMapper.findById(
                        participantId
                );

        if (participant == null) {
            throw new IllegalArgumentException(
                    "참가자를 찾을 수 없습니다."
            );
        }

        meetingParticipantMapper.resetInputToDraft(
                participantId
        );
    }
}