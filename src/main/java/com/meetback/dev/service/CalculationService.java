package com.meetback.dev.service;

import com.meetback.dev.domain.CandidateReturnResult;
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


        // 후보 장소에서 참가자 귀가 장소까지 일반 경로 조회
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


        // 일반 경로에서 확인한 출발역, 도착역 기준 막차 조회
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


        // 자정 이후 막차는 다음 날로 처리
        if (departureTime.isBefore(LocalTime.of(5, 0))) {
            lastTrainDepartureAt =
                    lastTrainDepartureAt.plusDays(1);
        }
        if (arrivalTime.isBefore(LocalTime.of(5, 0))) {
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


    // 후보 하나에 대해 참가자 전원 귀가 계산
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
            // 다음 참가자 API 호출 전 대기
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
     * 1 = 평일
     * 2 = 토요일
     * 3 = 일요일
     */
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
}