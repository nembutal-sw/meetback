package com.meetback.dev.service;

import com.meetback.dev.dto.admin.AdminCandidate;
import com.meetback.dev.dto.admin.AdminChatMessage;
import com.meetback.dev.dto.admin.AdminMeetingDetail;
import com.meetback.dev.dto.admin.AdminMeetingListItem;
import com.meetback.dev.dto.admin.AdminParticipant;
import com.meetback.dev.dto.admin.AdminVoteSummary;
import com.meetback.dev.dto.admin.PageResponse;
import com.meetback.dev.repository.AdminMeetingMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 관리자 모임 조회와 페이지 범위를 관리한다. */
@Service
@RequiredArgsConstructor
public class AdminMeetingService {

    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;
    private static final int MAX_QUERY_LENGTH = 100;

    private final AdminMeetingMapper adminMeetingMapper;

    @Transactional(readOnly = true)
    public PageResponse<AdminMeetingListItem> getMeetings(
            String query,
            int page,
            int size
    ) {
        int safePage = normalizePage(page);
        int safeSize = normalizeSize(size);
        int offset = offset(safePage, safeSize);
        String safeQuery = normalizeQuery(query);

        List<AdminMeetingListItem> items = adminMeetingMapper.findMeetings(
                safeQuery,
                offset,
                safeSize
        );
        long total = adminMeetingMapper.countMeetings(safeQuery);
        return new PageResponse<>(items, total, safePage, safeSize);
    }

    @Transactional(readOnly = true)
    public AdminMeetingDetail getMeeting(Long meetingId) {
        return requireMeeting(meetingId);
    }

    @Transactional(readOnly = true)
    public List<AdminParticipant> getParticipants(Long meetingId) {
        requireMeeting(meetingId);
        return adminMeetingMapper.findParticipants(meetingId);
    }

    @Transactional(readOnly = true)
    public List<AdminCandidate> getCandidates(Long meetingId) {
        requireMeeting(meetingId);
        return adminMeetingMapper.findCandidates(meetingId);
    }

    @Transactional(readOnly = true)
    public AdminVoteSummary getVotes(Long meetingId) {
        requireMeeting(meetingId);
        AdminVoteSummary summary = adminMeetingMapper.findVoteSummary(meetingId);
        summary.setNotVotedParticipants(Math.max(
                summary.getTotalParticipants() - summary.getVotedParticipants(),
                0
        ));
        summary.setCandidates(adminMeetingMapper.findCandidateVotes(meetingId));
        return summary;
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminChatMessage> getChats(
            Long meetingId,
            int page,
            int size
    ) {
        requireMeeting(meetingId);
        int safePage = normalizePage(page);
        int safeSize = normalizeSize(size);
        int offset = offset(safePage, safeSize);

        List<AdminChatMessage> items = adminMeetingMapper.findChats(
                meetingId,
                offset,
                safeSize
        );
        long total = adminMeetingMapper.countChats(meetingId);
        return new PageResponse<>(items, total, safePage, safeSize);
    }

    private AdminMeetingDetail requireMeeting(Long meetingId) {
        if (meetingId == null || meetingId < 1) {
            throw new IllegalArgumentException("올바른 모임 ID가 필요합니다.");
        }

        AdminMeetingDetail meeting = adminMeetingMapper.findMeetingDetail(meetingId);
        if (meeting == null) {
            throw new IllegalArgumentException("존재하지 않는 모임입니다.");
        }
        return meeting;
    }

    private String normalizeQuery(String query) {
        if (query == null || query.isBlank()) {
            return null;
        }

        String safeQuery = query.trim();
        if (safeQuery.length() > MAX_QUERY_LENGTH) {
            throw new IllegalArgumentException("검색어는 100자 이하여야 합니다.");
        }
        return safeQuery;
    }

    private int normalizePage(int page) {
        return Math.max(page, 0);
    }

    private int normalizeSize(int size) {
        if (size < 1) {
            return DEFAULT_SIZE;
        }
        return Math.min(size, MAX_SIZE);
    }

    private int offset(int page, int size) {
        // 지나치게 큰 페이지 값도 정수 범위를 넘지 않게 제한한다.
        return (int) Math.min((long) page * size, Integer.MAX_VALUE);
    }
}
