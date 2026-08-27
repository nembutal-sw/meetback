package com.meetback.dev.controller;

import com.meetback.dev.dto.admin.AdminAccountResponse;
import com.meetback.dev.dto.admin.AdminAccountUpdateRequest;
import com.meetback.dev.dto.admin.AdminAccountUpdateResponse;
import com.meetback.dev.security.AuthenticatedUser;
import com.meetback.dev.service.AdminAccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/accounts")
@PreAuthorize("hasRole('ADMIN')")
public class AdminAccountController {

    private final AdminAccountService adminAccountService;

    @GetMapping("/{userId}")
    public AdminAccountResponse findAccount(
            @PathVariable Long userId
    ) {
        return adminAccountService.findAccount(userId);
    }

    @PatchMapping("/{userId}")
    public AdminAccountUpdateResponse updateAccount(
            @AuthenticationPrincipal AuthenticatedUser actor,
            @PathVariable Long userId,
            @RequestBody AdminAccountUpdateRequest request
    ) {
        return adminAccountService.updateAccount(
                actor.userId(),
                userId,
                request
        );
    }
}
