package com.meetback.dev.dto;

import java.time.LocalDateTime;

public record ParticipantReturnSummaryDTO(

        Long participantId,
        String nickname,
        Integer returnMinutes,
        Integer transferCount,

        LocalDateTime lastTrainDepartureAt,
        LocalDateTime lastTrainArrivalAt,
        LocalDateTime lastSafeDepartureAt,

        boolean canReturn

) {
}