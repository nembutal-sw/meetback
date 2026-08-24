package com.meetback.dev.controller;

import com.meetback.dev.domain.MeetingParticipant;
import com.meetback.dev.dto.CurrentParticipantResponse;
import com.meetback.dev.dto.ParticipantLocationRequestDTO;
import com.meetback.dev.security.dev.DevAuthenticatedUser;
import com.meetback.dev.service.MeetingParticipantService;
import com.meetback.dev.service.ParticipantService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/participants")
public class MeetingParticipantController {

    private final MeetingParticipantService meetingParticipantService;
    private final ParticipantService participantService;


    @GetMapping("/{participantId}")
    public MeetingParticipant findById(
            @PathVariable Long participantId
    ) {

        return meetingParticipantService.findById(
                participantId
        );
    }


    /*
     * 출발지 + 귀가지 등록 / 수정 완료
     *
     * 저장 완료 후
     * DRAFT -> SUBMITTED
     */
    @PutMapping("/{participantId}/location")
    public ResponseEntity<Void> updateLocation(
            @PathVariable Long participantId,
            @RequestBody ParticipantLocationRequestDTO request
    ) {

        meetingParticipantService.updateLocation(
                participantId,
                request
        );

        return ResponseEntity.noContent().build();
    }


    /*
     * 수정 시작
     *
     * SUBMITTED -> DRAFT
     */
    @PutMapping("/{participantId}/edit")
    public ResponseEntity<Void> startEdit(
            @PathVariable Long participantId
    ) {

        meetingParticipantService.startEdit(
                participantId
        );

        return ResponseEntity.noContent().build();
    }


    /*
     * 모임 참가자 전원이
     * 장소 등록을 완료했는지 확인
     */
    @GetMapping("/meeting/{meetingId}/submitted")
    public boolean isAllSubmitted(
            @PathVariable Long meetingId
    ) {

        return meetingParticipantService.isAllSubmitted(
                meetingId
        );
    }

    /*
     * 모임 참가자 전체 조회
     */
    @GetMapping("/meeting/{meetingId}")
    public List<MeetingParticipant> findByMeetingId(
            @PathVariable Long meetingId
    ) {

        return meetingParticipantService.findByMeetingId(
                meetingId
        );
    }

    @GetMapping("/meetings/{meetingId}/participants/me")
    public CurrentParticipantResponse getCurrentParticipant(

            @PathVariable Long meetingId,

            @AuthenticationPrincipal
            DevAuthenticatedUser user

    ) {

        return participantService.getCurrentParticipant(
                meetingId,
                user.userId()
        );
    }
}