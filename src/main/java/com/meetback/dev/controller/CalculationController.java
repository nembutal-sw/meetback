package com.meetback.dev.controller;

import com.meetback.dev.domain.CandidateEvaluation;
import com.meetback.dev.domain.CandidateReturnResult;
import com.meetback.dev.domain.Meeting;
import com.meetback.dev.domain.MeetingCandidate;
import com.meetback.dev.domain.MeetingParticipant;
import com.meetback.dev.domain.ParticipantStatus;
import com.meetback.dev.dto.CandidateRankingResponseDTO;
import com.meetback.dev.dto.QuickFixedReturnCheckResponseDTO;
import com.meetback.dev.dto.QuickFixedReturnLocationRequestDTO;
import com.meetback.dev.repository.MeetingCandidateMapper;
import com.meetback.dev.repository.MeetingMapper;
import com.meetback.dev.repository.MeetingParticipantMapper;
import com.meetback.dev.security.AuthenticatedUser;
import com.meetback.dev.service.CalculationService;
import com.meetback.dev.service.CandidateEvaluationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

@RestController
@RequiredArgsConstructor
@RequestMapping("/calculations")
public class CalculationController {

    private final CalculationService calculationService;
    private final CandidateEvaluationService candidateEvaluationService;

    private final MeetingMapper meetingMapper;
    private final MeetingParticipantMapper meetingParticipantMapper;
    private final MeetingCandidateMapper meetingCandidateMapper;



    @PostMapping("/return")
    public CandidateReturnResult calculateReturn(
            @RequestParam Long participantId,
            @RequestParam Long candidateId,
            @AuthenticationPrincipal AuthenticatedUser user) {

        MeetingParticipant participant = meetingParticipantMapper.findById(participantId);

        if (participant == null) {
            throw new IllegalArgumentException("참가자를 찾을 수 없습니다.");
        }

        if (!Objects.equals(participant.getUserId(), user.userId())) {
            throw new AccessDeniedException("본인의 귀가 결과만 계산할 수 있습니다.");
        }

        if (participant.getParticipantStatus() != ParticipantStatus.ACTIVE) {

            throw new AccessDeniedException("현재 모임의 활성 참가자가 아닙니다.");
        }


        MeetingCandidate candidate = meetingCandidateMapper.findById(candidateId);


        if (candidate == null) {
            throw new IllegalArgumentException("후보 장소를 찾을 수 없습니다.");
        }


        if (!Objects.equals(participant.getMeetingId(), candidate.getMeetingId())) {

            throw new AccessDeniedException("해당 모임의 후보 장소가 아닙니다.");
        }


        return calculationService.calculateReturn(participantId, candidateId);
    }


    @PostMapping("/candidate")
    public List<CandidateReturnResult> calculateCandidate(
            @RequestParam Long candidateId,
            @AuthenticationPrincipal AuthenticatedUser user) {

        MeetingCandidate candidate = meetingCandidateMapper.findById(candidateId);

        if (candidate == null) {
            throw new IllegalArgumentException("후보 장소를 찾을 수 없습니다.");
        }

        requireHost(candidate.getMeetingId(),user.userId());

        return calculationService.calculateCandidate(
                candidateId
        );
    }



    @PostMapping("/meeting")
    public List<CandidateEvaluation> calculateMeeting(
            @RequestParam Long meetingId,
            @AuthenticationPrincipal AuthenticatedUser user) {

        requireHost(meetingId, user.userId());

        return calculationService.calculateMeeting(meetingId);
    }


    @GetMapping("/meeting/recommendation")
    public CandidateRankingResponseDTO getTopRecommendation(
            @RequestParam Long meetingId,
            @AuthenticationPrincipal AuthenticatedUser user) {

        requireActiveParticipant(meetingId, user.userId());

        return candidateEvaluationService.getTopRecommendation(meetingId);
    }



    @GetMapping("/meeting/ranking")
    public List<CandidateRankingResponseDTO> getRanking(
            @RequestParam Long meetingId,
            @AuthenticationPrincipal AuthenticatedUser user) {

        requireActiveParticipant(meetingId, user.userId());

        return candidateEvaluationService.getRanking(meetingId);
    }

    @PostMapping("/meeting/{meetingId}/quick-fixed/return-check")
    public QuickFixedReturnCheckResponseDTO checkQuickFixedReturn(
            @PathVariable Long meetingId,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return calculationService
                .calculateQuickFixedReturn(meetingId, user.userId());
    }

    @PostMapping("/meeting/{meetingId}/quick-fixed/preview")
    public QuickFixedReturnCheckResponseDTO previewQuickFixedReturn(
            @PathVariable Long meetingId,
            @RequestBody QuickFixedReturnLocationRequestDTO request) {
        return calculationService.calculateQuickFixedPreview(meetingId, request);
    }
    private void requireActiveParticipant(Long meetingId, Long userId) {
        Meeting meeting = meetingMapper.findById(meetingId);


        if (meeting == null) {
            throw new IllegalArgumentException("모임을 찾을 수 없습니다.");
        }


        MeetingParticipant participant = meetingParticipantMapper.findByMeetingIdAndUserId(
                meetingId, userId);



        if (participant == null || participant.getParticipantStatus() != ParticipantStatus.ACTIVE) {
            throw new AccessDeniedException("해당 모임의 참가자만 조회할 수 있습니다.");
        }
    }

    private void requireHost(Long meetingId, Long userId) {

        Meeting meeting = meetingMapper.findById(meetingId);


        if (meeting == null) {
            throw new IllegalArgumentException("모임을 찾을 수 없습니다.");
        }


        if (!Objects.equals(meeting.getHostUserId(), userId)) {
            throw new AccessDeniedException("방장만 계산을 실행할 수 있습니다.");
        }



        MeetingParticipant participant = meetingParticipantMapper
                        .findByMeetingIdAndUserId(meetingId, userId);


        if (participant == null || participant.getParticipantStatus() != ParticipantStatus.ACTIVE) {
            throw new AccessDeniedException("현재 모임의 활성 참가자가 아닙니다.");
        }
    }
}