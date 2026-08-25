package com.meetback.dev.dto;

public record MeetingJoinResponse(
        Long meetingId,
        boolean newlyJoined
) {
}
