package com.meetback.dev.dto;

public record ParticipantLocationRequestDTO(

        String departureName,
        String departureAddress,
        Double departureLongitude,
        Double departureLatitude,

        String returnName,
        String returnAddress,
        Double returnLongitude,
        Double returnLatitude

) {
}