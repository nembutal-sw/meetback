package com.meetback.dev.controller;

import com.meetback.dev.dto.CandidateVoteResult;
import com.meetback.dev.dto.VoteRequest;
import com.meetback.dev.service.VoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/meetings")
@RequiredArgsConstructor
public class VoteController {

    private final VoteService voteService;

    // 투표 / 재투표
    @PutMapping("/{meetingId}/votes")
    public void vote(
            @PathVariable Long meetingId,
            @RequestParam Long participantId,
            @RequestBody VoteRequest request
    ) {
        voteService.vote(
                meetingId,
                participantId,
                request
        );
    }

    // 후보별 득표수 조회
    @GetMapping("/{meetingId}/votes/results")
    public List<CandidateVoteResult> getVoteResults(
            @PathVariable Long meetingId
    ) {
        return voteService.getVoteResults(meetingId);
    }

}
