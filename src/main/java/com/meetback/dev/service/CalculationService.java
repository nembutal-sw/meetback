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
                participantMapper.findById(participantId);

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
                candidateMapper.findById(candidateId);

        if (candidate == null) {
            throw new IllegalArgumentException(
                    "후보 장소를 찾을 수 없습니다."
            );
        }


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
                getOdsayDay(meetingDate);


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


        if (departureTime.isBefore(LocalTime.of(5,0))) {
            lastTrainDepartureAt =
                    lastTrainDepartureAt.plusDays(1);
        }


        if (arrivalTime.isBefore(LocalTime.of(5,0))) {
            lastTrainArrivalAt =
                    lastTrainArrivalAt.plusDays(1);
        }


        LocalDateTime lastSafeDepartureAt =
                lastTrainDepartureAt.minusMinutes(
                        SAFE_MARGIN_MINUTES
                );


        boolean canReturn =
                !meeting.getDesiredEndAt()
                        .isAfter(lastSafeDepartureAt);



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


        returnResultMapper.insert(result);

        return result;
    }



    // 후보 하나 + 참가자 전원 계산
    public List<CandidateReturnResult> calculateCandidate(
            Long candidateId
    ) {

        MeetingCandidate candidate =
                candidateMapper.findById(candidateId);


        if (candidate == null) {
            throw new IllegalArgumentException(
                    "후보 장소를 찾을 수 없습니다."
            );
        }


        // 재계산 시 기존 결과 삭제
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


            results.add(result);


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



    private int getOdsayDay(LocalDate date) {

        return switch (date.getDayOfWeek()) {

            case SATURDAY -> 2;

            case SUNDAY -> 3;

            default -> 1;
        };
    }



    private void waitForOdsay() {

        try {

            Thread.sleep(1100);

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();

            throw new IllegalStateException(
                    "ODsay 호출 대기 중 오류가 발생했습니다.",
                    e
            );
        }
    }



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


            calculateCandidate(
                    candidate.getCandidateId()
            );


            CandidateEvaluation evaluation =
                    candidateEvaluationService
                            .findByCandidateId(
                                    candidate.getCandidateId()
                            );


            evaluations.add(evaluation);


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