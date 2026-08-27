package com.meetback.dev.service;

import com.meetback.dev.domain.Meeting;
import com.meetback.dev.domain.MeetingParticipant;
import com.meetback.dev.domain.MeetingStatus;
import com.meetback.dev.dto.ParticipantLocationRequestDTO;
import com.meetback.dev.dto.ParticipantRoomResponse;
import com.meetback.dev.repository.CandidateReturnResultMapper;
import com.meetback.dev.repository.MeetingMapper;
import com.meetback.dev.repository.MeetingParticipantMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class MeetingParticipantService {

    private final MeetingParticipantMapper meetingParticipantMapper;
    private final CandidateReturnResultMapper returnResultMapper;
    private final MeetingMapper meetingMapper;
    private final MeetingPresenceService meetingPresenceService;


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

        MeetingParticipant participant =
                getOwnedParticipant(
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


        if (
                meeting.getStatus()
                        != MeetingStatus.INPUT_OPEN
        ) {

            throw new IllegalStateException(
                    "투표가 시작된 이후에는 장소를 수정할 수 없습니다."
            );
        }


        // =====================================================
        // 3. 요청값 확인
        // =====================================================

        if (request == null) {

            throw new IllegalArgumentException(
                    "장소 정보가 없습니다."
            );
        }


        if (
                request.departureName() == null
                        ||
                        request.departureName().isBlank()
        ) {

            throw new IllegalArgumentException(
                    "출발 장소를 선택해주세요."
            );
        }


        if (
                request.returnName() == null
                        ||
                        request.returnName().isBlank()
        ) {

            throw new IllegalArgumentException(
                    "귀가 장소를 선택해주세요."
            );
        }


        if (
                request.departureLatitude() == null
                        ||
                        request.departureLongitude() == null
        ) {

            throw new IllegalArgumentException(
                    "출발 장소 좌표가 올바르지 않습니다."
            );
        }


        if (
                request.returnLatitude() == null
                        ||
                        request.returnLongitude() == null
        ) {

            throw new IllegalArgumentException(
                    "귀가 장소 좌표가 올바르지 않습니다."
            );
        }


        // =====================================================
        // 4. 사용자가 카카오 검색에서 선택한 장소 그대로 사용
        //
        // 서버에서 장소명으로 Kakao API를 다시 검색하지 않음
        // =====================================================

        String newDepartureName =
                request.departureName().trim();

        String newDepartureAddress =
                request.departureAddress();

        BigDecimal newDepartureLatitude =
                BigDecimal.valueOf(
                        request.departureLatitude()
                );

        BigDecimal newDepartureLongitude =
                BigDecimal.valueOf(
                        request.departureLongitude()
                );


        String newReturnName =
                request.returnName().trim();

        String newReturnAddress =
                request.returnAddress();

        BigDecimal newReturnLatitude =
                BigDecimal.valueOf(
                        request.returnLatitude()
                );

        BigDecimal newReturnLongitude =
                BigDecimal.valueOf(
                        request.returnLongitude()
                );


        // =====================================================
        // 5. 귀가지가 실제로 변경됐는지 확인
        //
        // 귀가지가 변경된 경우에만 기존 계산 결과 삭제
        // =====================================================

        boolean returnChanged =

                !Objects.equals(
                        participant.getReturnName(),
                        newReturnName
                )

                        ||

                        !Objects.equals(
                                participant.getReturnAddress(),
                                newReturnAddress
                        )

                        ||

                        !sameBigDecimal(
                                participant.getReturnLatitude(),
                                newReturnLatitude
                        )

                        ||

                        !sameBigDecimal(
                                participant.getReturnLongitude(),
                                newReturnLongitude
                        );


        // =====================================================
        // 6. 출발 장소 저장값 설정
        // =====================================================

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


        // =====================================================
        // 7. 귀가 장소 저장값 설정
        // =====================================================

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


        // =====================================================
        // 8. DB 저장
        // =====================================================

        meetingParticipantMapper.updateLocation(
                participant
        );


        // =====================================================
        // 9. 귀가지 변경 시 기존 계산 결과 삭제
        //
        // 출발지는 귀가 계산에 사용하지 않으므로
        // 출발지만 바뀐 경우 기존 계산 결과 재사용
        // =====================================================

        if (returnChanged) {

            returnResultMapper.deleteByParticipantId(
                    participantId
            );
        }


        // =====================================================
        // 10. 입력 완료 처리
        // =====================================================

        meetingParticipantMapper.submitInput(
                participantId
        );
    }


    private boolean sameBigDecimal(
            BigDecimal first,
            BigDecimal second
    ) {

        if (
                first == null
                        &&
                        second == null
        ) {

            return true;
        }


        if (
                first == null
                        ||
                        second == null
        ) {

            return false;
        }


        return first
                .setScale(
                        7,
                        RoundingMode.HALF_UP
                )
                .compareTo(
                        second.setScale(
                                7,
                                RoundingMode.HALF_UP
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
                &&
                totalCount == submittedCount;
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
    ) {

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
    ) {

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


        if (
                !Objects.equals(
                        participant.getUserId(),
                        userId
                )
        ) {

            throw new AccessDeniedException(
                    "본인의 참가자 정보만 사용할 수 있습니다."
            );
        }


        return participant;
    }


    public MeetingParticipant findOwnedById(
            Long participantId,
            Long userId
    ) {

        return getOwnedParticipant(
                participantId,
                userId
        );
    }


    public List<ParticipantRoomResponse> findRoomParticipants(
            Long meetingId
    ) {

        List<ParticipantRoomResponse> participants =
                meetingParticipantMapper.findRoomParticipants(
                        meetingId
                );


        participants.forEach(
                participant -> {

                    boolean online =
                            meetingPresenceService.isOnline(
                                    meetingId,
                                    participant.getUserId()
                            );


                    participant.setOnline(
                            online
                    );
                }
        );


        return participants;
    }
}