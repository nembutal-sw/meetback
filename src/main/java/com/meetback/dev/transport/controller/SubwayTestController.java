package com.meetback.dev.transport.controller;

import com.meetback.dev.transport.client.OdsaySubwayClient;
import com.meetback.dev.transport.dto.LastTrainDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/subway-test")
public class SubwayTestController {

    private final OdsaySubwayClient odsaySubwayClient;

    @GetMapping("/last-train")
    public LastTrainDTO lastTrain(
            @RequestParam String sid,
            @RequestParam String eid,
            @RequestParam(defaultValue = "1") int day
    ) {
        return odsaySubwayClient.findLastTrain(
                sid,
                eid,
                day
        );
    }
}