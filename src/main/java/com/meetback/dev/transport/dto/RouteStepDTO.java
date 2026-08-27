package com.meetback.dev.transport.dto;

public record RouteStepDTO(
        String startStationName,
        String endStationName,
        String lineName,
        Double startLongitude,
        Double startLatitude,
        Double endLongitude,
        Double endLatitude
) {
}