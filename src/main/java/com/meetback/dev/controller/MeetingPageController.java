package com.meetback.dev.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MeetingPageController {

    // 모임방
    @GetMapping("/meeting")
    public String meetingRoom() {

        return "meeting/meeting-room";
    }


    // 장소 입력 / 수정
    @GetMapping("/meeting/location")
    public String locationInput() {

        return "meeting/location-input";
    }


    // 장소 투표
    @GetMapping("/meeting/vote")
    public String vote() {

        return "meeting/vote";
    }
}