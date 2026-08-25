package com.meetback.dev.service;

import com.meetback.dev.domain.Meeting;
import com.meetback.dev.domain.MeetingParticipant;
import com.meetback.dev.domain.MeetingStatus;
import com.meetback.dev.dto.ParticipantLocationRequestDTO;
import com.meetback.dev.place.client.KakaoLocalClient;
import com.meetback.dev.place.dto.PlaceDTO;
import com.meetback.dev.repository.CandidateReturnResultMapper;
import com.meetback.dev.repository.MeetingMapper;
import com.meetback.dev.repository.MeetingParticipantMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class MeetingParticipantService {

    private final MeetingParticipantMapper meetingParticipantMapper;
    private final CandidateReturnResultMapper returnResultMapper;
    private final KakaoLocalClient kakaoLocalClient;
    private final MeetingMapper meetingMapper;


    public MeetingParticipant findById(
            Long participantId
    ) {

        return meetingParticipantMapper.findById(
                participantId
        );
    }


    @Transactional
    public void updateLocation(
            Long participantId,
            Long userId,
            ParticipantLocationRequestDTO request
    ) {
        // =====================================================
        // 1. 참가자 확인
        // =====================================================

        MeetingParticipant participant = getOwnedParticipant(
                participantId,
                userId
        );

        // =====================================================
        // 2. 모임 상태 확인
        // =====================================================

        Meeting meeting =
                meetingMapper.findById(
                        participant.getMeetingId()
                );


        if (meeting == null) {

            throw new IllegalArgumentException(
                    "모임을 찾을 수 없습니다."
            );
        }


        // 투표 시작 후에는 장소 수정 금지
        if (
                meeting.getStatus()
                        != MeetingStatus.INPUT_OPEN
        ) {

            throw new IllegalStateException(
                    "투표가 시작된 이후에는 장소를 수정할 수 없습니다."
            );
        }


        // =====================================================
        // 3. 여기부터 기존 장소 검색 코드
        // =====================================================



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

        /*
         * 새 출발 장소 데이터
         */
        String newDepartureName =
                departurePlace.name();

        String newDepartureAddress =
                departurePlace.roadAddress() == null
                        || departurePlace.roadAddress().isBlank()
                        ? departurePlace.address()
                        : departurePlace.roadAddress();

        BigDecimal newDepartureLatitude =
                BigDecimal.valueOf(
                        departurePlace.latitude()
                );

        BigDecimal newDepartureLongitude =
                BigDecimal.valueOf(
                        departurePlace.longitude()
                );


        /*
         * 새 귀가 장소 데이터
         */
        String newReturnName =
                returnPlace.name();

        String newReturnAddress =
                returnPlace.roadAddress() == null
                        || returnPlace.roadAddress().isBlank()
                        ? returnPlace.address()
                        : returnPlace.roadAddress();

        BigDecimal newReturnLatitude =
                BigDecimal.valueOf(
                        returnPlace.latitude()
                );

        BigDecimal newReturnLongitude =
                BigDecimal.valueOf(
                        returnPlace.longitude()
                );


        /*
         * 기존 값과 실제로 달라졌는지 확인
         */
        boolean returnChanged =
                !Objects.equals(
                        participant.getReturnName(),
                        newReturnName
                )
                        || !Objects.equals(
                        participant.getReturnAddress(),
                        newReturnAddress
                )
                        || !sameBigDecimal(
                        participant.getReturnLatitude(),
                        newReturnLatitude
                )
                        || !sameBigDecimal(
                        participant.getReturnLongitude(),
                        newReturnLongitude
                );


        // 출발 장소
        participant.setDepartureName(
                newDepartureName
        );

        participant.setDepartureAddress(
                newDepartureAddress
        );

        participant.setDepartureLatitude(
                newDepartureLatitude
        );

        participant.setDepartureLongitude(
                newDepartureLongitude
        );


        // 귀가 장소
        participant.setReturnName(
                newReturnName
        );

        participant.setReturnAddress(
                newReturnAddress
        );

        participant.setReturnLatitude(
                newReturnLatitude
        );

        participant.setReturnLongitude(
                newReturnLongitude
        );


        // 출발지 + 귀가지 저장
        meetingParticipantMapper.updateLocation(
                participant
        );


        /*
         * 귀가지가 실제로 바뀌었다면(출발지 제외)
         * 이 참가자의 기존 귀가 계산 결과만 삭제
         *
         * 다른 참가자 계산 결과는 그대로 재사용
         */
        if (returnChanged) {

            returnResultMapper.deleteByParticipantId(
                    participantId
            );
        }


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


    private boolean sameBigDecimal(
            BigDecimal first,
            BigDecimal second
    ) {

        if (first == null && second == null) {
            return true;
        }

        if (first == null || second == null) {
            return false;
        }

        return first.setScale(
                7,
                java.math.RoundingMode.HALF_UP
        ).compareTo(
                second.setScale(
                        7,
                        java.math.RoundingMode.HALF_UP
                )
        ) == 0;
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


    @Transactional
    public void startEdit(
            Long participantId,
            Long userId
    ) {

        MeetingParticipant participant =
                getOwnedParticipant(
                        participantId,
                        userId
                );


        Meeting meeting =
                meetingMapper.findById(
                        participant.getMeetingId()
                );


        if (meeting == null) {

            throw new IllegalArgumentException(
                    "모임을 찾을 수 없습니다."
            );
        }


        if (
                meeting.getStatus()
                        != MeetingStatus.INPUT_OPEN
        ) {

            throw new IllegalStateException(
                    "투표가 시작된 이후에는 장소를 수정할 수 없습니다."
            );
        }


        meetingParticipantMapper.resetInputToDraft(
                participantId
        );
    }

    @Transactional
    public void cancelEdit(
            Long participantId,
            Long userId
    )
    {
        getOwnedParticipant(
                participantId,
                userId
        );


        meetingParticipantMapper.submitInput(
                participantId
        );
    }

    @Transactional
    public void submitInput(
            Long participantId,
            Long userId
    )
    {
        getOwnedParticipant(
                participantId,
                userId
        );


        meetingParticipantMapper.submitInput(
                participantId
        );
    }

    private MeetingParticipant getOwnedParticipant(
            Long participantId,
            Long userId
    )
    {
        MeetingParticipant participant =
                meetingParticipantMapper.findById(
                        participantId
                );


        if (participant == null)
        {
            throw new IllegalArgumentException(
                    "참가자를 찾을 수 없습니다."
            );
        }


        if (
                !Objects.equals(
                        participant.getUserId(),
                        userId
                )
        )
        {
            throw new AccessDeniedException(
                    "본인의 참가자 정보만 사용할 수 있습니다."
            );
        }


        return participant;
    }

    public MeetingParticipant findOwnedById(
            Long participantId,
            Long userId
    )
    {
        return getOwnedParticipant(
                participantId,
                userId
        );
    }

}