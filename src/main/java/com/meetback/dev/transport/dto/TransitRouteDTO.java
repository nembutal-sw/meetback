package com.meetback.dev.transport.dto;

import java.util.List;

public record TransitRouteDTO(
        int totalMinutes,
        int transferCount,
        String startStationId,
        String endStationId,
        String startStationName,
        String endStationName,
        String summary,
        String mapObj,
        List<RouteStepDTO> steps
) {
}