package com.meetback.dev.controller;

import com.meetback.dev.dto.admin.AdminTermResponse;
import com.meetback.dev.dto.admin.AdminTermSaveRequest;
import com.meetback.dev.service.AdminTermService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/admin/api/terms")
@RequiredArgsConstructor
public class AdminTermController {

    private final AdminTermService adminTermService;


    // 현재 적용 중인 약관조회
    @GetMapping("/{termCode}")
    public ResponseEntity<AdminTermResponse> getCurrentTerm(
            @PathVariable String termCode
    ){

        return ResponseEntity.ok(
                adminTermService.getCurrentTerm(
                        termCode
                )
        );
    }

    // 약관 버전 전체 이력 조회
    @GetMapping("/{termCode}/history")
    public ResponseEntity<List<AdminTermResponse>> getTermHistory(
            @PathVariable String termCode
    ) {

        return ResponseEntity.ok(
                adminTermService.getTermHistory(
                        termCode
                )
        );
    }

    // 새 약관 버전 등록
    @PostMapping
    public ResponseEntity<AdminTermResponse> createNewVersion(
            @RequestBody AdminTermSaveRequest request
    ) {

        return ResponseEntity.ok(
                adminTermService.createNewVersion(
                        request
                )
        );
    }
}
