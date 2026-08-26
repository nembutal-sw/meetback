package com.meetback.dev.service;

import com.meetback.dev.domain.CandidateEvaluation;
import com.meetback.dev.domain.CandidateReturnResult;
import com.meetback.dev.domain.InputStatus;
import com.meetback.dev.domain.Meeting;
import com.meetback.dev.domain.MeetingCandidate;
import com.meetback.dev.domain.MeetingParticipant;
import com.meetback.dev.repository.CandidateReturnResultMapper;
import com.meetback.dev.repository.MeetingCandidateMapper;
import com.meetback.dev.repository.MeetingMapper;
import com.meetback.dev.repository.MeetingParticipantMapper;
import com.meetback.dev.transport.client.OdsaySubwayClient;
import com.meetback.dev.transport.dto.LastTrainDTO;
import com.meetback.dev.transport.dto.TransitRouteDTO;
import com.meetback.dev.transport.service.RouteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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


    private CandidateReturnResult calculateReturn(
            Long participantId,
            Long candidateId,
            Integer calculationVersion
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


        validateParticipantLocation(
                participant
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


        if (meeting.getDesiredEndAt() == null) {
            throw new IllegalStateException(
                    "모임 희망 종료시간이 설정되어 있지 않습니다."
            );
        }


        MeetingCandidate candidate =
                candidateMapper.findById(
                        candidateId
                );


        if (candidate == null) {
            throw new IllegalArgumentException(
                    "후보 장소를 찾을 수 없습니다."
            );
        }


        if (!participant.getMeetingId()
                .equals(candidate.getMeetingId())) {

            throw new IllegalArgumentException(
                    "참가자와 후보 장소의 모임이 일치하지 않습니다."
            );
        }


        /*
         * 같은 후보 + 같은 참가자 + 같은 계산 버전이면
         * 기존 계산 결과 재사용
         */
        CandidateReturnResult existingResult =
                returnResultMapper.findByCandidateAndParticipantAndVersion(
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


        /*
         * 도보 거리 확인
         */
        if (routeService.isWithinWalkingDistance(
                participantId,
                candidateId
        )) {

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


        /*
         * ODsay 일반 귀가 경로 조회
         */
        TransitRouteDTO route =
                routeService.searchReturnRoute(
                        participantId,
                        candidateId
                );


        LocalDate meetingDate =
                meeting.getDesiredEndAt()
                        .toLocalDate();


        int odsayDay =
                getOdsayDay(
                        meetingDate
                );


        /*
         * ODsay 막차 조회
         */
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


        if (departureTime.isBefore(
                LocalTime.of(
                        5,
                        0
                )
        )) {

            lastTrainDepartureAt =
                    lastTrainDepartureAt.plusDays(
                            1
                    );
        }


        if (arrivalTime.isBefore(
                LocalTime.of(
                        5,
                        0
                )
        )) {

            lastTrainArrivalAt =
                    lastTrainArrivalAt.plusDays(
                            1
                    );
        }


        LocalDateTime lastSafeDepartureAt =
                lastTrainDepartureAt.minusMinutes(
                        SAFE_MARGIN_MINUTES
                );


        boolean canReturn =
                !meeting.getDesiredEndAt()
                        .isAfter(
                                lastSafeDepartureAt
                        );


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


        returnResultMapper.insert(
                result
        );


        return result;
    }


    @Transactional
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


        if (participants == null
                || participants.isEmpty()) {

            throw new IllegalArgumentException(
                    "참가자가 없습니다."
            );
        }


        List<CandidateReturnResult> results =
                new ArrayList<>();


        for (int i = 0; i < participants.size(); i++) {

            MeetingParticipant participant =
                    participants.get(i);


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


    @Transactional
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


        if (candidates == null
                || candidates.isEmpty()) {

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


        for (int i = 0; i < candidates.size(); i++) {

            MeetingCandidate candidate =
                    candidates.get(i);


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


        if (participants == null
                || participants.isEmpty()) {

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


        for (MeetingParticipant participant : participants) {

            validateParticipantLocation(
                    participant
            );
        }
    }


    private void validateParticipantLocation(
            MeetingParticipant participant
    ) {

        if (participant.getDepartureLatitude() == null
                || participant.getDepartureLongitude() == null
                || participant.getReturnLatitude() == null
                || participant.getReturnLongitude() == null) {

            throw new IllegalStateException(
                    "참가자 "
                            + participant.getParticipantId()
                            + "의 출발지 또는 귀가지 정보가 없습니다."
            );
        }
    }
}