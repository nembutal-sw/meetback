package com.meetback.dev.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminPageController {

    @GetMapping("/admin/terms")
    public String adminTermsPage()
    {
        return "admin/terms";
    }
}
