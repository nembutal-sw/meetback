package com.meetback.dev.controller;

import com.meetback.dev.domain.MeetingEventType;
import com.meetback.dev.dto.*;
import com.meetback.dev.realtime.event.RealtimeEvent;
import com.meetback.dev.realtime.publisher.RealtimeEventPublisher;
import com.meetback.dev.security.AuthenticatedUser;
import com.meetback.dev.service.ChatService;
import com.meetback.dev.service.VoteService;

import lombok.RequiredArgsConstructor;

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
    private final RealtimeEventPublisher realtimeEventPublisher;
    private final ChatService chatService;



    // =============================================================
    // 투표 / 재투표
    // =============================================================

    @PutMapping("/{meetingId}/votes")
    public void vote(
            @PathVariable Long meetingId,
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestBody VoteRequest request
    ) {

        /*
         * 1. 후보 투표 / 재투표 / 기권을 DB에 저장
         */

        voteService.vote(
                meetingId,
                user.userId(),
                request
        );


        /*
         * 2. 모든 접속자에게 투표 상태 갱신 이벤트 전달
         *
         * 이 이벤트는 채팅 DB에 저장하지 않습니다.
         * 화면에서 투표 현황을 다시 조회하게 만드는 용도입니다.
         */

        String voteUpdatedEventType = MeetingEventType.VOTE_UPDATED.name();

        realtimeEventPublisher.publish(
                RealtimeEvent.meetingBroadcast(
                        voteUpdatedEventType,
                        meetingId,
                        user.userId(),
                        Map.of(
                                "messageType","EVENT",
                                "eventType", voteUpdatedEventType,
                                "userId", user.userId()
                        )
                )
        );


        /*
         * 3. 모든 참가자가 투표했는지 확인
         */

        VoteProgressResponse progress =
                voteService.getVoteProgress(
                        meetingId,
                        user.userId()
                );


        if (!progress.allVoted()) {
            return;
        }


        /*
         * 4. 전원 투표 완료 공지
         */

        String allVotesCompletedEventType =
                MeetingEventType.ALL_VOTES_COMPLETED.name();

        ChatMessageResponse notice =
                chatService.saveSystemMessageOnce(
                        meetingId,
                        user.userId(),
                        allVotesCompletedEventType,
                        allVotesCompletedEventType,
                        "모든 참가자의 투표가 완료되었습니다."
                );

        /*
         * DB에 처음 저장된 경우에만 실시간으로 전달합니다.
         * 이미 저장된 공지라면 notice가 null입니다.
         */

        if (notice != null) {

            realtimeEventPublisher.publish(
                    RealtimeEvent.meetingBroadcast(
                            allVotesCompletedEventType,
                            meetingId,
                            user.userId(),
                            notice
                    )
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