package com.meetback.dev.controller;

import com.meetback.dev.dto.admin.AdminCandidate;
import com.meetback.dev.dto.admin.AdminChatMessage;
import com.meetback.dev.dto.admin.AdminMeetingDetail;
import com.meetback.dev.dto.admin.AdminMeetingListItem;
import com.meetback.dev.dto.admin.AdminParticipant;
import com.meetback.dev.dto.admin.AdminVoteSummary;
import com.meetback.dev.dto.admin.PageResponse;
import com.meetback.dev.service.AdminMeetingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 관리자 모임 관련 조회 API. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/meetings")
public class AdminMeetingController {

    private final AdminMeetingService adminMeetingService;

    @GetMapping
    public PageResponse<AdminMeetingListItem> getMeetings(
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return adminMeetingService.getMeetings(query, page, size);
    }

    @GetMapping("/{meetingId}")
    public AdminMeetingDetail getMeeting(
            @PathVariable Long meetingId
    ) {
        return adminMeetingService.getMeeting(meetingId);
    }

    @GetMapping("/{meetingId}/participants")
    public List<AdminParticipant> getParticipants(
            @PathVariable Long meetingId
    ) {
        return adminMeetingService.getParticipants(meetingId);
    }

    @GetMapping("/{meetingId}/candidates")
    public List<AdminCandidate> getCandidates(
            @PathVariable Long meetingId
    ) {
        return adminMeetingService.getCandidates(meetingId);
    }

    @GetMapping("/{meetingId}/votes")
    public AdminVoteSummary getVotes(
            @PathVariable Long meetingId
    ) {
        return adminMeetingService.getVotes(meetingId);
    }

    @GetMapping("/{meetingId}/chats")
    public PageResponse<AdminChatMessage> getChats(
            @PathVariable Long meetingId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return adminMeetingService.getChats(meetingId, page, size);
    }
}
