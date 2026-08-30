package com.meetback.dev.service;

import com.meetback.dev.domain.CandidateEvaluation;
import com.meetback.dev.domain.CandidateReturnResult;
import com.meetback.dev.domain.InputStatus;
import com.meetback.dev.domain.Meeting;
import com.meetback.dev.domain.MeetingCandidate;
import com.meetback.dev.domain.MeetingParticipant;
import com.meetback.dev.domain.MeetingType;
import com.meetback.dev.domain.ParticipantStatus;
import com.meetback.dev.dto.QuickFixedReturnCheckResponseDTO;
import com.meetback.dev.dto.QuickFixedReturnLocationRequestDTO;
import com.meetback.dev.repository.CandidateReturnResultMapper;
import com.meetback.dev.repository.MeetingCandidateMapper;
import com.meetback.dev.repository.MeetingMapper;
import com.meetback.dev.repository.MeetingParticipantMapper;
import com.meetback.dev.transport.client.OdsaySubwayClient;
import com.meetback.dev.transport.dto.LastTrainDTO;
import com.meetback.dev.transport.dto.RouteMapDTO;
import com.meetback.dev.transport.dto.TransitRouteDTO;
import com.meetback.dev.transport.service.RouteService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;


@Service
@RequiredArgsConstructor
public class CalculationService {

    private static final int SAFE_MARGIN_MINUTES = 10;
    private static final int WALKING_RETURN_MINUTES = 10;


    private final MeetingMapper meetingMapper;
    private final MeetingParticipantMapper participantMapper;
    private final MeetingCandidateMapper candidateMapper;
    private final CandidateReturnResultMapper returnResultMapper;
    private final CandidateEvaluationService candidateEvaluationService;
    private final RouteService routeService;
    private final OdsaySubwayClient odsaySubwayClient;
    private final ObjectMapper objectMapper;


    // =========================================================
    // 참가자 1명 + 후보 1개 귀가 계산
    // =========================================================

    public CandidateReturnResult calculateReturn(
            Long participantId,
            Long candidateId
    ) {

        MeetingParticipant participant =
                participantMapper.findById(
                        participantId
                );


        if (participant == null) {

            throw new IllegalArgumentException(
                    "참가자를 찾을 수 없습니다."
            );
        }


        Meeting meeting =
                meetingMapper.findById(
                        participant.getMeetingId()
                );


        if (meeting == null) {

            throw new IllegalArgumentException(
                    "모임을 찾을 수 없습니다."
            );
        }


        Integer calculationVersion =
                meeting.getCalculationVersion();


        if (calculationVersion == null) {

            calculationVersion = 0;
        }


        return calculateReturn(
                participantId,
                candidateId,
                calculationVersion
        );
    }


    // =========================================================
    // QUICK_FIXED
    // 이미 참가한 사용자의 귀가 정보 확인
    // =========================================================

