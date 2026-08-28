package com.meetback.dev.controller;

import com.meetback.dev.dto.*;
import com.meetback.dev.security.AuthenticatedUser;
import com.meetback.dev.service.ChatService;
import com.meetback.dev.service.MeetingService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;


// ============================================================
// [TEMP-BKW-AUTH]
// WebSocket/JWT 개발용 임시 인증 Principal.
// 범석 Security 최종 코드 병합 시
// AuthenticatedUser → 범석의 최종 인증 Principal 타입으로 교체.
// Service 계층은 Long userId를 사용하므로 수정할 필요 없음.
// ============================================================
@RestController
@RequestMapping("/meetings")
@RequiredArgsConstructor
public class MeetingController {

    private final MeetingService meetingService;
    private final SimpMessagingTemplate messagingTemplate;
    private final ChatService chatService;

    /*
     * ============================================================
     * [TEMP-BKW-AUTH]
     * 실제 로그인 JWT에서 userId를 얻어서 모임 생성자(host)로 사용.
     * 범석 Security 병합 시 Principal 타입만 교체.
     * ============================================================
     */

    @PostMapping
    public MeetingCreateResponse createMeeting(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestBody MeetingCreateRequest request
            ){

        System.out.println(
                "[MeetingController] user = " + user
        );

        return meetingService.createMeeting(
                user.userId(),
                request
        );

    }

    /*
     * ============================================================
     * [TEMP-BKW-AUTH]
     * 참가 요청의 userId를 클라이언트에게 받지 않고
     * 실제 로그인 JWT에서 추출.
     * ============================================================
     */

    @PostMapping("/join")
    public MeetingJoinResponse joinMeeting(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestBody MeetingJoinRequest request
            )
    {

        MeetingJoinResponse response = meetingService.joinMeeting(
                user.userId(),
                request
        );

        /*
         * 진짜 새 참가자일 때만
         * 입장 이벤트 발생
         */

        if(response.newlyJoined())
        {
            ChatMessageResponse event =
                    chatService.saveSystemMessage(
                            response.meetingId(),
                            user.userId(),
                            "PARTICIPANT_JOINED",
                            "모임에 참가했습니다."
                    );


            messagingTemplate.convertAndSend(
                    "/topic/meetings/"
                    + response.meetingId()
                    + "/chat",

                    event
            );
        }

        return response;

    }

    /*
     * ============================================================
     * [TEMP-BKW-AUTH]
     * hostUserId RequestParam 제거.
     * 실제 로그인 JWT 사용자로 방장 여부를 검증.
     * ============================================================
     */

    @PutMapping("/{meetingId}/final-candidate")
    public void confirmFinalCandidate(

            @PathVariable
            Long meetingId,

            @AuthenticationPrincipal
            AuthenticatedUser user,

            @RequestBody
            FinalCandidateRequest request

    ) {

        // =========================================================
        // 1. 최종 후보 DB 확정
        // =========================================================

        meetingService.confirmFinalCandidate(
                meetingId,
                user.userId(),
                request
        );


        // =========================================================
        // 2. 최종 확정 공지
        // =========================================================

        ChatMessageResponse notice =
                chatService.saveSystemMessageOnce(
                        meetingId,
                        user.userId(),
                        "MEETING_CONFIRMED",
                        "MEETING_CONFIRMED",
                        "최종 장소가 확정되었습니다."
                );


        // =========================================================
        // 3. 모든 참가자에게 Broadcast
        // =========================================================

        if (notice != null) {

            messagingTemplate.convertAndSend(

                    "/topic/meetings/"
                            + meetingId
                            + "/chat",

                    notice
            );
        }
    }

    /*
     * ============================================================
     * [TEMP-BKW-AUTH]
     * hostUserId RequestParam 제거.
     * 실제 로그인 JWT 사용자로 방장 여부를 검증.
     * ============================================================
     */
    @PutMapping("/{meetingId}/voting")
    public void startVoting(
            @PathVariable Long meetingId,
            @AuthenticationPrincipal AuthenticatedUser user
            ) {

        ChatMessageResponse notice =
                chatService.saveSystemMessageOnce(
                        meetingId,
                        user.userId(),
                        "VOTING_STARTED",
                        "VOTING_STARTED",
                        "장소 투표가 시작되었습니다."
                );

        if(notice != null)
        {
            messagingTemplate.convertAndSend(
                    "/topic/meetings/"
                        + meetingId
                        + "/chat",
                    notice
            );
        }
        // DB 상태
        // INPUT_OPEN -> VOTING
        meetingService.startVoting(
                meetingId,
                user.userId()
        );

        ChatMessageResponse event =
                chatService.saveSystemMessage(
                        meetingId,
                        user.userId(),
                        "VOTING_STARTED",
                        "장소 투표를 시작합니다."
                );

        messagingTemplate.convertAndSend(
                "/topic/meetings/"
                        + meetingId
                        + "/chat",

                event
        );
    }

    @GetMapping("/my")
    public List<MyMeetingResponse> getMyMeetings(
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return meetingService.getMyMeetings(
                user.userId()
        );
    }

    @GetMapping("/{meetingId}")
    public MeetingRoomResponse getMeeting(
            @PathVariable Long meetingId,
            @AuthenticationPrincipal
            AuthenticatedUser user
    )
    {
        return meetingService.getMeetingRoom(
                meetingId,
                user.userId()
        );
    }
}
