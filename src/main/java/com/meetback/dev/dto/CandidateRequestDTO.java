package com.meetback.dev.dto;

public record CandidateRequestDTO(
        String name,
        String address,
        Double longitude,
        Double latitude
) {
}
