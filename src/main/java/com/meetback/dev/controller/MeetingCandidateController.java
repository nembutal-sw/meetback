package com.meetback.dev.controller;

import com.meetback.dev.domain.MeetingCandidate;
import com.meetback.dev.dto.CandidateRequestDTO;
import com.meetback.dev.security.AuthenticatedUser;
import com.meetback.dev.service.MeetingCandidateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/participants")
public class MeetingCandidateController {

    private final MeetingCandidateService meetingCandidateService;

    @PostMapping("/{participantId}/candidate")
    public ResponseEntity<Void> saveCandidate(
            @PathVariable Long participantId,

            @AuthenticationPrincipal
            AuthenticatedUser user,

            @RequestBody CandidateRequestDTO request
    )
    {
        meetingCandidateService.saveCandidate(
                participantId,
                user.userId(),
                request.candidateQuery()
        );


        return ResponseEntity.noContent().build();
    }

    @GetMapping("/meeting/{meetingId}/candidates")
    public List<MeetingCandidate> findCandidates(
            @PathVariable Long meetingId) {
        return meetingCandidateService.findByMeetingId(meetingId);
    }
}
