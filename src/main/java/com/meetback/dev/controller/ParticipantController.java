package com.meetback.dev.controller;

import com.meetback.dev.dto.LocationSubmitRequest;
import com.meetback.dev.service.ParticipantService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/participants")
@RequiredArgsConstructor
public class ParticipantController {

    private final ParticipantService participantService;

    @PutMapping("/{participantId}/location")
    public void submitLocation(
            @PathVariable Long participantId,
            @RequestBody LocationSubmitRequest request
            ){

        participantService.submitLocation(
                participantId,
                request
        );

    }

    @GetMapping("/meeting/{meetingId}/submitted")
    public boolean isAllSubmitted(
            @PathVariable Long meetingId
    )
    {
        return participantService.isAllSubmitted(meetingId);
    }

}
