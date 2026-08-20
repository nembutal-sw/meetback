package com.meetback.dev.controller;

import com.meetback.dev.dto.CandidateCreateRequest;
import com.meetback.dev.service.CandidateService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/meetings")
@RequiredArgsConstructor
public class CandidateController {

    private final CandidateService candidateService;

    @PostMapping("/{meetingId}/candidates")
    public Long createCandidate(
            @PathVariable Long meetingId,
            @RequestParam Long participantId,
            @RequestBody CandidateCreateRequest request
            )
    {
        return candidateService.createCandidate(
                meetingId,
                participantId,
                request
        );
    }

}
