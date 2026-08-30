package com.meetback.dev.dto;

public record QuickFixedReturnLocationRequestDTO(
        String name,
        String address,
        Double longitude,
        Double latitude
) {
}