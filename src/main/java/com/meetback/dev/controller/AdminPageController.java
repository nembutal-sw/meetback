package com.meetback.dev.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminPageController {

    @GetMapping("/admin")
    public String dashboard() {
        // 민감한 운영 데이터는 관리자 API를 통해서만 조회한다.
        return "admin/dashboard";
    }
}