    public QuickFixedReturnCheckResponseDTO calculateQuickFixedReturn(
            Long meetingId,
            Long userId
    ) {

        // =====================================================
        // 1. 모임 조회
        // =====================================================

        Meeting meeting =
                meetingMapper.findById(
                        meetingId
                );


        if (meeting == null) {

            throw new IllegalArgumentException(
                    "모임을 찾을 수 없습니다."
            );
        }


        // =====================================================
        // 2. QUICK_FIXED 확인
        // =====================================================

        if (
                meeting.getMeetingType()
                        != MeetingType.QUICK_FIXED
        ) {

            throw new IllegalStateException(
                    "고정 번개방에서만 사용할 수 있습니다."
            );
        }


        // =====================================================
        // 3. 희망 종료시간 확인
        // =====================================================

        if (meeting.getDesiredEndAt() == null) {

            throw new IllegalStateException(
                    "모임 희망 종료시간이 설정되어 있지 않습니다."
            );
        }


        // =====================================================
        // 4. 현재 로그인 사용자의 참가자 정보 조회
        // =====================================================

        MeetingParticipant participant =
                participantMapper.findByMeetingIdAndUserId(
                        meetingId,
                        userId
                );


        if (participant == null) {

            throw new AccessDeniedException(
                    "해당 모임의 참가자가 아닙니다."
            );
        }


        if (
                participant.getParticipantStatus()
                        != ParticipantStatus.ACTIVE
        ) {

            throw new AccessDeniedException(
                    "현재 참가 중인 사용자만 귀가 정보를 확인할 수 있습니다."
            );
        }


        // =====================================================
        // 5. 귀가지 확인
        // =====================================================

        validateReturnLocation(
                participant
        );


        // =====================================================
        // 6. 고정 장소 조회
        // =====================================================

        Long finalCandidateId =
                meeting.getFinalCandidateId();


        if (finalCandidateId == null) {

            throw new IllegalStateException(
                    "고정 장소가 설정되어 있지 않습니다."
            );
        }


        MeetingCandidate candidate =
                candidateMapper.findById(
                        finalCandidateId
                );


        if (candidate == null) {

            throw new IllegalStateException(
                    "고정 장소 정보를 찾을 수 없습니다."
            );
        }


        if (
                !meetingId.equals(
                        candidate.getMeetingId()
                )
        ) {

            throw new IllegalStateException(
                    "고정 장소와 모임 정보가 일치하지 않습니다."
            );
        }


        // =====================================================
        // 7. 계산 버전
        // =====================================================

        Integer calculationVersion =
                meeting.getCalculationVersion();


        if (calculationVersion == null) {

            calculationVersion = 0;
        }


        // =====================================================
        // 8. 기존 귀가 계산 로직 재사용
        // =====================================================

        CandidateReturnResult result =
                calculateReturn(
                        participant.getParticipantId(),
                        finalCandidateId,
                        calculationVersion
                );


        // =====================================================
        // 9. 희망 종료시간 기준 여유시간
        // =====================================================

        Integer marginMinutes =
                null;


        if (
                result.getLastSafeDepartureAt()
                        != null
        ) {

            marginMinutes =
                    (int) Duration.between(
                            meeting.getDesiredEndAt(),
                            result.getLastSafeDepartureAt()
                    ).toMinutes();
        }


        // =====================================================
        // 10. 지도 데이터
        //
        // 도보:
        // routeMap = null
        //
        // 대중교통:
        // 기존 저장된 mapObj / routeMapData를 이용해
        // RouteService의 기존 지도 로직 재사용
        // =====================================================

        RouteMapDTO routeMap =
                null;


        if (
                result.getLastTrainDepartureAt() != null
                        &&
                        result.getRouteMapObj() != null
                        &&
                        !result.getRouteMapObj().isBlank()
        ) {

            routeMap =
                    routeService.getRouteMap(
                            finalCandidateId,
                            participant.getParticipantId()
                    );
        }


        // =====================================================
        // 11. 화면용 DTO 반환
        // =====================================================

        return new QuickFixedReturnCheckResponseDTO(
                Boolean.TRUE.equals(
                        result.getCanReturn()
                ),
                result.getReturnMinutes(),
                result.getTransferCount(),
                result.getLastTrainDepartureAt(),
                result.getLastSafeDepartureAt(),
                meeting.getDesiredEndAt(),
                marginMinutes,
                routeMap
        );
    }


    // =========================================================
    // QUICK_FIXED
    // 참가 전 귀가 가능 여부 미리 확인
    // =========================================================

