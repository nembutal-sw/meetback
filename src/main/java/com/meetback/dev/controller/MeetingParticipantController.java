package com.meetback.dev.controller;

import com.meetback.dev.domain.MeetingParticipant;
import com.meetback.dev.dto.CurrentParticipantResponse;
import com.meetback.dev.dto.ParticipantLocationRequestDTO;
import com.meetback.dev.security.AuthenticatedUser;
import com.meetback.dev.service.MeetingParticipantService;
import com.meetback.dev.service.ParticipantService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/participants")
public class MeetingParticipantController {

    private final MeetingParticipantService meetingParticipantService;
    private final ParticipantService participantService;
    private final SimpMessagingTemplate messagingTemplate;


    @GetMapping("/{participantId}")
    public MeetingParticipant findById(
            @PathVariable Long participantId,

            @AuthenticationPrincipal
            AuthenticatedUser user
    )
    {
        return meetingParticipantService.findOwnedById(
                participantId,
                user.userId()
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
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestBody ParticipantLocationRequestDTO request
    ) {

        meetingParticipantService.updateLocation(
                participantId,
                user.userId(),
                request
        );


        MeetingParticipant participant =
                meetingParticipantService.findOwnedById(
                        participantId,
                        user.userId()
                );


        messagingTemplate.convertAndSend(
                "/topic/meetings/"
                        + participant.getMeetingId()
                        + "/chat",

                (Object) Map.of(
                        "messageType", "SYSTEM",
                        "eventType", "LOCATION_SUBMITTED",
                        "userId", participant.getUserId(),
                        "content",
                        "참가자 "
                                + participant.getUserId()
                                + "님이 장소 입력을 완료했습니다."
                )
        );


        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{participantId}/submit")
    public ResponseEntity<Void> submitInput(
            @PathVariable Long participantId,

            @AuthenticationPrincipal
            AuthenticatedUser user
    )
    {
        meetingParticipantService.submitInput(
                participantId,
                user.userId()
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
            @PathVariable Long participantId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {

        meetingParticipantService.startEdit(
                participantId,
                user.userId()
        );

        return ResponseEntity.noContent().build();
    }

   /*
    * 장소 수정 취소
    *
    * DRAFT -> SUBMITTED
    */
   @PutMapping("/{participantId}/edit/cancel")
   public ResponseEntity<Void> cancelEdit(
           @PathVariable Long participantId,

           @AuthenticationPrincipal
           AuthenticatedUser user
   )
   {
       meetingParticipantService.cancelEdit(
               participantId,
               user.userId()
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
            @PathVariable Long meetingId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {

        participantService.getCurrentParticipant(
                meetingId,
                user.userId()
        );

        return meetingParticipantService.findByMeetingId(
                meetingId
        );
    }

    @GetMapping("/meetings/{meetingId}/participants/me")
    public CurrentParticipantResponse getCurrentParticipant(

            @PathVariable Long meetingId,

            @AuthenticationPrincipal
            AuthenticatedUser user

    ) {

        return participantService.getCurrentParticipant(
                meetingId,
                user.userId()
        );
    }
}