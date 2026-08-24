package com.meetback.dev.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MeetingPageController {

    @GetMapping("/location-test")
    public String locationTestPage() {
        return "meeting/location-test";
    }

    @GetMapping("/meeting")
    public String meeting()
    {
        return "meeting/location-test";
    }

}