    public QuickFixedReturnCheckResponseDTO calculateQuickFixedPreview(
            Long meetingId,
            QuickFixedReturnLocationRequestDTO request
    ) {

        // =====================================================
        // 1. 모임 조회
        // =====================================================

        Meeting meeting =
                meetingMapper.findById(
                        meetingId
                );


        if (meeting == null) {

            throw new IllegalArgumentException(
                    "모임을 찾을 수 없습니다."
            );
        }


        // =====================================================
        // 2. QUICK_FIXED 확인
        // =====================================================

        if (
                meeting.getMeetingType()
                        != MeetingType.QUICK_FIXED
        ) {

            throw new IllegalStateException(
                    "고정 번개방에서만 사용할 수 있습니다."
            );
        }


        // =====================================================
        // 3. 희망 종료시간 확인
        // =====================================================

        if (meeting.getDesiredEndAt() == null) {

            throw new IllegalStateException(
                    "모임 희망 종료시간이 설정되어 있지 않습니다."
            );
        }


        // =====================================================
        // 4. 귀가지 요청 검증
        // =====================================================

        if (request == null) {

            throw new IllegalArgumentException(
                    "귀가 장소 정보가 없습니다."
            );
        }


        if (
                request.name() == null
                        ||
                        request.name().isBlank()
        ) {

            throw new IllegalArgumentException(
                    "귀가 장소를 선택해주세요."
            );
        }


        if (
                request.latitude() == null
                        ||
                        request.longitude() == null
        ) {

            throw new IllegalArgumentException(
                    "귀가 장소 좌표가 올바르지 않습니다."
            );
        }


        // =====================================================
        // 5. 고정 장소 조회
        // =====================================================

        Long finalCandidateId =
                meeting.getFinalCandidateId();


        if (finalCandidateId == null) {

            throw new IllegalStateException(
                    "고정 장소가 설정되어 있지 않습니다."
            );
        }


        MeetingCandidate candidate =
                candidateMapper.findById(
                        finalCandidateId
                );


        if (candidate == null) {

            throw new IllegalStateException(
                    "고정 장소 정보를 찾을 수 없습니다."
            );
        }


        if (
                !meetingId.equals(
                        candidate.getMeetingId()
                )
        ) {

            throw new IllegalStateException(
                    "고정 장소와 모임 정보가 일치하지 않습니다."
            );
        }


        // =====================================================
        // 6. 도보 거리 확인
        // =====================================================

        boolean walking =
                routeService.isWithinWalkingDistance(
                        candidate,
                        request.latitude(),
                        request.longitude()
                );


        // =====================================================
        // 7. 도보 귀가
        //
        // 도보는 대중교통 경로 / 막차 / 지도 경로 없음
        // =====================================================

        if (walking) {

            return new QuickFixedReturnCheckResponseDTO(
                    true,
                    WALKING_RETURN_MINUTES,
                    0,
                    null,
                    null,
                    meeting.getDesiredEndAt(),
                    null,
                    null
            );
        }


        // =====================================================
        // 8. ODsay 일반 귀가 경로
        // =====================================================

        TransitRouteDTO route =
                routeService.searchReturnRoute(
                        candidate,
                        request.name().trim(),
                        request.address(),
                        request.longitude(),
                        request.latitude()
                );


        // =====================================================
        // 9. 기준 날짜
        // =====================================================

        LocalDateTime desiredEndAt =
                meeting.getDesiredEndAt();


        LocalDate meetingDate =
                desiredEndAt.toLocalDate();


        /*
         * 새벽 05:00 이전 종료라면
         * 전날 운행일 기준으로 막차를 조회한다.
         */
        if (
                desiredEndAt.toLocalTime()
                        .isBefore(
                                LocalTime.of(
                                        5,
                                        0
                                )
                        )
        ) {

            meetingDate =
                    meetingDate.minusDays(
                            1
                    );
        }


        int odsayDay =
                getOdsayDay(
                        meetingDate
                );


        // =====================================================
        // 10. 막차 조회
        // =====================================================

        LastTrainDTO lastTrain =
                odsaySubwayClient.findLastTrain(
                        route.startStationId(),
                        route.endStationId(),
                        odsayDay
                );


        if (lastTrain == null) {

            throw new IllegalStateException(
                    "막차 정보를 조회할 수 없습니다."
            );
        }


        // =====================================================
        // 11. 막차 시간 변환
        // =====================================================

        LocalTime departureTime =
                LocalTime.parse(
                        lastTrain.departureTime()
                );


        LocalTime arrivalTime =
                LocalTime.parse(
                        lastTrain.arrivalTime()
                );


        LocalDateTime lastTrainDepartureAt =
                LocalDateTime.of(
                        meetingDate,
                        departureTime
                );


        LocalDateTime lastTrainArrivalAt =
                LocalDateTime.of(
                        meetingDate,
                        arrivalTime
                );


        // =====================================================
        // 12. 자정 이후 날짜 보정
        // =====================================================

        if (
                departureTime.isBefore(
                        LocalTime.of(
                                5,
                                0
                        )
                )
        ) {

            lastTrainDepartureAt =
                    lastTrainDepartureAt.plusDays(
                            1
                    );
        }


        if (
                arrivalTime.isBefore(
                        LocalTime.of(
                                5,
                                0
                        )
                )
        ) {

            lastTrainArrivalAt =
                    lastTrainArrivalAt.plusDays(
                            1
                    );
        }


        // =====================================================
        // 13. 안전 출발 마감시간
        // =====================================================

        LocalDateTime lastSafeDepartureAt =
                lastTrainDepartureAt.minusMinutes(
                        SAFE_MARGIN_MINUTES
                );


        // =====================================================
        // 14. 귀가 가능 여부
        // =====================================================

        boolean canReturn =
                !desiredEndAt.isAfter(
                        lastSafeDepartureAt
                );


        // =====================================================
        // 15. 여유시간
        //
        // 양수 = 여유 있음
        // 0   = 정확히 마감
        // 음수 = 안전 출발시간 초과
        // =====================================================

        int marginMinutes =
                (int) Duration.between(
                        desiredEndAt,
                        lastSafeDepartureAt
                ).toMinutes();


        // =====================================================
        // 16. 네이버 지도용 경로 데이터
        //
        // searchReturnRoute에서 받은 mapObj를 이용해
        // ODsay loadLane 호출
        //
        // lines = 실제 지도 좌표
        // steps = 역 / 호선 안내
        // =====================================================

        RouteMapDTO routeMap =
                routeService.createPreviewRouteMap(
                        candidate,
                        request.name().trim(),
                        route
                );


        // =====================================================
        // 17. 화면용 DTO 반환
        // =====================================================

        return new QuickFixedReturnCheckResponseDTO(
                canReturn,
                route.totalMinutes(),
                route.transferCount(),
                lastTrainDepartureAt,
                lastSafeDepartureAt,
                desiredEndAt,
                marginMinutes,
                routeMap
        );
    }


