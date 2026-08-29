package com.meetback.dev.controller;

import com.meetback.dev.domain.MeetingEventType;
import com.meetback.dev.domain.MeetingType;
import com.meetback.dev.dto.*;
import com.meetback.dev.realtime.event.RealtimeEvent;
import com.meetback.dev.realtime.publisher.RealtimeEventPublisher;
import com.meetback.dev.security.AuthenticatedUser;
import com.meetback.dev.service.ChatService;
import com.meetback.dev.service.MeetingService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;


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
    private final ChatService chatService;
    private final RealtimeEventPublisher realtimeEventPublisher;

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

        MeetingCreateResponse response =
                meetingService.createMeeting(
                        user.userId(),
                        request
                );

        MeetingType meetingType =
                request.getMeetingType() != null
                    ? request.getMeetingType()
                    : MeetingType.FRIEND;

        /*
         * 번개방 생성 시 홈의 번개방 목록 갱신 이벤트 발행
         */
        if(meetingType == MeetingType.QUICK_VOTE)
        {
            String eventType =
                    MeetingEventType.QUICK_MEETING_LIST_CHANGED.name();

            realtimeEventPublisher.publish(
                    RealtimeEvent.quickLobbyBroadcast(
                            eventType,
                            response.getMeetingId(),
                            user.userId(),
                            Map.of(
                                    "messageType", "EVENT",
                                    "eventType", eventType,
                                    "meetingId", response.getMeetingId()
                            )
                    )
            );
        }

        return response;

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
            String eventType = MeetingEventType.PARTICIPANT_JOINED.name();

            ChatMessageResponse event =
                    chatService.saveSystemMessage(
                            response.meetingId(),
                            user.userId(),
                            "PARTICIPANT_JOINED",
                            "모임에 참가했습니다."
                    );

            realtimeEventPublisher.publish(
                    RealtimeEvent.meetingBroadcast(
                            eventType,
                            response.meetingId(),
                            user.userId(),
                            event
                    )
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

        String eventType = MeetingEventType.MEETING_CONFIRMED.name();

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
                        eventType,
                        eventType,
                        "최종 장소가 확정되었습니다."
                );


        // =========================================================
        // 3. 모든 참가자에게 Broadcast
        // =========================================================

        if (notice != null) {
            realtimeEventPublisher.publish(
                    RealtimeEvent.meetingBroadcast(
                            eventType,
                            meetingId,
                            user.userId(),
                            notice
                    )
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

        /*
         * 먼저 DB 상태를 INPUT_OPEN에서 VOTING으로 변경합니다.
         *
         * 상태 변경이 실패하면 투표 시작 메시지도 발생하지 않습니다.
         */
        meetingService.startVoting(
                meetingId,
                user.userId()
        );

        String eventType =
                MeetingEventType.VOTING_STARTED.name();

        /*
         * 투표 시작 공지는 모임당 한 번만 저장합니다.
         */
        ChatMessageResponse notice =
                chatService.saveSystemMessageOnce(
                        meetingId,
                        user.userId(),
                        eventType,
                        eventType,
                        "장소 투표가 시작되었습니다."
                );

        if (notice != null)
        {
            realtimeEventPublisher.publish(
                    RealtimeEvent.meetingBroadcast(
                            eventType,
                            meetingId,
                            user.userId(),
                            notice
                    )
            );
        }
    }

    @GetMapping("/my")
    public List<MyMeetingResponse> getMyMeetings(
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return meetingService.getMyMeetings(
                user.userId()
        );
    }

    @GetMapping("/quick")
    public List<QuickMeetingResponse> getQuickVoteMeetings(
            @RequestParam(
                    required = false,
                    defaultValue = ""
            )
            String keyword
    ) {

        return meetingService.getQuickVoteMeetings(
                keyword
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
