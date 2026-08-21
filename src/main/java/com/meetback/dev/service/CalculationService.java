package com.meetback.dev.service;

import com.meetback.dev.domain.*;
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


        MeetingCandidate candidate =
                candidateMapper.findById(
                        candidateId
                );

        if (candidate == null) {
            throw new IllegalArgumentException(
                    "후보 장소를 찾을 수 없습니다."
            );
        }


        /*
         * 후보 장소와 귀가 장소가 700m 이내이면
         * ODsay를 호출하지 않고 도보 10분으로 처리
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
                    meeting.getCalculationVersion()
            );

            result.setReturnMinutes(
                    WALKING_RETURN_MINUTES
            );

            result.setTransferCount(
                    0
            );

            /*
             * 도보 귀가이므로
             * 막차 정보는 없음
             */
            result.setLastTrainDepartureAt(
                    null
            );

            result.setLastTrainArrivalAt(
                    null
            );

            result.setLastSafeDepartureAt(
                    null
            );

            /*
             * 막차 제한이 없으므로 귀가 가능
             */
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
                            + ", returnMinutes="
                            + WALKING_RETURN_MINUTES
            );


            return result;
        }


        /*
         * 700m 초과이면 기존 ODsay 경로 조회
         */
        TransitRouteDTO route =
                routeService.searchReturnRoute(
                        participantId,
                        candidateId
                );


        waitForOdsay();


        LocalDate meetingDate =
                meeting.getDesiredEndAt()
                        .toLocalDate();


        int odsayDay =
                getOdsayDay(
                        meetingDate
                );


        LastTrainDTO lastTrain =
                odsaySubwayClient.findLastTrain(
                        route.startStationId(),
                        route.endStationId(),
                        odsayDay
                );


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


        /*
         * 자정 이후 막차는 다음 날로 처리
         */
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
                meeting.getCalculationVersion()
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


    /*
     * 후보 하나 + 참가자 전원 계산
     */
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


        /*
         * 재계산 시 기존 결과 삭제
         */
        returnResultMapper.deleteByCandidateId(
                candidateId
        );


        List<MeetingParticipant> participants =
                participantMapper.findByMeetingId(
                        candidate.getMeetingId()
                );


        if (participants.isEmpty()) {
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
                            candidateId
                    );


            results.add(
                    result
            );


            /*
             * 도보 처리된 경우에는 ODsay를 호출하지 않았지만
             * 다음 참가자가 ODsay를 호출할 수도 있으므로
             * 기존 대기 로직은 유지
             */
            if (i < participants.size() - 1) {
                waitForOdsay();
            }
        }


        candidateEvaluationService.evaluateAndSave(
                candidateId,
                candidate.getMeetingId(),
                results
        );


        return results;
    }


    /*
     * ODsay DAY
     *
     * 1 = 평일
     * 2 = 토요일
     * 3 = 일요일
     */
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


    /*
     * ODsay 호출 간 대기
     */
    private void waitForOdsay() {

        try {

            Thread.sleep(
                    1100
            );

        } catch (InterruptedException e) {

            Thread.currentThread()
                    .interrupt();


            throw new IllegalStateException(
                    "ODsay 호출 대기 중 오류가 발생했습니다.",
                    e
            );
        }
    }


    /*
     * 모임 전체 후보 계산
     */
    public List<CandidateEvaluation> calculateMeeting(
            Long meetingId
    ) {

        List<MeetingCandidate> candidates =
                candidateMapper.findByMeetingId(
                        meetingId
                );


        if (candidates.isEmpty()) {

            throw new IllegalArgumentException(
                    "후보 장소가 없습니다."
            );
        }


        List<CandidateEvaluation> evaluations =
                new ArrayList<>();


        for (int i = 0; i < candidates.size(); i++) {


            MeetingCandidate candidate =
                    candidates.get(i);


            /*
             * 후보 하나 × 참가자 전원 계산
             */
            calculateCandidate(
                    candidate.getCandidateId()
            );


            CandidateEvaluation evaluation =
                    candidateEvaluationService
                            .findByCandidateId(
                                    candidate.getCandidateId()
                            );


            evaluations.add(
                    evaluation
            );


            if (i < candidates.size() - 1) {
                waitForOdsay();
            }
        }


        candidateEvaluationService.rankCandidates(
                evaluations
        );


        return evaluations;
    }
}