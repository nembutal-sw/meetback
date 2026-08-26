package com.meetback.dev.controller;

import com.meetback.dev.dto.term.TermResponse;
import com.meetback.dev.service.TermService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/terms")
@RequiredArgsConstructor
public class TermController {

    private final TermService termService;

    // 현재 적용 중인 약관 조회
    @GetMapping
    public ResponseEntity<List<TermResponse>> getActiveTerms() {

        return ResponseEntity.ok(
                termService.getActiveTerms()
        );
    }
}