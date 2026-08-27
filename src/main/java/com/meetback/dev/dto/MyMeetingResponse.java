package com.meetback.dev.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MyMeetingResponse {

    private Long meetingId;
    private String title;
    private String inviteCode;
}