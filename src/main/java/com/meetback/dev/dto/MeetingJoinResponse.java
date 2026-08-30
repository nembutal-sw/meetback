package com.meetback.dev.dto;

import com.meetback.dev.domain.MeetingType;

public record MeetingJoinResponse(
        Long meetingId,
        boolean newlyJoined,
        MeetingType meetingType
) {
}
