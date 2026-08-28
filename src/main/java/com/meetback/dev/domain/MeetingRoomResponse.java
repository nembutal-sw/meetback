package com.meetback.dev.domain;

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