    // =========================================================
    // 실제 귀가 계산
    // =========================================================

    private CandidateReturnResult calculateReturn(
            Long participantId,
            Long candidateId,
            Integer calculationVersion
    ) {

        // =====================================================
        // 참가자 조회
        // =====================================================

        MeetingParticipant participant =
                participantMapper.findById(
                        participantId
                );


        if (participant == null) {

            throw new IllegalArgumentException(
                    "참가자를 찾을 수 없습니다."
            );
        }


        validateReturnLocation(
                participant
        );


        // =====================================================
        // 모임 조회
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


        if (meeting.getDesiredEndAt() == null) {

            throw new IllegalStateException(
                    "모임 희망 종료시간이 설정되어 있지 않습니다."
            );
        }


        // =====================================================
        // 후보 장소 조회
        // =====================================================

        MeetingCandidate candidate =
                candidateMapper.findById(
                        candidateId
                );


        if (candidate == null) {

            throw new IllegalArgumentException(
                    "후보 장소를 찾을 수 없습니다."
            );
        }


        if (
                !participant.getMeetingId()
                        .equals(
                                candidate.getMeetingId()
                        )
        ) {

            throw new IllegalArgumentException(
                    "참가자와 후보 장소의 모임이 일치하지 않습니다."
            );
        }


        // =====================================================
        // 기존 결과 재사용
        // =====================================================

        CandidateReturnResult existingResult =
                returnResultMapper
                        .findByCandidateAndParticipantAndVersion(
                                candidateId,
                                participantId,
                                calculationVersion
                        );


        if (existingResult != null) {

            System.out.println(
                    "[기존 계산 결과 재사용] candidateId="
                            + candidateId
                            + ", participantId="
                            + participantId
                            + ", calculationVersion="
                            + calculationVersion
            );


            return existingResult;
        }


        // =====================================================
        // 도보 거리 확인
        // =====================================================

        if (
                routeService.isWithinWalkingDistance(
                        participantId,
                        candidateId
                )
        ) {

            CandidateReturnResult result =
                    new CandidateReturnResult();


            result.setMeetingId(
                    participant.getMeetingId()
            );


            result.setCandidateId(
                    candidate.getCandidateId()
            );


            result.setParticipantId(
                    participant.getParticipantId()
            );


            result.setCalculationVersion(
                    calculationVersion
            );


            result.setReturnMinutes(
                    WALKING_RETURN_MINUTES
            );


            result.setTransferCount(
                    0
            );


            result.setRouteMapObj(
                    null
            );


            result.setRouteMapData(
                    null
            );


            result.setLastTrainDepartureAt(
                    null
            );


            result.setLastTrainArrivalAt(
                    null
            );


            result.setLastSafeDepartureAt(
                    null
            );


            result.setCanReturn(
                    true
            );


            returnResultMapper.insert(
                    result
            );


            System.out.println(
                    "[도보 처리] candidateId="
                            + candidateId
                            + ", participantId="
                            + participantId
                            + ", calculationVersion="
                            + calculationVersion
                            + ", returnMinutes="
                            + WALKING_RETURN_MINUTES
            );


            return result;
        }


        // =====================================================
        // ODsay 일반 귀가 경로 조회
        // =====================================================

        TransitRouteDTO route =
                routeService.searchReturnRoute(
                        participantId,
                        candidateId
                );


        // =====================================================
        // 날짜 기준
        // =====================================================

        LocalDateTime desiredEndAt =
                meeting.getDesiredEndAt();


        LocalDate meetingDate =
                desiredEndAt.toLocalDate();


        if (
                desiredEndAt.toLocalTime()
                        .isBefore(
                                LocalTime.of(
                                        5,
                                        0
                                )
                        )
        ) {

            meetingDate =
                    meetingDate.minusDays(
                            1
                    );
        }


        int odsayDay =
                getOdsayDay(
                        meetingDate
                );


        // =====================================================
        // 막차 조회
        // =====================================================

        LastTrainDTO lastTrain =
                odsaySubwayClient.findLastTrain(
                        route.startStationId(),
                        route.endStationId(),
                        odsayDay
                );


        if (lastTrain == null) {

            throw new IllegalStateException(
                    "막차 정보를 조회할 수 없습니다."
            );
        }


        // =====================================================
        // 막차 시간
        // =====================================================

        LocalTime departureTime =
                LocalTime.parse(
                        lastTrain.departureTime()
                );


        LocalTime arrivalTime =
                LocalTime.parse(
                        lastTrain.arrivalTime()
                );


        LocalDateTime lastTrainDepartureAt =
                LocalDateTime.of(
                        meetingDate,
                        departureTime
                );


        LocalDateTime lastTrainArrivalAt =
                LocalDateTime.of(
                        meetingDate,
                        arrivalTime
                );


        // =====================================================
        // 자정 이후 날짜 보정
        // =====================================================

        if (
                departureTime.isBefore(
                        LocalTime.of(
                                5,
                                0
                        )
                )
        ) {

            lastTrainDepartureAt =
                    lastTrainDepartureAt.plusDays(
                            1
                    );
        }


        if (
                arrivalTime.isBefore(
                        LocalTime.of(
                                5,
                                0
                        )
                )
        ) {

            lastTrainArrivalAt =
                    lastTrainArrivalAt.plusDays(
                            1
                    );
        }


        // =====================================================
        // 안전 마진
        // =====================================================

        LocalDateTime lastSafeDepartureAt =
                lastTrainDepartureAt.minusMinutes(
                        SAFE_MARGIN_MINUTES
                );


        boolean canReturn =
                !meeting.getDesiredEndAt()
                        .isAfter(
                                lastSafeDepartureAt
                        );


        // =====================================================
        // 결과 객체 생성
        // =====================================================

        CandidateReturnResult result =
                new CandidateReturnResult();


        result.setMeetingId(
                participant.getMeetingId()
        );


        result.setCandidateId(
                candidate.getCandidateId()
        );


        result.setParticipantId(
                participant.getParticipantId()
        );


        result.setCalculationVersion(
                calculationVersion
        );


        result.setReturnMinutes(
                route.totalMinutes()
        );


        result.setTransferCount(
                route.transferCount()
        );


        // =====================================================
        // 지도 mapObj 저장
        // =====================================================

        result.setRouteMapObj(
                route.mapObj()
        );


        // =====================================================
        // 지도 안내 정보 초기 저장
        //
        // 아직 loadLane 좌표는 저장하지 않고
        // steps만 저장한다.
        //
        // 이후 지도를 처음 조회하면
        // RouteService.getRouteMap()에서
        // loadLane 좌표를 합쳐 다시 저장한다.
        // =====================================================

        try {

            RouteMapDTO initialRouteMap =
                    new RouteMapDTO(
                            candidate.getPlaceName(),
                            participant.getReturnName(),
                            List.of(),
                            route.steps()
                    );


            result.setRouteMapData(
                    objectMapper.writeValueAsString(
                            initialRouteMap
                    )
            );

        } catch (JacksonException e) {

            throw new IllegalStateException(
                    "귀가 경로 정보를 저장할 수 없습니다.",
                    e
            );
        }


        // =====================================================
        // 막차 정보
        // =====================================================

        result.setLastTrainDepartureAt(
                lastTrainDepartureAt
        );


        result.setLastTrainArrivalAt(
                lastTrainArrivalAt
        );


        result.setLastSafeDepartureAt(
                lastSafeDepartureAt
        );


        result.setCanReturn(
                canReturn
        );


        // =====================================================
        // DB 저장
        // =====================================================

        returnResultMapper.insert(
                result
        );


        return result;
    }


