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

        // 1. 참가자 조회
        MeetingParticipant participant =
                participantMapper.findById(participantId);

        if (participant == null) {
            throw new IllegalArgumentException(
                    "참가자를 찾을 수 없습니다."
            );
        }


        // 2. 모임 조회
        Meeting meeting =
                meetingMapper.findById(
                        participant.getMeetingId()
                );

        if (meeting == null) {
            throw new IllegalArgumentException(
                    "모임을 찾을 수 없습니다."
            );
        }


        // 3. 후보 장소 조회
        MeetingCandidate candidate =
                candidateMapper.findById(candidateId);

        if (candidate == null) {
            throw new IllegalArgumentException(
                    "후보 장소를 찾을 수 없습니다."
            );
        }


        // 4. 후보 장소 -> 참가자 귀가 장소 일반 경로 조회
        TransitRouteDTO route =
                routeService.searchReturnRoute(
                        participantId,
                        candidateId
                );


        // 5. ODsay 연속 호출 방지를 위해 1.1초 대기
        waitForOdsay();


        // 6. 모임 날짜 확인
        LocalDate meetingDate =
                meeting.getDesiredEndAt()
                        .toLocalDate();


        // 7. ODsay DAY 값 계산
        int odsayDay =
                getOdsayDay(meetingDate);


        // 8. 일반 경로에서 구한 역 ID로 막차 조회
        LastTrainDTO lastTrain =
                odsaySubwayClient.findLastTrain(
                        route.startStationId(),
                        route.endStationId(),
                        odsayDay
                );


        // 9. 막차 시간 변환
        LocalTime departureTime =
                LocalTime.parse(
                        lastTrain.departureTime()
                );

        LocalTime arrivalTime =
                LocalTime.parse(
                        lastTrain.arrivalTime()
                );


        // 10. 모임 날짜 기준 막차 날짜 생성
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
        if (departureTime.isBefore(
                LocalTime.of(5, 0)
        )) {

            lastTrainDepartureAt =
                    lastTrainDepartureAt.plusDays(1);
        }


        if (arrivalTime.isBefore(
                LocalTime.of(5, 0)
        )) {

            lastTrainArrivalAt =
                    lastTrainArrivalAt.plusDays(1);
        }


        // 11. 막차 출발시간에서 안전마진 10분 차감
        LocalDateTime lastSafeDepartureAt =
                lastTrainDepartureAt.minusMinutes(
                        SAFE_MARGIN_MINUTES
                );


        // 12. 희망 종료시간 기준 귀가 가능 여부 계산
        boolean canReturn =
                !meeting.getDesiredEndAt()
                        .isAfter(lastSafeDepartureAt);


        // 13. 결과 객체 생성
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


        // 14. DB 저장
        returnResultMapper.insert(result);

        return result;
    }


    // 후보 하나에 대해 참가자 전원 계산
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


            /*
             * 참가자 A 막차 조회
             * ↓ 1.1초
             * 참가자 B 일반 경로 조회
             *
             * 마지막 참가자 뒤에는 대기 불필요
             */
            if (i < participants.size() - 1) {
                waitForOdsay();
            }
        }


        // 참가자 전원 계산 후 후보 평가 및 저장
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
     * 3 = 일요일 / 공휴일
     */
    private int getOdsayDay(LocalDate date) {

        return switch (date.getDayOfWeek()) {

            case SATURDAY -> 2;

            case SUNDAY -> 3;

            default -> 1;
        };
    }


    // ODsay 연속 호출 방지
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