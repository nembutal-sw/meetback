package com.meetback.dev.controller;

import com.meetback.dev.dto.*;
import com.meetback.dev.security.AuthenticatedUser;
import com.meetback.dev.service.ChatService;
import com.meetback.dev.service.VoteService;

import lombok.RequiredArgsConstructor;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/meetings")
@RequiredArgsConstructor
public class VoteController {


    private final VoteService voteService;

    /*
     * WebSocket Broadcast
     */
    private final SimpMessagingTemplate messagingTemplate;
    private final ChatService chatService;



    // =============================================================
    // 투표 / 재투표
    // =============================================================

    @PutMapping("/{meetingId}/votes")
    public void vote(

            @PathVariable
            Long meetingId,

            @AuthenticationPrincipal
            AuthenticatedUser user,

            @RequestBody
            VoteRequest request

    ) {

        // =========================================================
        // 1. 후보 투표 / 재투표 / 기권
        // =========================================================

        voteService.vote(
                meetingId,
                user.userId(),
                request
        );


        // =========================================================
        // 2. 실시간 투표 화면 갱신
        //
        // DB에 저장할 공지는 아님.
        // =========================================================

        messagingTemplate.convertAndSend(

                "/topic/meetings/"
                        + meetingId
                        + "/chat",

                (Object) Map.of(
                        "messageType",
                        "EVENT",

                        "eventType",
                        "VOTE_UPDATED",

                        "userId",
                        user.userId()
                )
        );


        // =========================================================
        // 3. 전원 투표 완료 확인
        // =========================================================

        VoteProgressResponse progress =
                voteService.getVoteProgress(
                        meetingId,
                        user.userId()
                );


        if (!progress.allVoted()) {
            return;
        }


        // =========================================================
        // 4. 전원 투표 완료 공지
        //
        // 재투표를 해도 DB UNIQUE 때문에
        // 한 번만 발생한다.
        // =========================================================

        ChatMessageResponse notice =
                chatService.saveSystemMessageOnce(

                        meetingId,

                        user.userId(),

                        "ALL_VOTES_COMPLETED",

                        "ALL_VOTES_COMPLETED",

                        "모든 참가자의 투표가 완료되었습니다."
                );


        if (notice != null) {

            messagingTemplate.convertAndSend(

                    "/topic/meetings/"
                            + meetingId
                            + "/chat",

                    notice
            );
        }
    }



    // =============================================================
    // 후보별 득표수
    // =============================================================

    @GetMapping("/{meetingId}/votes/results")
    public List<CandidateVoteResult> getVoteResults(

            @PathVariable Long meetingId,

            @AuthenticationPrincipal
            AuthenticatedUser user

    ) {

        return voteService.getVoteResults(
                meetingId,
                user.userId()
        );
    }



    // =============================================================
    // 후보별 투표자
    // =============================================================

    @GetMapping("/{meetingId}/votes/voters")
    public List<VoteVoterResponse> getVoteVoters(

            @PathVariable Long meetingId,

            @AuthenticationPrincipal
            AuthenticatedUser user

    ) {

        return voteService.getVoteVoters(
                meetingId,
                user.userId()
        );
    }

    // =============================================================
    // 투표 진행 상태
    //
    // 후보 투표 + 기권 포함
    // =============================================================

    @GetMapping("/{meetingId}/votes/progress")
    public VoteProgressResponse getVoteProgress(
            @PathVariable Long meetingId,
            @AuthenticationPrincipal AuthenticatedUser user
    )
    {
        return voteService.getVoteProgress(
                meetingId,
                user.userId()
        );
    }

}