    // =========================================================
    // 후보 장소 하나 계산
    // =========================================================

    public List<CandidateReturnResult> calculateCandidate(
            Long candidateId
    ) {

        MeetingCandidate candidate =
                candidateMapper.findById(
                        candidateId
                );


        if (candidate == null) {

            throw new IllegalArgumentException(
                    "후보 장소를 찾을 수 없습니다."
            );
        }


        Meeting meeting =
                meetingMapper.findById(
                        candidate.getMeetingId()
                );


        if (meeting == null) {

            throw new IllegalArgumentException(
                    "모임을 찾을 수 없습니다."
            );
        }


        Integer calculationVersion =
                meeting.getCalculationVersion();


        if (calculationVersion == null) {

            calculationVersion = 0;
        }


        return calculateCandidate(
                candidateId,
                calculationVersion
        );
    }


    // =========================================================
    // 후보 장소 하나 계산 - 내부용
    // =========================================================

    private List<CandidateReturnResult> calculateCandidate(
            Long candidateId,
            Integer calculationVersion
    ) {

        MeetingCandidate candidate =
                candidateMapper.findById(
                        candidateId
                );


        if (candidate == null) {

            throw new IllegalArgumentException(
                    "후보 장소를 찾을 수 없습니다."
            );
        }


        validateAllParticipantsSubmitted(
                candidate.getMeetingId()
        );


        List<MeetingParticipant> participants =
                participantMapper.findByMeetingId(
                        candidate.getMeetingId()
                );


        if (
                participants == null
                        ||
                        participants.isEmpty()
        ) {

            throw new IllegalArgumentException(
                    "참가자가 없습니다."
            );
        }


        List<CandidateReturnResult> results =
                new ArrayList<>();


        for (
                int i = 0;
                i < participants.size();
                i++
        ) {

            MeetingParticipant participant =
                    participants.get(
                            i
                    );


            CandidateReturnResult result =
                    calculateReturn(
                            participant.getParticipantId(),
                            candidateId,
                            calculationVersion
                    );


            results.add(
                    result
            );
        }


        candidateEvaluationService.evaluateAndSave(
                candidateId,
                candidate.getMeetingId(),
                calculationVersion,
                results
        );


        return results;
    }


