package com.meetback.dev.service;

import com.meetback.dev.domain.CandidateEvaluation;
import com.meetback.dev.domain.CandidateReturnResult;
import com.meetback.dev.domain.Meeting;
import com.meetback.dev.domain.MeetingCandidate;
import com.meetback.dev.dto.CandidateRankingResponseDTO;
import com.meetback.dev.dto.ParticipantReturnSummaryDTO;
import com.meetback.dev.repository.CandidateEvaluationMapper;
import com.meetback.dev.repository.CandidateReturnResultMapper;
import com.meetback.dev.repository.MeetingCandidateMapper;
import com.meetback.dev.repository.MeetingMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CandidateEvaluationService {

    private final CandidateEvaluationMapper candidateEvaluationMapper;
    private final MeetingMapper meetingMapper;
    private final RuleEngineService ruleEngineService;
    private final MeetingCandidateMapper candidateMapper;
    private final CandidateReturnResultMapper returnResultMapper;


    public LocalDateTime calculateDeadline(
            List<CandidateReturnResult> results
    ) {

        if (results == null || results.isEmpty()) {
            throw new IllegalArgumentException(
                    "귀가 계산 결과가 없습니다."
            );
        }

        return results.stream()
                .map(CandidateReturnResult::getLastSafeDepartureAt)
                .filter(time -> time != null)
                .min(LocalDateTime::compareTo)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "마지막 안전 출발시간이 없습니다."
                        )
                );
    }


    public int calculateGoldenMargin(
            LocalDateTime desiredEndAt,
            LocalDateTime deadlineAt
    ) {

        if (desiredEndAt == null || deadlineAt == null) {
            throw new IllegalArgumentException(
                    "종료시간 또는 Deadline이 없습니다."
            );
        }

        LocalDateTime desired =
                desiredEndAt.withSecond(0).withNano(0);

        LocalDateTime deadline =
                deadlineAt.withSecond(0).withNano(0);

        return (int) Duration.between(
                desired,
                deadline
        ).toMinutes();
    }


    public int calculateFairnessGap(
            List<CandidateReturnResult> results
    ) {

        if (results == null || results.isEmpty()) {
            throw new IllegalArgumentException(
                    "귀가 계산 결과가 없습니다."
            );
        }

        int minReturnMinutes =
                results.stream()
                        .map(CandidateReturnResult::getReturnMinutes)
                        .filter(minutes -> minutes != null)
                        .min(Integer::compareTo)
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "귀가 시간이 없습니다."
                                )
                        );

        int maxReturnMinutes =
                results.stream()
                        .map(CandidateReturnResult::getReturnMinutes)
                        .filter(minutes -> minutes != null)
                        .max(Integer::compareTo)
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "귀가 시간이 없습니다."
                                )
                        );

        return maxReturnMinutes - minReturnMinutes;
    }


    public int calculateFairnessScore(
            int fairnessGapMinutes
    ) {

        if (fairnessGapMinutes <= 10) {
            return 50;
        }

        if (fairnessGapMinutes <= 20) {
            return 40;
        }

        if (fairnessGapMinutes <= 30) {
            return 30;
        }

        if (fairnessGapMinutes <= 40) {
            return 20;
        }

        return 10;
    }


    public CandidateEvaluation evaluateAndSave(
            Long candidateId,
            Long meetingId,
            List<CandidateReturnResult> results
    ) {

        Meeting meeting =
                meetingMapper.findById(meetingId);

        if (meeting == null) {
            throw new IllegalArgumentException(
                    "모임을 찾을 수 없습니다."
            );
        }

        LocalDateTime deadline =
                calculateDeadline(results);

        int goldenMargin =
                calculateGoldenMargin(
                        meeting.getDesiredEndAt(),
                        deadline
                );

        int fairnessGap =
                calculateFairnessGap(results);

        int fairnessScore =
                calculateFairnessScore(fairnessGap);

        double averageReturnMinutes =
                results.stream()
                        .map(CandidateReturnResult::getReturnMinutes)
                        .filter(minutes -> minutes != null)
                        .mapToInt(Integer::intValue)
                        .average()
                        .orElse(0.0);

        boolean allReturnable =
                results.stream()
                        .allMatch(result ->
                                Boolean.TRUE.equals(
                                        result.getCanReturn()
                                )
                        );

        double ruleScore =
                ruleEngineService.calculateRuleScore(
                        allReturnable,
                        fairnessScore,
                        averageReturnMinutes,
                        results
                );

        CandidateEvaluation evaluation =
                new CandidateEvaluation();

        evaluation.setCandidateId(
                candidateId
        );

        evaluation.setCalculationVersion(
                meeting.getCalculationVersion()
        );

        evaluation.setAllReturnable(
                allReturnable
        );

        evaluation.setDeadlineAt(
                deadline
        );

        evaluation.setGoldenMarginMinutes(
                goldenMargin
        );

        evaluation.setAverageReturnMinutes(
                averageReturnMinutes
        );

        evaluation.setFairnessGapMinutes(
                fairnessGap
        );

        evaluation.setFairnessScore(
                fairnessScore
        );

        evaluation.setRuleScore(
                ruleScore
        );

        // 전체 후보 계산 후 순위 결정
        evaluation.setRecommendationRank(
                null
        );


        CandidateEvaluation saved =
                candidateEvaluationMapper.findByCandidateId(
                        candidateId
                );

        if (saved == null) {

            candidateEvaluationMapper.insert(
                    evaluation
            );

        } else {

            candidateEvaluationMapper.update(
                    evaluation
            );
        }

        return evaluation;
    }


    public CandidateEvaluation findByCandidateId(
            Long candidateId
    ) {

        return candidateEvaluationMapper
                .findByCandidateId(
                        candidateId
                );
    }


    public void rankCandidates(
            List<CandidateEvaluation> evaluations
    ) {

        if (evaluations == null || evaluations.isEmpty()) {
            throw new IllegalArgumentException(
                    "후보 평가 결과가 없습니다."
            );
        }

        evaluations.sort(
                (a, b) -> {

                    // 1. Rule Score 높은 후보 우선
                    int scoreCompare =
                            Double.compare(
                                    b.getRuleScore(),
                                    a.getRuleScore()
                            );

                    if (scoreCompare != 0) {
                        return scoreCompare;
                    }

                    // 2. 동점이면 Golden Margin 큰 후보 우선
                    int marginCompare =
                            Integer.compare(
                                    b.getGoldenMarginMinutes(),
                                    a.getGoldenMarginMinutes()
                            );

                    if (marginCompare != 0) {
                        return marginCompare;
                    }

                    // 3. 그래도 동점이면 평균 귀가시간 짧은 후보 우선
                    return Double.compare(
                            a.getAverageReturnMinutes(),
                            b.getAverageReturnMinutes()
                    );
                }
        );


        for (int i = 0; i < evaluations.size(); i++) {

            CandidateEvaluation evaluation =
                    evaluations.get(i);

            int rank =
                    i + 1;

            evaluation.setRecommendationRank(
                    rank
            );

            candidateEvaluationMapper
                    .updateRecommendationRank(
                            evaluation.getCandidateId(),
                            rank
                    );
        }
    }


    /*
     * 추천 1위 조회
     */
    public CandidateRankingResponseDTO getTopRecommendation(
            Long meetingId
    ) {

        CandidateEvaluation evaluation =
                candidateEvaluationMapper
                        .findTopRankedByMeetingId(
                                meetingId
                        );

        if (evaluation == null) {
            throw new IllegalStateException(
                    "추천 결과가 없습니다."
            );
        }

        return toRankingResponse(
                evaluation
        );
    }


    /*
     * 후보 전체 순위 조회
     */
    public List<CandidateRankingResponseDTO> getRanking(
            Long meetingId
    ) {

        List<CandidateEvaluation> evaluations =
                candidateEvaluationMapper
                        .findRankingByMeetingId(
                                meetingId
                        );

        if (evaluations == null
                || evaluations.isEmpty()) {

            throw new IllegalStateException(
                    "후보 순위 결과가 없습니다."
            );
        }

        return evaluations.stream()
                .map(this::toRankingResponse)
                .toList();
    }


    /*
     * 평가 결과 + 후보 장소 + 참가자별 귀가 결과
     * → 추천 결과 DTO 변환
     */
    private CandidateRankingResponseDTO toRankingResponse(
            CandidateEvaluation evaluation
    ) {

        MeetingCandidate candidate =
                candidateMapper.findById(
                        evaluation.getCandidateId()
                );

        if (candidate == null) {
            throw new IllegalStateException(
                    "후보 장소를 찾을 수 없습니다."
            );
        }


        /*
         * 귀가시간 최대 - 최소 차이가
         * 10분 이하이면 공정한 장소
         */
        boolean fairPlace =
                evaluation.getFairnessGapMinutes() != null
                        && evaluation.getFairnessGapMinutes() <= 10;


        /*
         * 이미 계산되어 DB에 저장된 참가자별 귀가 결과 조회
         * ODsay 재호출 없음
         */
        List<CandidateReturnResult> returnResults =
                returnResultMapper
                        .findByCandidateIdAndVersion(
                                evaluation.getCandidateId(),
                                evaluation.getCalculationVersion()
                        );


        List<ParticipantReturnSummaryDTO> participantResults =
                returnResults.stream()
                        .map(result ->
                                new ParticipantReturnSummaryDTO(

                                        result.getParticipantId(),

                                        result.getReturnMinutes(),

                                        result.getTransferCount(),

                                        result.getLastTrainDepartureAt(),

                                        result.getLastTrainArrivalAt(),

                                        result.getLastSafeDepartureAt(),

                                        Boolean.TRUE.equals(
                                                result.getCanReturn()
                                        )
                                )
                        )
                        .toList();


        return new CandidateRankingResponseDTO(

                evaluation.getCandidateId(),

                candidate.getPlaceName(),

                candidate.getAddress(),

                evaluation.getRecommendationRank(),

                evaluation.getRuleScore(),

                evaluation.getAllReturnable(),

                evaluation.getDeadlineAt(),

                evaluation.getGoldenMarginMinutes(),

                evaluation.getAverageReturnMinutes(),

                evaluation.getFairnessGapMinutes(),

                evaluation.getFairnessScore(),

                fairPlace,

                participantResults
        );
    }
}