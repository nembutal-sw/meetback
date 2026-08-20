package com.meetback.dev.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CandidateCreateRequest {

    private String placeName;
    private String address;

    private BigDecimal latitude;
    private BigDecimal longitude;

}
