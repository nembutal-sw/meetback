package com.meetback.dev.controller;

import com.meetback.dev.dto.admin.AdminTermCreateRequest;
import com.meetback.dev.dto.admin.AdminTermResponse;
import com.meetback.dev.service.AdminTermService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/terms")
@PreAuthorize("hasRole('ADMIN')")
public class AdminTermController {

    private final AdminTermService adminTermService;

    @GetMapping
    public List<AdminTermResponse> findTerms() {
        return adminTermService.findTerms();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AdminTermResponse createTerm(
            @RequestBody AdminTermCreateRequest request
    ) {
        return adminTermService.createTerm(request);
    }
}
