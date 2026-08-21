package com.meetback.dev.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MeetingCreateRequest {

    private String title;
    private LocalDateTime desiredEndAt;

}
