package com.meetback.dev.transport.dto;

import java.util.List;

public record RouteLineDTO(
        int type,
        List<RoutePointDTO> points
) {
}