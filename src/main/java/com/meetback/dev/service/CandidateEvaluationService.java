package com.meetback.dev.service;

import com.meetback.dev.domain.CandidateEvaluation;
import com.meetback.dev.domain.CandidateReturnResult;
import com.meetback.dev.domain.Meeting;
import com.meetback.dev.repository.CandidateEvaluationMapper;
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

        CandidateEvaluation evaluation =
                new CandidateEvaluation();

        evaluation.setCandidateId(candidateId);

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

        // RuleEngine은 다음 단계에서 계산
        evaluation.setRuleScore(null);

        // 전체 후보 계산 후 순위 결정
        evaluation.setRecommendationRank(null);


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
}