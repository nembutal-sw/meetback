package com.meetback.dev.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

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

    @GetMapping("/feed/{feedId}")
    public String feedDetailPage(
            @PathVariable Long feedId,
            Model model
    ) {

        model.addAttribute(
                "feedId",
                feedId
        );

        return "feed/feedDetail";
    }
}