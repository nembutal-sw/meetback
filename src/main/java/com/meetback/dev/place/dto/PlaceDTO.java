package com.meetback.dev.place.dto;

public record PlaceDTO(
        String id,
        String name,
        String address,
        String roadAddress,
        double longitude,
        double latitude,
        String category,
        String url
) {
}