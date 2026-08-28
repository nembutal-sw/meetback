package com.meetback.dev.dto;

import com.meetback.dev.domain.MeetingStatus;
import com.meetback.dev.domain.MeetingType;

import java.time.LocalDateTime;

public record MeetingRoomResponse(
        Long meetingId,
        Long hostUserId,
        String title,
        String inviteCode,
        LocalDateTime desiredEndAt,
        MeetingType meetingType,
        MeetingStatus status,
        Long finalCandidateId
) {
}
