package com.meetback.dev.service;

import com.meetback.dev.domain.User;
import com.meetback.dev.domain.UserStatus;
import com.meetback.dev.dto.admin.AdminAccountResponse;
import com.meetback.dev.dto.admin.AdminAccountUpdateRequest;
import com.meetback.dev.dto.admin.AdminAccountUpdateResponse;
import com.meetback.dev.dto.admin.AdminUserDetail;
import com.meetback.dev.event.UserCredentialsChangedEvent;
import com.meetback.dev.repository.AdminUserMapper;
import com.meetback.dev.repository.RefreshTokenMapper;
import com.meetback.dev.repository.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class AdminAccountService {

    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
            "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[^A-Za-z0-9\\s])\\S{8,20}$"
    );

    private static final Pattern NICKNAME_PATTERN = Pattern.compile(
            "^[가-힣A-Za-z0-9_]{2,12}$"
    );

    private final AdminUserMapper adminUserMapper;
    private final UserMapper userMapper;
    private final RefreshTokenMapper refreshTokenMapper;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public AdminAccountResponse findAccount(Long userId) {
        validateUserId(userId);

        AdminUserDetail account = adminUserMapper.findUserDetail(userId);
        validateAdminTarget(account);

        return new AdminAccountResponse(
                account.getUserId(),
                account.getEmail(),
                account.getNickname()
        );
    }

    @Transactional
    public AdminAccountUpdateResponse updateAccount(
            Long actorId,
            Long targetId,
            AdminAccountUpdateRequest request
    ) {
        validateUserId(actorId);
        validateUserId(targetId);
        validateRequest(request);

        LockedAccounts accounts = lockAccounts(actorId, targetId);
        User actor = accounts.actor();
        User target = accounts.target();

        validateActor(actor, request.currentPassword());
        validateAdminTarget(target);

        String loginId = changedLoginId(request.loginId(), target);
        String nickname = changedNickname(request.nickname(), target);
        String passwordHash = changedPassword(request.newPassword(), target);
        boolean credentialsChanged = loginId != null || passwordHash != null;

        if (loginId == null && nickname == null && passwordHash == null) {
            throw new IllegalArgumentException("실제로 변경할 계정정보가 없습니다.");
        }

        checkDuplicates(loginId, nickname);

        try {
            int updatedRows = adminUserMapper.updateAdminAccount(
                    targetId,
                    loginId,
                    nickname,
                    passwordHash,
                    credentialsChanged
            );

            if (updatedRows != 1) {
                throw new IllegalStateException("관리자 계정정보 변경에 실패했습니다.");
            }
        } catch (DuplicateKeyException e) {
            throw new IllegalArgumentException(
                    "이미 사용 중인 로그인 ID 또는 닉네임입니다."
            );
        }

        if (credentialsChanged) {
            refreshTokenMapper.deleteByUserId(targetId);
            eventPublisher.publishEvent(
                    new UserCredentialsChangedEvent(targetId)
            );
        }

        boolean reLoginRequired = credentialsChanged
                && actorId.equals(targetId);

        return new AdminAccountUpdateResponse(
                reLoginRequired
                        ? "계정정보가 변경되었습니다. 다시 로그인해주세요."
                        : "계정정보가 변경되었습니다.",
                reLoginRequired
        );
    }

    private void validateRequest(AdminAccountUpdateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("계정 변경 요청이 없습니다.");
        }
        if (request.currentPassword() == null
                || request.currentPassword().isBlank()) {
            throw new IllegalArgumentException("현재 비밀번호를 입력해주세요.");
        }
        if (isBlank(request.loginId())
                && isBlank(request.nickname())
                && isBlank(request.newPassword())) {
            throw new IllegalArgumentException("변경할 계정정보를 입력해주세요.");
        }
        if (!isBlank(request.newPassword())
                && !request.newPassword().equals(
                request.newPasswordConfirm()
        )) {
            throw new IllegalArgumentException("새 비밀번호 확인이 일치하지 않습니다.");
        }
    }

    private LockedAccounts lockAccounts(Long actorId, Long targetId) {
        if (actorId.equals(targetId)) {
            User account = adminUserMapper.findUserForUpdate(actorId);
            return new LockedAccounts(account, account);
        }

        Long firstId = Math.min(actorId, targetId);
        Long secondId = Math.max(actorId, targetId);
        User first = adminUserMapper.findUserForUpdate(firstId);
        User second = adminUserMapper.findUserForUpdate(secondId);

        User actor = actorId.equals(firstId) ? first : second;
        User target = targetId.equals(firstId) ? first : second;
        return new LockedAccounts(actor, target);
    }

    private void validateActor(User actor, String currentPassword) {
        if (actor == null
                || !isAdmin(actor.getRole())
                || actor.getStatus() != UserStatus.ACTIVE
                || actor.getDeletedAt() != null) {
            throw new IllegalArgumentException("관리자 정보를 확인할 수 없습니다.");
        }
        if (actor.getPasswordHash() == null
                || !passwordEncoder.matches(
                currentPassword,
                actor.getPasswordHash()
        )) {
            throw new IllegalArgumentException("현재 비밀번호가 일치하지 않습니다.");
        }
    }

    private void validateAdminTarget(AdminUserDetail target) {
        if (target == null || !isAdmin(target.getRole())) {
            throw new IllegalArgumentException("관리자 계정을 찾을 수 없습니다.");
        }
        if (target.getDeletedAt() != null) {
            throw new IllegalArgumentException("탈퇴 처리된 관리자 계정입니다.");
        }
    }

    private void validateAdminTarget(User target) {
        if (target == null || !isAdmin(target.getRole())) {
            throw new IllegalArgumentException("관리자 계정을 찾을 수 없습니다.");
        }
        if (target.getDeletedAt() != null) {
            throw new IllegalArgumentException("탈퇴 처리된 관리자 계정입니다.");
        }
    }

    private String changedLoginId(String value, User target) {
        if (isBlank(value)) {
            return null;
        }

        String loginId = value.trim().toLowerCase(Locale.ROOT);
        if (loginId.length() > 255 || loginId.chars().anyMatch(Character::isWhitespace)) {
            throw new IllegalArgumentException(
                    "로그인 ID는 공백 없이 255자 이하여야 합니다."
            );
        }

        return loginId.equals(target.getEmail()) ? null : loginId;
    }

    private String changedNickname(String value, User target) {
        if (isBlank(value)) {
            return null;
        }

        String nickname = value.trim();
        if (!NICKNAME_PATTERN.matcher(nickname).matches()) {
            throw new IllegalArgumentException(
                    "닉네임은 한글, 영문, 숫자, 밑줄로 2~12자여야 합니다."
            );
        }

        return nickname.equals(target.getNickname()) ? null : nickname;
    }

    private String changedPassword(String value, User target) {
        if (isBlank(value)) {
            return null;
        }
        if (!PASSWORD_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException(
                    "비밀번호는 8~20자이며 대문자, 소문자, 숫자, 특수문자를 각각 1개 이상 포함해야 하며 공백은 사용할 수 없습니다."
            );
        }
        if (target.getPasswordHash() != null
                && passwordEncoder.matches(value, target.getPasswordHash())) {
            return null;
        }

        return passwordEncoder.encode(value);
    }

    private void checkDuplicates(String loginId, String nickname) {
        if (loginId != null && userMapper.existByEmail(loginId) > 0) {
            throw new IllegalArgumentException("이미 사용 중인 로그인 ID입니다.");
        }
        if (nickname != null && userMapper.existByNickname(nickname) > 0) {
            throw new IllegalArgumentException("이미 사용 중인 닉네임입니다.");
        }
    }

    private boolean isAdmin(String role) {
        return role != null
                && ("ADMIN".equalsIgnoreCase(role)
                || "ROLE_ADMIN".equalsIgnoreCase(role));
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private void validateUserId(Long userId) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("올바른 사용자 ID가 필요합니다.");
        }
    }

    private record LockedAccounts(
            User actor,
            User target
    ) {
    }
}