    // =========================================================
    // 모임 전체 계산
    // =========================================================

    public List<CandidateEvaluation> calculateMeeting(
            Long meetingId
    ) {

        validateAllParticipantsSubmitted(
                meetingId
        );


        Meeting meeting =
                meetingMapper.findById(
                        meetingId
                );


        if (meeting == null) {

            throw new IllegalArgumentException(
                    "모임을 찾을 수 없습니다."
            );
        }


        if (meeting.getDesiredEndAt() == null) {

            throw new IllegalStateException(
                    "모임 희망 종료시간이 설정되어 있지 않습니다."
            );
        }


        List<MeetingCandidate> candidates =
                candidateMapper.findByMeetingId(
                        meetingId
                );


        if (
                candidates == null
                        ||
                        candidates.isEmpty()
        ) {

            throw new IllegalArgumentException(
                    "후보 장소가 없습니다."
            );
        }


        Integer currentVersion =
                meeting.getCalculationVersion();


        if (currentVersion == null) {

            currentVersion = 0;
        }


        List<CandidateEvaluation> evaluations =
                new ArrayList<>();


        for (
                int i = 0;
                i < candidates.size();
                i++
        ) {

            MeetingCandidate candidate =
                    candidates.get(
                            i
                    );


            calculateCandidate(
                    candidate.getCandidateId(),
                    currentVersion
            );


            CandidateEvaluation evaluation =
                    candidateEvaluationService
                            .findByCandidateId(
                                    candidate.getCandidateId()
                            );


            if (evaluation == null) {

                throw new IllegalStateException(
                        "후보 평가 결과를 찾을 수 없습니다."
                );
            }


            evaluations.add(
                    evaluation
            );
        }


        candidateEvaluationService.rankCandidates(
                evaluations
        );


        System.out.println(
                "[모임 계산 완료] meetingId="
                        + meetingId
                        + ", calculationVersion="
                        + currentVersion
        );


        return evaluations;
    }


