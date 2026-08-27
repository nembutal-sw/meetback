package com.meetback.dev.transport.dto;

import java.util.List;

public record RouteMapDTO(
        String startName,
        String endName,
        List<RouteLineDTO> lines,
        List<RouteStepDTO> steps
) {

    public RouteMapDTO(
            List<RouteLineDTO> lines
    ) {
        this(
                null,
                null,
                lines,
                List.of()
        );
    }
}