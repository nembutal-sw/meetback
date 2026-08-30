package com.meetback.dev.dto;

import com.meetback.dev.transport.dto.RouteMapDTO;

import java.time.LocalDateTime;

public record QuickFixedReturnCheckResponseDTO(
        boolean canReturn,
        Integer returnMinutes,
        Integer transferCount,
        LocalDateTime lastTrainDepartureAt,
        LocalDateTime lastSafeDepartureAt,
        LocalDateTime desiredEndAt,
        Integer marginMinutes,
        RouteMapDTO routeMap
) {
}