package com.meetback.dev.controller;

import com.meetback.dev.domain.MeetingEventType;
import com.meetback.dev.domain.MeetingParticipant;
import com.meetback.dev.domain.ParticipantKickResult;
import com.meetback.dev.domain.ParticipantLeaveResult;
import com.meetback.dev.dto.*;
import com.meetback.dev.realtime.event.RealtimeEvent;
import com.meetback.dev.realtime.publisher.RealtimeEventPublisher;
import com.meetback.dev.security.AuthenticatedUser;
import com.meetback.dev.service.ChatService;
import com.meetback.dev.service.MeetingParticipantService;
import com.meetback.dev.service.ParticipantService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
    private final ChatService chatService;
    private final RealtimeEventPublisher realtimeEventPublisher;


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

        String locationSubmittedEventType =
                MeetingEventType.ALL_LOCATIONS_SUBMITTED.name();

        ChatMessageResponse event =
                chatService.saveSystemMessage(
                        meetingId,
                        user.userId(),
                        locationSubmittedEventType,
                        "장소 입력을 완료했습니다."
                );


        realtimeEventPublisher.publish(
                RealtimeEvent.meetingBroadcast(
                        locationSubmittedEventType,
                        meetingId,
                        user.userId(),
                        event
                )
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

            String allSubmittedEventType =
                    MeetingEventType.ALL_LOCATIONS_SUBMITTED.name();

            /*
             * meeting_id + event_key UNIQUE 제약으로
             * 모임마다 한 번만 저장됩니다.
             */
            ChatMessageResponse notice =
                    chatService.saveSystemMessageOnce(
                            meetingId,
                            user.userId(),
                            allSubmittedEventType,
                            allSubmittedEventType,
                            "모든 참가자의 장소 입력이 완료되었습니다."
                    );

            /*
             * 이미 저장된 공지라면 notice가 null이므로
             * 실시간 이벤트도 중복 발행하지 않습니다.
             */
            if (notice != null)
            {
                realtimeEventPublisher.publish(
                        RealtimeEvent.meetingBroadcast(
                                allSubmittedEventType,
                                meetingId,
                                user.userId(),
                                notice
                        )
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

        String eventType = MeetingEventType.LOCATION_EDITING.name();

        ChatMessageResponse event =
                chatService.saveSystemMessage(
                        participant.getMeetingId(),
                        user.userId(),
                        eventType,
                        "장소를 수정 중입니다."
                );

        realtimeEventPublisher.publish(
                RealtimeEvent.meetingBroadcast(
                        eventType,
                        participant.getMeetingId(),
                        user.userId(),
                        event
                )
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

       String eventType = MeetingEventType.LOCATION_EDIT_CANCELED.name();

       ChatMessageResponse event =
               chatService.saveSystemMessage(
                       participant.getMeetingId(),
                       user.userId(),
                       "LOCATION_EDIT_CANCELED",
                       "장소 수정을 취소했습니다."
               );

       realtimeEventPublisher.publish(
               RealtimeEvent.meetingBroadcast(
                       eventType,
                       participant.getMeetingId(),
                       user.userId(),
                       event
               )
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

    @GetMapping("/meeting/{meetingId}/kicked")
    public List<ParticipantRoomResponse> findKickedParticipants(
            @PathVariable Long meetingId,
            @AuthenticationPrincipal AuthenticatedUser user
    )
    {
        return meetingParticipantService.
                findKickedRoomParticipants(
                        meetingId,
                        user.userId()
                );
    }

    @DeleteMapping("/{participantId}/kick")
    public ResponseEntity<Void> kickParticipant(
            @PathVariable Long participantId,
            @AuthenticationPrincipal AuthenticatedUser user
    )
    {
        ParticipantKickResult kicked =
                meetingParticipantService.kickParticipant(
                        participantId,
                        user.userId()
                );

        String nickname =
                kicked.nickname() == null
                    ? "참가자"
                    : kicked.nickname();

        String eventType = MeetingEventType.PARTICIPANT_KICKED.name();

        /*
         * 1. DB에 저장되는 채팅 SYSTEM 메시지
         */
        ChatMessageResponse systemMessage =
                chatService.saveSystemMessage(
                        kicked.meetingId(),
                        user.userId(),
                        eventType,
                        nickname + "님이 강퇴되었습니다."
                );

        /*
         * 1. 채팅창에 남는 영구 SYSTEM 메시지
         */
        realtimeEventPublisher.publish(
                RealtimeEvent.meetingBroadcast(
                        eventType,
                        kicked.meetingId(),
                        user.userId(),
                        kicked.userId(),
                        kicked.participantId(),
                        systemMessage
                )
        );

        /*
         * 2. 참가자 목록 갱신 및 강퇴 대상 연결 종료 이벤트
         */
        Map<String, Object> kickPayload =
                Map.of(
                        "messageType", "EVENT",
                        "eventType", eventType,
                        "meetingId", kicked.meetingId(),
                        "participantId", kicked.participantId(),
                        "userId", kicked.userId(),
                        "nickname", nickname
                );

        realtimeEventPublisher.publish(
                RealtimeEvent.meetingBroadcastAndDisconnectTarget(
                        eventType,
                        kicked.meetingId(),
                        user.userId(),
                        kicked.userId(),
                        kicked.participantId(),
                        kickPayload
                )
        );

        String lobbyEventType =
                MeetingEventType
                        .QUICK_MEETING_LIST_CHANGED
                        .name();

        realtimeEventPublisher.publish(
                RealtimeEvent.quickLobbyBroadcast(
                        lobbyEventType,
                        kicked.meetingId(),
                        user.userId(),
                        Map.of(
                                "messageType", "EVENT",
                                "eventType", lobbyEventType,
                                "meetingId", kicked.meetingId()
                        )
                )
        );

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{participantId}/kick/cancel")
    public ResponseEntity<Void> cancelKick(
            @PathVariable Long participantId,
            @AuthenticationPrincipal AuthenticatedUser user
    )
    {
        ParticipantKickResult canceled =
                meetingParticipantService.cancelKick(
                        participantId,
                        user.userId()
                );

        String nickname = canceled.nickname() == null
                ? "참가자"
                : canceled.nickname();

        String eventType = MeetingEventType.PARTICIPANT_KICK_CANCELED.name();

        ChatMessageResponse systemMessage =
                chatService.saveSystemMessage(
                        canceled.meetingId(),
                        user.userId(),
                        eventType,
                        nickname + "님의 강퇴가 취소되었습니다."
                );

        /*
         * 채팅창에 표시할 영구 SYSTEM 메세지
         */
        realtimeEventPublisher.publish(
                RealtimeEvent.meetingBroadcast(
                        eventType,
                        canceled.meetingId(),
                        user.userId(),
                        canceled.userId(),
                        canceled.participantId(),
                        systemMessage
                )
        );

        /*
         * 참가자 목록을 갱신하기 위한 일회성 EVENT
         */
        Map<String,Object> payload =
                Map.of(
                        "messageType","EVENT",
                        "eventType",eventType,
                        "meetingId", canceled.meetingId(),
                        "participantId", canceled.participantId(),
                        "userId", canceled.userId(),
                        "nickname", nickname
                );

        realtimeEventPublisher.publish(
                RealtimeEvent.meetingBroadcast(
                        eventType,
                        canceled.meetingId(),
                        user.userId(),
                        canceled.userId(),
                        canceled.participantId(),
                        payload
                )
        );

        String lobbyEventType =
                MeetingEventType
                        .QUICK_MEETING_LIST_CHANGED
                        .name();

        realtimeEventPublisher.publish(
                RealtimeEvent.quickLobbyBroadcast(
                        lobbyEventType,
                        canceled.meetingId(),
                        user.userId(),
                        Map.of(
                                "messageType", "EVENT",
                                "eventType", lobbyEventType,
                                "meetingId", canceled.meetingId()
                        )
                )
        );

        return ResponseEntity.noContent().build();
    }

    /*
     * QUICK_VOTE 방 나가기
     *
     * 현재는 INPUT_OPEN 단계에서만 지원한다.
     */
    @DeleteMapping("/{participantId}/leave")
    public ResponseEntity<Void> leaveQuickVoteMeeting(
            @PathVariable Long participantId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {

        ParticipantLeaveResult left =
                meetingParticipantService
                        .leaveQuickVoteMeeting(
                                participantId,
                                user.userId()
                        );


        String nickname =
                left.nickname() == null
                        ? "참가자"
                        : left.nickname();

        String eventType = MeetingEventType.PARTICIPANT_LEFT.name();

        /*
         * 참가자 목록 갱신 및
         * 다른 탭의 퇴장 처리를 위한 실시간 EVENT
         */
        Map<String, Object> payload =
                Map.of(
                        "messageType", "EVENT",
                        "eventType", eventType,
                        "meetingId", left.meetingId(),
                        "participantId", left.participantId(),
                        "userId", left.userId(),
                        "nickname", nickname
                );

        realtimeEventPublisher.publish(
                RealtimeEvent.meetingBroadcast(
                        eventType,
                        left.meetingId(),
                        user.userId(),
                        left.userId(),
                        left.participantId(),
                        payload
                )
        );

        /*
         * 홈의 공개 번개방 목록 인원 갱신
         */
        String lobbyEventType =
                MeetingEventType
                        .QUICK_MEETING_LIST_CHANGED
                        .name();

        realtimeEventPublisher.publish(
                RealtimeEvent.quickLobbyBroadcast(
                        lobbyEventType,
                        left.meetingId(),
                        user.userId(),
                        Map.of(
                                "messageType", "EVENT",
                                "eventType", lobbyEventType,
                                "meetingId", left.meetingId()
                        )
                )
        );


        return ResponseEntity
                .noContent()
                .build();
    }

}