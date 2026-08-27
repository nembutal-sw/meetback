package com.meetback.dev.transport.dto;

import java.util.List;

public record RouteMapDTO(
        String startName,
        String endName,
        List<RouteLineDTO> lines
) {

    public RouteMapDTO(
            List<RouteLineDTO> lines
    ) {
        this(
                null,
                null,
                lines
        );
    }
}