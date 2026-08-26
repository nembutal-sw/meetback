package com.meetback.dev.controller;

import com.meetback.dev.transport.dto.RouteMapDTO;
import com.meetback.dev.transport.service.RouteService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/routes")
public class RouteMapController {

    private final RouteService routeService;


    @GetMapping("/{candidateId}/{participantId}/map")
    public RouteMapDTO getRouteMap(
            @PathVariable Long candidateId,
            @PathVariable Long participantId
    ) {

        return routeService.getRouteMap(
                candidateId,
                participantId
        );
    }
}