package com.meetback.dev.service;

import com.meetback.dev.domain.CandidateReturnResult;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RuleEngineService {

    public double calculateRuleScore(
            boolean allReturnable,
            int fairnessScore,
            double averageReturnMinutes,
            List<CandidateReturnResult> results
    ) {

        /*
         * 1. 전원 귀가 불가능한 후보
         * 추천 우선순위에서 제외하기 위해 0점 처리
         */
        if (!allReturnable) {
            return 0.0;
        }


        /*
         * 2. 평균 귀가시간 점수
         * 최대 50점
         *
         * 30분 이하  -> 50점
         * 45분 이하  -> 40점
         * 60분 이하  -> 30점
         * 75분 이하  -> 20점
         * 75분 초과  -> 10점
         */
        int averageReturnScore =
                calculateAverageReturnScore(
                        averageReturnMinutes
                );


        /*
         * 3. Fairness 점수
         * CandidateEvaluationService에서
         * 이미 10 ~ 50점으로 계산된 값 사용
         */
        int fairnessRuleScore =
                fairnessScore;


        /*
         * 4. 참가자 전체 환승 횟수 계산
         */
        int totalTransferCount =
                results.stream()
                        .map(CandidateReturnResult::getTransferCount)
                        .filter(count -> count != null)
                        .mapToInt(Integer::intValue)
                        .sum();


        /*
         * 환승 1회당 5점 감점
         */
        double transferPenalty =
                totalTransferCount * 5.0;


        /*
         * 5. 최종 Rule Score
         *
         * 평균 귀가시간 최대 50점
         * + Fairness 최대 50점
         * - 전체 환승 횟수 × 5점
         */
        double ruleScore =
                averageReturnScore
                        + fairnessRuleScore
                        - transferPenalty;


        /*
         * 최종 점수는 0 ~ 100 사이로 제한
         */
        return Math.max(
                0.0,
                Math.min(ruleScore, 100.0)
        );
    }


    /*
     * 평균 귀가시간을 50점 만점으로 변환
     */
    private int calculateAverageReturnScore(
            double averageReturnMinutes
    ) {

        if (averageReturnMinutes <= 30) {
            return 50;
        }

        if (averageReturnMinutes <= 45) {
            return 40;
        }

        if (averageReturnMinutes <= 60) {
            return 30;
        }

        if (averageReturnMinutes <= 75) {
            return 20;
        }

        return 10;
    }
}