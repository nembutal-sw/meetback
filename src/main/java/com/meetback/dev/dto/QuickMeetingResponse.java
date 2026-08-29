package com.meetback.dev.dto;

import com.meetback.dev.domain.MeetingType;

import java.time.LocalDateTime;

public record QuickMeetingResponse(
        Long meetingId,
        String title,
        String inviteCode,
        MeetingType meetingType,
        LocalDateTime meetingStartAt,
        LocalDateTime desiredEndAt,
        Integer currentParticipants,
        Integer maxParticipants,
        String fixedPlaceName,
        String fixedPlaceAddress,
        String hostNickname
) {
}