package com.meetback.dev.controller;

import com.meetback.dev.dto.CandidateVoteResult;
import com.meetback.dev.dto.VoteRequest;
import com.meetback.dev.security.AuthenticatedUser;
import com.meetback.dev.service.VoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestBody VoteRequest request
    ) {
        voteService.vote(
                meetingId,
                user.userId(),
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
