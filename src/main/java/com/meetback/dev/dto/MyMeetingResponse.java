package com.meetback.dev.dto;

import com.meetback.dev.domain.MeetingType;
import lombok.*;

@Data
public class MyMeetingResponse {

    private Long meetingId;
    private String title;
    private String inviteCode;
    private MeetingType meetingType;
}