package com.meetback.dev.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class LocationSubmitRequest {

    private String departureName;
    private String departureAddress;
    private BigDecimal departureLatitude;
    private BigDecimal departureLongitude;

    private String returnName;
    private String returnAddress;
    private BigDecimal returnLatitude;
    private BigDecimal returnLongitude;

}
