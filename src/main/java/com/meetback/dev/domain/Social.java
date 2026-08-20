package com.meetback.dev.domain;

import lombok.Data;

@Data
public class Social {

    private Long socialId;
    private Long userId;
    private String provider;
    private String providerId;
    private String email;
    private Boolean emailVerified;
    private String name;
}
