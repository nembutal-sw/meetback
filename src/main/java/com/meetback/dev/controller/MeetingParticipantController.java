package com.meetback.dev.controller;


import com.meetback.dev.domain.MeetingParticipant;
import com.meetback.dev.dto.ParticipantLocationRequestDTO;
import com.meetback.dev.service.MeetingParticipantService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/participants")
public class MeetingParticipantController {
    private final MeetingParticipantService meetingParticipantService;

    @GetMapping("/{participantId}")
    public MeetingParticipant findById(
            @PathVariable Long participantId) {

        return meetingParticipantService.findById(participantId);
    }

    @PutMapping("/{participantId}/location")
    public ResponseEntity<Void> updateLocation(
            @PathVariable Long participantId,
            @RequestBody ParticipantLocationRequestDTO request) {

        meetingParticipantService.updateLocation(participantId, request);

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/meeting/{meetingId}/complete")
    public boolean isAllComplete(
            @PathVariable Long meetingId) {

        return meetingParticipantService.isAllComplete(meetingId);
    }

    @GetMapping("/meeting/{meetingId}")
    public List<MeetingParticipant> findByMeetingId(
            @PathVariable Long meetingId) {

        return meetingParticipantService.findByMeetingId(meetingId);
    }
}