    // =========================================================
    // ODsay 요일
    //
    // 1 = 평일
    // 2 = 토요일
    // 3 = 일요일
    // =========================================================

    private int getOdsayDay(
            LocalDate date
    ) {

        return switch (
                date.getDayOfWeek()
                ) {

            case SATURDAY -> 2;

            case SUNDAY -> 3;

            default -> 1;
        };
    }


    // =========================================================
    // 참가자 전체 입력 완료 검증
    // =========================================================

    private void validateAllParticipantsSubmitted(
            Long meetingId
    ) {

        Meeting meeting =
                meetingMapper.findById(
                        meetingId
                );


        if (meeting == null) {

            throw new IllegalArgumentException(
                    "모임을 찾을 수 없습니다."
            );
        }


        List<MeetingParticipant> participants =
                participantMapper.findByMeetingId(
                        meetingId
                );


        if (
                participants == null
                        ||
                        participants.isEmpty()
        ) {

            throw new IllegalArgumentException(
                    "참가자가 없습니다."
            );
        }


        boolean allSubmitted =
                participants.stream()
                        .allMatch(
                                participant ->
                                        participant.getInputStatus()
                                                == InputStatus.SUBMITTED
                        );


        if (!allSubmitted) {

            throw new IllegalStateException(
                    "아직 장소 입력을 완료하지 않은 참가자가 있습니다."
            );
        }


        for (
                MeetingParticipant participant
                :
                participants
        ) {

            validateParticipantLocation(
                    participant
            );
        }
    }


    // =========================================================
    // 출발지 + 귀가지 검증
    // =========================================================

    private void validateParticipantLocation(
            MeetingParticipant participant
    ) {

        if (
                participant.getDepartureLatitude() == null
                        ||
                        participant.getDepartureLongitude() == null
                        ||
                        participant.getReturnLatitude() == null
                        ||
                        participant.getReturnLongitude() == null
        ) {

            throw new IllegalStateException(
                    "참가자 "
                            + participant.getParticipantId()
                            + "의 출발지 또는 귀가지 정보가 없습니다."
            );
        }
    }


    // =========================================================
    // 귀가지 검증
    // =========================================================

    private void validateReturnLocation(
            MeetingParticipant participant
    ) {

        if (
                participant.getReturnLatitude() == null
                        ||
                        participant.getReturnLongitude() == null
        ) {

            throw new IllegalStateException(
                    "귀가 장소를 먼저 등록해주세요."
            );
        }
    }
}