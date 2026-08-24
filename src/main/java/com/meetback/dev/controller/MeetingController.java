package com.meetback.dev.controller;

import com.meetback.dev.dto.FinalCandidateRequest;
import com.meetback.dev.dto.MeetingCreateRequest;
import com.meetback.dev.dto.MeetingCreateResponse;
import com.meetback.dev.dto.MeetingJoinRequest;
import com.meetback.dev.security.AuthenticatedUser;
import com.meetback.dev.service.MeetingService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;


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
    public Long joinMeeting(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestBody MeetingJoinRequest request
            )
    {
        return meetingService.joinMeeting(
                user.userId(),
                request
        );
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
            @PathVariable Long meetingId,

            // [TEMP-BKW-AUTH]
            // 범석 Security 코드 병합 시 최종 Principal 타입으로 교체
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestBody FinalCandidateRequest request
            )
    {
        meetingService.confirmFinalCandidate(
                meetingId,
                user.userId(),
                request
        );
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

        meetingService.startVoting(
                meetingId,
                user.userId()
        );
    }
}
