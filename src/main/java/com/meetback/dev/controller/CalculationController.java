package com.meetback.dev.controller;

import com.meetback.dev.domain.CandidateEvaluation;
import com.meetback.dev.domain.CandidateReturnResult;
import com.meetback.dev.dto.CandidateRankingResponseDTO;
import com.meetback.dev.service.CalculationService;
import com.meetback.dev.service.CandidateEvaluationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/calculations")
public class CalculationController {

    private final CalculationService calculationService;
    private final CandidateEvaluationService candidateEvaluationService;


    /*
     * 참가자 1명 × 후보 1개
     */
    @PostMapping("/return")
    public CandidateReturnResult calculateReturn(
            @RequestParam Long participantId,
            @RequestParam Long candidateId
    ) {

        return calculationService.calculateReturn(
                participantId,
                candidateId
        );
    }


    /*
     * 후보 1개 × 참가자 전원
     */
    @PostMapping("/candidate")
    public List<CandidateReturnResult> calculateCandidate(
            @RequestParam Long candidateId
    ) {

        return calculationService.calculateCandidate(
                candidateId
        );
    }


    /*
     * 모임 전체 후보 계산
     */
    @PostMapping("/meeting")
    public List<CandidateEvaluation> calculateMeeting(
            @RequestParam Long meetingId
    ) {

        return calculationService.calculateMeeting(
                meetingId
        );
    }


    /*
     * 추천 1위 조회
     */
    @GetMapping("/meeting/recommendation")
    public CandidateRankingResponseDTO getTopRecommendation(
            @RequestParam Long meetingId
    ) {

        return candidateEvaluationService
                .getTopRecommendation(
                        meetingId
                );
    }


    /*
     * 후보 전체 순위 조회
     */
    @GetMapping("/meeting/ranking")
    public List<CandidateRankingResponseDTO> getRanking(
            @RequestParam Long meetingId
    ) {

        return candidateEvaluationService
                .getRanking(
                        meetingId
                );
    }
}