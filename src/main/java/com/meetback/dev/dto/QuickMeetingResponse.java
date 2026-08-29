package com.meetback.dev.dto;

import java.time.LocalDateTime;

public record QuickMeetingResponse(
        Long meetingId,
        String title,
        String inviteCode,
        LocalDateTime desiredEndAt,
        String hostNickname
) {
}