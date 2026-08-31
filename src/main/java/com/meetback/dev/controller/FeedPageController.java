package com.meetback.dev.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class FeedPageController {

    @GetMapping("/feed")
    public String feedListPage() {
        return "feed/feedList";
    }

    @GetMapping("/feed/write")
    public String feedWritePage() {
        return "feed/feedWrite";
    }
}