package com.meetback.dev.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MeetingCreateResponse {

    private Long meetingId;
    private String inviteCode;

}
