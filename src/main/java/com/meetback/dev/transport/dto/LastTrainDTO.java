package com.meetback.dev.transport.dto;

public record LastTrainDTO(
        String departureTime,
        String arrivalTime,
        int totalMinutes,
        int transferCount
) {
}