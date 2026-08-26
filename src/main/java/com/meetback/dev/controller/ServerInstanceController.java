package com.meetback.dev.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class ServerInstanceController {

    private final String serverInstanceId;


    @GetMapping("/auth/server-instance")
    public Map<String, String> getServerInstance() {

        return Map.of(
                "serverInstanceId",
                serverInstanceId
        );
    }
}