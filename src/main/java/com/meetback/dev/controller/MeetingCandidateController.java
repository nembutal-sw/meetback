package com.meetback.dev.controller;

import com.meetback.dev.domain.MeetingCandidate;
import com.meetback.dev.dto.CandidateRequestDTO;
import com.meetback.dev.service.MeetingCandidateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
            @RequestBody CandidateRequestDTO request) {
        meetingCandidateService.saveCandidate(
                participantId,
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
