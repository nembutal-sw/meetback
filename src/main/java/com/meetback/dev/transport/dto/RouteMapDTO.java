package com.meetback.dev.transport.dto;

import java.util.List;

public record RouteMapDTO(
        List<RoutePointDTO> points
) {
}