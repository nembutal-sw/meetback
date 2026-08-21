package com.meetback.dev.controller;

import com.meetback.dev.dto.FinalCandidateRequest;
import com.meetback.dev.dto.MeetingCreateRequest;
import com.meetback.dev.dto.MeetingCreateResponse;
import com.meetback.dev.dto.MeetingJoinRequest;
import com.meetback.dev.service.MeetingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/meetings")
@RequiredArgsConstructor
public class MeetingController {

    private final MeetingService meetingService;

    @PostMapping
    public MeetingCreateResponse createMeeting(
            @RequestParam Long hostUserId,
            @RequestBody MeetingCreateRequest request
            ){

        return meetingService.createMeeting(
                hostUserId,
                request
        );

    }

    @PostMapping("/join")
    public Long joinMeeting(
            @RequestParam Long userId,
            @RequestBody MeetingJoinRequest request
            )
    {
        return meetingService.joinMeeting(userId,request);
    }

    @PutMapping("/{meetingId}/final-candidate")
    public void confirmFinalCandidate(
            @PathVariable Long meetingId,
            @RequestParam Long hostUserId,
            @RequestBody FinalCandidateRequest request
            )
    {
        meetingService.confirmFinalCandidate(
                meetingId,
                hostUserId,
                request
        );
    }

    @PutMapping("/{meetingId}/voting")
    public void startVoting(
            @PathVariable Long meetingId,
            @RequestParam Long hostUserId
    ) {

        meetingService.startVoting(
                meetingId,
                hostUserId
        );
    }

}
