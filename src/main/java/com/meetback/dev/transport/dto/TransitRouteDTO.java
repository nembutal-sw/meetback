package com.meetback.dev.transport.dto;

public record TransitRouteDTO(
        int totalMinutes,
        int transferCount,
        String startStationId,
        String endStationId,
        String startStationName,
        String endStationName,
        String summary,
        String mapObj
) {
}
