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


    /*
     * 후보 장소의 Deadline 계산
     *
     * 막차를 이용하는 참가자들의
     * lastSafeDepartureAt 중 가장 빠른 시간
     *
     * 도보 귀가자는 lastSafeDepartureAt = null 이므로 제외
     *
     * 전원이 도보 귀가라면
     * 막차 제한이 없으므로 null 반환
     */
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
                .orElse(null);
    }


    /*
     * Golden Margin 계산
     *
     * Deadline - 희망 종료시간
     *
     * 전원이 도보 귀가라 Deadline이 없으면
     * Golden Margin은 0으로 처리
     */
    public int calculateGoldenMargin(
            LocalDateTime desiredEndAt,
            LocalDateTime deadlineAt
    ) {

        if (desiredEndAt == null) {
            throw new IllegalArgumentException(
                    "희망 종료시간이 없습니다."
            );
        }

        if (deadlineAt == null) {
            return 0;
        }

        LocalDateTime desired =
                desiredEndAt
                        .withSecond(0)
                        .withNano(0);

        LocalDateTime deadline =
                deadlineAt
                        .withSecond(0)
                        .withNano(0);

        return (int) Duration.between(
                desired,
                deadline
        ).toMinutes();
    }


    /*
     * 참가자 귀가시간 편차 계산
     *
     * 최대 귀가시간 - 최소 귀가시간
     */
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

        return maxReturnMinutes
                - minReturnMinutes;
    }


    /*
     * Fairness Score 계산
     */
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


    /*
     * 후보 평가 계산 및 저장
     */
    public CandidateEvaluation evaluateAndSave(
            Long candidateId,
            Long meetingId,
            Integer calculationVersion,
            List<CandidateReturnResult> results
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


        /*
         * 도보 귀가자는 Deadline 계산에서 제외
         *
         * 전원이 도보라면 deadline = null
         */
        LocalDateTime deadline =
                calculateDeadline(
                        results
                );


        int goldenMargin =
                calculateGoldenMargin(
                        meeting.getDesiredEndAt(),
                        deadline
                );


        int fairnessGap =
                calculateFairnessGap(
                        results
                );


        int fairnessScore =
                calculateFairnessScore(
                        fairnessGap
                );


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
                calculationVersion
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


    /*
     * 후보 순위 계산
     */
    public void rankCandidates(
            List<CandidateEvaluation> evaluations
    ) {

        if (evaluations == null
                || evaluations.isEmpty()) {

            throw new IllegalArgumentException(
                    "후보 평가 결과가 없습니다."
            );
        }


        evaluations.sort(
                (a, b) -> {

                    /*
                     * 1. Rule Score 높은 후보 우선
                     */
                    int scoreCompare =
                            Double.compare(
                                    b.getRuleScore(),
                                    a.getRuleScore()
                            );

                    if (scoreCompare != 0) {
                        return scoreCompare;
                    }


                    /*
                     * 2. 동점이면
                     * Golden Margin 큰 후보 우선
                     */
                    int marginCompare =
                            Integer.compare(
                                    b.getGoldenMarginMinutes(),
                                    a.getGoldenMarginMinutes()
                            );

                    if (marginCompare != 0) {
                        return marginCompare;
                    }


                    /*
                     * 3. 그래도 동점이면
                     * 평균 귀가시간 짧은 후보 우선
                     */
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
         * 이미 계산되어 DB에 저장된
         * 참가자별 귀가 결과 조회
         *
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

                                        result.getNickname(),

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

                participantResults
        );
    }
}