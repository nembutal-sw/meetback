package com.meetback.dev.service;

import com.meetback.dev.domain.User;
import com.meetback.dev.domain.UserStatus;
import com.meetback.dev.dto.admin.AdminUserDetail;
import com.meetback.dev.dto.admin.AdminUserListItem;
import com.meetback.dev.dto.admin.PageResponse;
import com.meetback.dev.event.UserSuspendedEvent;
import com.meetback.dev.repository.AdminUserMapper;
import com.meetback.dev.repository.RefreshTokenMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private static final int MAX_PAGE_SIZE = 100;

    private final AdminUserMapper adminUserMapper;
    private final RefreshTokenMapper refreshTokenMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public PageResponse<AdminUserListItem> findUsers(
            String query,
            String status,
            Boolean deleted,
            int page,
            int size
    ) {
        validatePage(page, size);

        String normalizedQuery = normalizeQuery(query);
        UserFilter userFilter = parseStatus(status, deleted);
        int offset = offset(page, size);

        List<AdminUserListItem> users = adminUserMapper.findUsers(
                normalizedQuery,
                userFilter.status(),
                userFilter.deleted(),
                offset,
                size
        );

        long total = adminUserMapper.countUsers(
                normalizedQuery,
                userFilter.status(),
                userFilter.deleted()
        );

        return new PageResponse<>(users, total, page, size);
    }

    @Transactional(readOnly = true)
    public AdminUserDetail findUser(Long userId) {
        validateUserId(userId);

        AdminUserDetail user = adminUserMapper.findUserDetail(userId);
        if (user == null) {
            throw new IllegalArgumentException("사용자를 찾을 수 없습니다.");
        }

        return user;
    }

    @Transactional
    public void suspendUser(Long adminId, Long userId) {
        validateUserId(adminId);
        validateUserId(userId);

        User user = adminUserMapper.findUserForUpdate(userId);
        validateSuspendTarget(adminId, user);

        int updatedRows = adminUserMapper.suspendUser(userId);
        if (updatedRows != 1) {
            throw new IllegalStateException("사용자 정지 상태 변경에 실패했습니다.");
        }

        refreshTokenMapper.deleteByUserId(userId);
        eventPublisher.publishEvent(new UserSuspendedEvent(userId));
    }

    @Transactional
    public void activateUser(Long userId) {
        validateUserId(userId);

        User user = adminUserMapper.findUserForUpdate(userId);
        if (user == null) {
            throw new IllegalArgumentException("사용자를 찾을 수 없습니다.");
        }
        if (user.getDeletedAt() != null) {
            throw new IllegalArgumentException("탈퇴 처리 중인 사용자는 활성화할 수 없습니다.");
        }
        if (user.getStatus() != UserStatus.SUSPENDED) {
            throw new IllegalArgumentException("정지된 사용자만 활성화할 수 있습니다.");
        }

        int updatedRows = adminUserMapper.activateUser(userId);
        if (updatedRows != 1) {
            throw new IllegalStateException("사용자 활성 상태 변경에 실패했습니다.");
        }
    }

    private void validateSuspendTarget(Long adminId, User user) {
        if (user == null) {
            throw new IllegalArgumentException("사용자를 찾을 수 없습니다.");
        }
        if (adminId.equals(user.getUserId())) {
            throw new IllegalArgumentException("자기 자신은 정지할 수 없습니다.");
        }
        if (isAdmin(user.getRole())) {
            throw new IllegalArgumentException("관리자 계정은 정지할 수 없습니다.");
        }
        if (user.getDeletedAt() != null) {
            throw new IllegalArgumentException("탈퇴 처리 중인 사용자는 정지할 수 없습니다.");
        }
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new IllegalArgumentException("활성 사용자만 정지할 수 있습니다.");
        }
    }

    private boolean isAdmin(String role) {
        return role != null
                && (role.equalsIgnoreCase("ADMIN")
                || role.equalsIgnoreCase("ROLE_ADMIN"));
    }

    private void validatePage(int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("페이지 번호는 0 이상이어야 합니다.");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("페이지 크기는 1에서 100 사이여야 합니다.");
        }
    }

    private int offset(int page, int size) {
        return (int) Math.min(
                (long) page * size,
                Integer.MAX_VALUE
        );
    }

    private void validateUserId(Long userId) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("올바른 사용자 ID가 필요합니다.");
        }
    }

    private String normalizeQuery(String query) {
        if (query == null || query.isBlank()) {
            return null;
        }

        String normalized = query.trim();
        if (normalized.length() > 100) {
            throw new IllegalArgumentException("검색어는 100자 이하여야 합니다.");
        }

        return normalized;
    }

    private UserFilter parseStatus(String status, Boolean deleted) {
        if (status == null || status.isBlank()) {
            return new UserFilter(null, deleted);
        }

        String normalized = status.trim().toUpperCase();
        if ("DELETED".equals(normalized)) {
            if (Boolean.FALSE.equals(deleted)) {
                throw new IllegalArgumentException("탈퇴 필터 값이 서로 일치하지 않습니다.");
            }
            return new UserFilter(null, true);
        }

        if (Boolean.TRUE.equals(deleted)) {
            throw new IllegalArgumentException("탈퇴 사용자와 이용 상태를 함께 조회할 수 없습니다.");
        }

        try {
            return new UserFilter(UserStatus.valueOf(normalized), deleted);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("올바르지 않은 사용자 상태입니다.");
        }
    }

    private record UserFilter(
            UserStatus status,
            Boolean deleted
    ) {
    }
}
