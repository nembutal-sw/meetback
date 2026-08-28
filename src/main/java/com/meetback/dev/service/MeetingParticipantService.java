package com.meetback.dev.service;

import com.meetback.dev.domain.*;
import com.meetback.dev.dto.ParticipantLocationRequestDTO;
import com.meetback.dev.dto.ParticipantRoomResponse;
import com.meetback.dev.repository.*;
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
    private final ParticipantKickHistoryMapper participantKickHistoryMapper;
    private final MeetingCandidateMapper meetingCandidateMapper;

    public MeetingParticipant findById(
            Long participantId,
            Long hostUserId
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


        // =========================================================
        // 1. 참가자 존재 확인
        // =========================================================

        if (participant == null) {

            throw new IllegalArgumentException(
                    "참가자를 찾을 수 없습니다."
            );
        }


        // =========================================================
        // 2. 정상 참가 상태 확인
        //
        // KICKED 참가자는 장소 입력, 수정, 제출 등의
        // 모임 기능을 사용할 수 없다.
        // =========================================================

        if (
                participant.getParticipantStatus()
                        != ParticipantStatus.ACTIVE
        ) {

            throw new AccessDeniedException(
                    "강퇴된 참가자는 모임 기능을 사용할 수 없습니다."
            );
        }


        // =========================================================
        // 3. 본인의 참가자 정보인지 확인
        //
        // 프론트에서 받은 participantId만 신뢰하지 않고
        // JWT userId와 실제 participant.userId를 비교한다.
        // =========================================================

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

    @Transactional
    public ParticipantKickResult kickParticipant(
            Long participantId,
            Long hostUserId
    ) {

        // =========================================================
        // 1. 강퇴 대상 참가자 조회
        // =========================================================

        MeetingParticipant participant =
                meetingParticipantMapper.findById(
                        participantId
                );


        if (participant == null) {

            throw new IllegalArgumentException(
                    "참가자를 찾을 수 없습니다."
            );
        }


        // =========================================================
        // 2. 이미 강퇴된 참가자인지 확인
        // =========================================================

        if (
                participant.getParticipantStatus()
                        != ParticipantStatus.ACTIVE
        ) {

            throw new IllegalStateException(
                    "이미 강퇴된 참가자입니다."
            );
        }


        // =========================================================
        // 3. 대상 참가자가 속한 모임 조회
        // =========================================================

        Meeting meeting =
                meetingMapper.findById(
                        participant.getMeetingId()
                );


        if (meeting == null) {

            throw new IllegalArgumentException(
                    "모임을 찾을 수 없습니다."
            );
        }


        // =========================================================
        // 4. 강퇴 요청자가 실제 방장인지 확인
        //
        // hostUserId는 Controller에서
        // JWT user.userId()를 전달한 값이다.
        // =========================================================

        if (
                !Objects.equals(
                        meeting.getHostUserId(),
                        hostUserId
                )
        ) {

            throw new AccessDeniedException(
                    "방장만 참가자를 강퇴할 수 있습니다."
            );
        }


        // =========================================================
        // 5. 방장 자기 자신 강퇴 방지
        // =========================================================

        if (
                Objects.equals(
                        participant.getUserId(),
                        hostUserId
                )
        ) {

            throw new IllegalArgumentException(
                    "방장은 자신을 강퇴할 수 없습니다."
            );
        }


        // =========================================================
        // 6. 참가자 현재 상태 변경
        //
        // ACTIVE -> KICKED
        // =========================================================

        int updatedRows =
                meetingParticipantMapper.kickParticipant(
                        participantId
                );


        if (updatedRows != 1) {

            throw new IllegalStateException(
                    "참가자 강퇴에 실패했습니다."
            );
        }


        // =========================================================
        // 7. 강퇴 이력 객체 생성
        //
        // 저장하는 정보:
        // meetingId
        // participantId
        // 강퇴당한 userId
        //
        // 강퇴한 방장과 강퇴 시각은 저장하지 않는다.
        // =========================================================

        ParticipantKickHistory history =
                new ParticipantKickHistory();


        history.setMeetingId(
                participant.getMeetingId()
        );


        history.setParticipantId(
                participant.getParticipantId()
        );


        history.setKickedUserId(
                participant.getUserId()
        );

        history.setKickedByUserId(
                hostUserId
        );


        // =========================================================
        // 8. 강퇴 이력 INSERT
        // =========================================================

        int insertedRows =
                participantKickHistoryMapper.insertKickHistory(
                        history
                );


        if (insertedRows != 1) {

            throw new IllegalStateException(
                    "강퇴 이력 저장에 실패했습니다."
            );
        }


        // =========================================================
        // 9. Controller에 WebSocket 방송 정보 반환
        // =========================================================

        return new ParticipantKickResult(
                participant.getMeetingId(),
                participant.getParticipantId(),
                participant.getUserId(),
                participant.getNickname()
        );
    }

    @Transactional
    public ParticipantKickResult cancelKick(
            Long participantId,
            Long hostUserId
    )
    {
        MeetingParticipant participant =
                meetingParticipantMapper.findById(
                        participantId
                );

        if(participant == null)
        {
            throw new IllegalArgumentException(
                    "참가자를 찾을 수 없습니다."
            );
        }

        if(participant.getParticipantStatus() != ParticipantStatus.KICKED)
        {
            throw new IllegalStateException(
                    "강퇴된 참가자가 아닙니다."
            );
        }

        Meeting meeting =
                meetingMapper.findById(
                        participant.getMeetingId()
                );

        if(meeting == null)
        {
            throw new IllegalArgumentException(
                    "모임을 찾을 수 없습니다."
            );
        }

        /*
         * 현재 모임의 방장만 취소할 수 있게 합니다.
         *
         * '강퇴했던 사람만 취소 가능'으로 만들고 싶다면
         * 이력의 kickedByUserId를 별도로 조회해 비교해야 합니다.
         */

        if(
                !Objects.equals(
                        meeting.getHostUserId(),
                        hostUserId
                )
        ){
            throw new AccessDeniedException(
                    "방장만 강퇴를 취소할 수 있습니다."
            );
        }

        int updatedRows =
                meetingParticipantMapper.cancelKick(
                        participantId
                );

        if(updatedRows != 1)
        {
            throw new IllegalStateException(
                    "강퇴 취소에 실패했습니다."
            );
        }

        int historyUpdatedRows =
                participantKickHistoryMapper.cancelLatestKickHistory(
                        participantId,
                        hostUserId
                );

        if(historyUpdatedRows != 1)
        {
            throw new IllegalStateException(
                    "강퇴 취소 이력 저장에 실패했습니다."
            );
        }

        return new ParticipantKickResult(
                participant.getMeetingId(),
                participant.getParticipantId(),
                participant.getUserId(),
                participant.getNickname()
        );
    }

    @Transactional
    public ParticipantLeaveResult leaveQuickVoteMeeting(
            Long participantId,
            Long userId
    )
    {
        /*
         * 1. 참가자 조회
         */
        MeetingParticipant participant =
                meetingParticipantMapper.findById(
                        participantId
                );

        if(participant == null)
        {
            throw new IllegalArgumentException(
                    "참가자를 찾을 수 없습니다."
            );
        }

        /*
         * 2. 본인 참가자 정보인지 검사
         */
        if(
                !Objects.equals(
                        participant.getUserId(),
                        userId
                )
        )
        {
            throw new AccessDeniedException(
                    "본인만 모임에서 나갈 수 있습니다."
            );
        }

        if (
                participant.getParticipantStatus()
                        != ParticipantStatus.ACTIVE
        ) {
            throw new IllegalStateException(
                    "현재 참가 중인 사용자가 아닙니다."
            );
        }


        /*
         * 4. 모임 조회
         */
        Meeting meeting =
                meetingMapper.findById(
                        participant.getMeetingId()
                );


        if (meeting == null) {
            throw new IllegalArgumentException(
                    "모임을 찾을 수 없습니다."
            );
        }


        /*
         * 5. QUICK_VOTE 방에서만 나가기 허용
         *
         * FRIEND는 WebSocket 연결이 끊겨도
         * 참가자 상태를 ACTIVE로 유지한다.
         */
        if (
                meeting.getMeetingType()
                        != MeetingType.QUICK_VOTE
        ) {
            throw new IllegalStateException(
                    "친구방에서는 참가자 상태가 유지됩니다."
            );
        }


        /*
         * 6. 방장은 나가기 불가
         */
        if (
                Objects.equals(
                        meeting.getHostUserId(),
                        userId
                )
        ) {
            throw new IllegalStateException(
                    "방장은 모임에서 나갈 수 없습니다."
            );
        }


        /*
         * 7. 우선 INPUT_OPEN 단계만 지원
         */
        if (
                meeting.getStatus()
                        != MeetingStatus.INPUT_OPEN
        ) {
            throw new IllegalStateException(
                    "투표가 시작된 이후에는 모임에서 나갈 수 없습니다."
            );
        }


        /*
         * 8. 참가자가 등록한 후보 조회
         */
        MeetingCandidate candidate =
                meetingCandidateMapper
                        .findByMeetingIdAndParticipantId(
                                meeting.getMeetingId(),
                                participantId
                        );


        /*
         * 후보가 존재하면 삭제하지 않고 비활성화한다.
         *
         * 재입장 후 다시 장소를 등록하면
         * 기존 후보 행을 재사용할 수 있다.
         */
        if (
                candidate != null
                        &&
                        Boolean.TRUE.equals(
                                candidate.getIsActive()
                        )
        ) {

            candidate.setIsActive(false);

            int candidateUpdatedRows =
                    meetingCandidateMapper.update(
                            candidate
                    );


            if (candidateUpdatedRows != 1) {
                throw new IllegalStateException(
                        "참가자의 후보 장소 비활성화에 실패했습니다."
                );
            }


            /*
             * 나간 참가자가 등록한 후보의 계산 결과 제거
             */
            returnResultMapper.deleteByCandidateId(
                    candidate.getCandidateId()
            );
        }


        /*
         * 다른 후보에 대해 계산된
         * 나간 참가자의 귀가 결과도 제거
         */
        returnResultMapper.deleteByParticipantId(
                participantId
        );


        /*
         * 기존 계산 결과를 현재 결과로 사용하지 않도록
         * 계산 버전을 증가시킨다.
         */
        int currentVersion =
                meeting.getCalculationVersion() == null
                        ? 0
                        : meeting.getCalculationVersion();


        int versionUpdatedRows =
                meetingMapper.updateCalculationVersion(
                        meeting.getMeetingId(),
                        currentVersion + 1
                );


        if (versionUpdatedRows != 1) {
            throw new IllegalStateException(
                    "모임 계산 버전 변경에 실패했습니다."
            );
        }


        /*
         * 9. ACTIVE → LEFT
         * 위치 입력값도 함께 초기화
         */
        int participantUpdatedRows =
                meetingParticipantMapper
                        .leaveBeforeVoting(
                                participantId
                        );


        if (participantUpdatedRows != 1) {
            throw new IllegalStateException(
                    "모임 나가기에 실패했습니다."
            );
        }


        return new ParticipantLeaveResult(
                participant.getMeetingId(),
                participant.getParticipantId(),
                participant.getUserId(),
                participant.getNickname()
        );


    }

}