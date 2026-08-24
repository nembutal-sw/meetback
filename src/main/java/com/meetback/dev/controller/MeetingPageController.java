package com.meetback.dev.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MeetingPageController {

    @GetMapping("/meeting")
    public String meeting()
    {
        return "meeting/location-test";
    }

}
