package com.meetback.dev.controller;

import com.meetback.dev.domain.MeetingParticipant;
import com.meetback.dev.dto.ChatMessageResponse;
import com.meetback.dev.dto.CurrentParticipantResponse;
import com.meetback.dev.dto.ParticipantLocationRequestDTO;
import com.meetback.dev.dto.ParticipantRoomResponse;
import com.meetback.dev.security.AuthenticatedUser;
import com.meetback.dev.service.ChatService;
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
    private final ChatService chatService;


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
        // =========================================================
        // 1. 출발지 / 귀가지 저장
        //
        // DRAFT -> SUBMITTED
        // =========================================================

        meetingParticipantService.updateLocation(
                participantId,
                user.userId(),
                request
        );


        // =========================================================
        // 2. 저장된 참가자 다시 조회
        //
        // meetingId를 사용하기 위해 조회
        // =========================================================

        MeetingParticipant participant =
                meetingParticipantService.findOwnedById(
                        participantId,
                        user.userId()
                );


        Long meetingId =
                participant.getMeetingId();


        // =========================================================
        // 3. 해당 참가자의 장소 입력 완료 이벤트
        //
        // 이 이벤트는 참가자마다 발생 가능
        //
        // 예:
        // A 장소 입력 완료
        // B 장소 입력 완료
        // C 장소 입력 완료
        // =========================================================

        ChatMessageResponse event =
                chatService.saveSystemMessage(
                        meetingId,
                        user.userId(),
                        "LOCATION_SUBMITTED",
                        "장소 입력을 완료했습니다."
                );


        messagingTemplate.convertAndSend(
                "/topic/meetings/"
                        + meetingId
                        + "/chat",

                event
        );


        // =========================================================
        // 4. ★ 모든 참가자가 장소 입력을 완료했는지 확인
        //
        // 방금 한 명의 상태가 SUBMITTED가 되었으므로
        // 여기서 전원 완료 여부를 다시 검사한다.
        // =========================================================

        boolean allSubmitted =
                meetingParticipantService
                        .isAllSubmitted(
                                meetingId
                        );


        // =========================================================
        // 5. 전원 입력 완료라면
        //    모임 전체 공지 발생
        // =========================================================

        if (allSubmitted) {

            /*
             * saveSystemMessageOnce()
             *
             * meeting_id + event_key UNIQUE 덕분에
             * 이 공지는 모임당 딱 한 번만 DB에 저장된다.
             */
            ChatMessageResponse notice =
                    chatService.saveSystemMessageOnce(

                            meetingId,

                            user.userId(),

                            "ALL_LOCATIONS_SUBMITTED",

                            "ALL_LOCATIONS_SUBMITTED",

                            "모든 참가자의 장소 입력이 완료되었습니다."
                    );


            /*
             * 이미 저장된 공지라면
             * notice == null
             *
             * 따라서 WebSocket Broadcast도 다시 하지 않는다.
             */
            if (notice != null) {

                messagingTemplate.convertAndSend(

                        "/topic/meetings/"
                                + meetingId
                                + "/chat",

                        notice
                );
            }
        }


        // =========================================================
        // 6. 완료
        // =========================================================

        return ResponseEntity
                .noContent()
                .build();
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
        // SUBMITTED -> DRAFT
        meetingParticipantService.startEdit(
                participantId,
                user.userId()
        );

        MeetingParticipant participant = meetingParticipantService.findOwnedById(
                participantId,
                user.userId()
        );

        /*
         * SYSTEM 메시지 DB 저장
         */

        ChatMessageResponse event =
                chatService.saveSystemMessage(
                        participant.getMeetingId(),
                        user.userId(),
                        "LOCATION_EDITING",
                        "장소를 수정 중입니다."
                );


        messagingTemplate.convertAndSend(
                "/topic/meetings/"
                        + participant.getMeetingId()
                        + "/chat",

                event
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

       MeetingParticipant participant = meetingParticipantService.findOwnedById(
               participantId,
               user.userId()
       );

       ChatMessageResponse event =
               chatService.saveSystemMessage(
                       participant.getMeetingId(),
                       user.userId(),
                       "LOCATION_EDIT_CANCELED",
                       "장소 수정을 취소했습니다."
               );

       messagingTemplate.convertAndSend(
               "/topic/meetings/"
                            + participant.getMeetingId()
                            + "/chat",
               event
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
    public List<ParticipantRoomResponse> findByMeetingId(
            @PathVariable Long meetingId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {

        /*
         * 해당 모임 참가자인지 검증
         */
        participantService.getCurrentParticipant(
                meetingId,
                user.userId()
        );

        return meetingParticipantService.findRoomParticipants(meetingId);

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