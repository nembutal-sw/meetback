package com.meetback.dev.transport.controller;

import com.meetback.dev.transport.dto.TransitRouteDTO;
import com.meetback.dev.transport.service.RouteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/routes")
public class RouteController {

    private final RouteService routeService;


    @GetMapping("/test")
    public TransitRouteDTO testRoute(
            @RequestParam Long participantId,
            @RequestParam Long candidateId) {

        return routeService.testRoute(
                participantId,
                candidateId
        );
    }


    @ExceptionHandler({
            IllegalStateException.class,
            IllegalArgumentException.class
    })
    public ResponseEntity<String> handleError(
            RuntimeException e) {

        return ResponseEntity
                .badRequest()
                .body(e.getMessage());
    }
}