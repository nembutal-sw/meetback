package com.meetback.dev.controller;

import com.meetback.dev.dto.admin.AdminUserDetail;
import com.meetback.dev.dto.admin.AdminUserListItem;
import com.meetback.dev.dto.admin.PageResponse;
import com.meetback.dev.security.AuthenticatedUser;
import com.meetback.dev.service.AdminUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final AdminUserService adminUserService;

    @GetMapping
    public PageResponse<AdminUserListItem> findUsers(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Boolean deleted,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return adminUserService.findUsers(
                query,
                status,
                deleted,
                page,
                size
        );
    }

    @GetMapping("/{userId}")
    public AdminUserDetail findUser(
            @PathVariable Long userId
    ) {
        return adminUserService.findUser(userId);
    }

    @PatchMapping("/{userId}/suspend")
    public ResponseEntity<Void> suspendUser(
            @AuthenticationPrincipal AuthenticatedUser admin,
            @PathVariable Long userId
    ) {
        adminUserService.suspendUser(admin.userId(), userId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{userId}/activate")
    public ResponseEntity<Void> activateUser(
            @PathVariable Long userId
    ) {
        adminUserService.activateUser(userId);
        return ResponseEntity.noContent().build();
    }
}
