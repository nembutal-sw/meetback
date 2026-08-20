package com.meetback.dev.controller;

import com.meetback.dev.domain.CandidateReturnResult;
import com.meetback.dev.service.CalculationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/calculations")
public class CalculationController {

    private final CalculationService calculationService;

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

    @PostMapping("/candidate")
    public List<CandidateReturnResult> calculateCandidate(
            @RequestParam Long candidateId
    ) {
        return calculationService.calculateCandidate(candidateId);
    }
}