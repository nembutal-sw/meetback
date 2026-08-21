package com.meetback.dev.controller;

import com.meetback.dev.service.ParticipantService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/participants")
@RequiredArgsConstructor
public class ParticipantController {

    private final ParticipantService participantService;

    @GetMapping("/meeting/{meetingId}/submitted")
    public boolean isAllSubmitted(
            @PathVariable Long meetingId
    )
    {
        return participantService.isAllSubmitted(meetingId);
    }

}
