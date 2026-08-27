package com.meetback.dev.service;

import com.meetback.dev.domain.User;
import com.meetback.dev.domain.UserStatus;
import com.meetback.dev.dto.admin.AdminAccountUpdateRequest;
import com.meetback.dev.dto.admin.AdminAccountUpdateResponse;
import com.meetback.dev.event.UserCredentialsChangedEvent;
import com.meetback.dev.repository.AdminUserMapper;
import com.meetback.dev.repository.RefreshTokenMapper;
import com.meetback.dev.repository.UserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminAccountServiceTest {

    @Mock
    private AdminUserMapper adminUserMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private RefreshTokenMapper refreshTokenMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private AdminAccountService adminAccountService;

    @Test
    void rejectsMismatchedPasswordConfirmationBeforeLockingRows() {
        AdminAccountUpdateRequest request = new AdminAccountUpdateRequest(
                "current-password",
                null,
                null,
                "NewStrong1!",
                "Different1!"
        );

        assertThatThrownBy(() -> adminAccountService.updateAccount(
                1L,
                1L,
                request
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("새 비밀번호 확인이 일치하지 않습니다.");

        verifyNoInteractions(adminUserMapper, passwordEncoder);
    }

    @Test
    void locksActorAndTargetInAscendingOrderBeforePasswordCheck() {
        User target = admin(10L, "target", "target-hash");
        User actor = admin(20L, "actor", "actor-hash");
        AdminAccountUpdateRequest request = new AdminAccountUpdateRequest(
                "actor-password",
                null,
                "target_new",
                null,
                null
        );

        when(adminUserMapper.findUserForUpdate(10L)).thenReturn(target);
        when(adminUserMapper.findUserForUpdate(20L)).thenReturn(actor);
        when(passwordEncoder.matches("actor-password", "actor-hash"))
                .thenReturn(true);
        when(userMapper.existByNickname("target_new")).thenReturn(0);
        when(adminUserMapper.updateAdminAccount(
                10L,
                null,
                "target_new",
                null,
                false
        )).thenReturn(1);

        AdminAccountUpdateResponse response = adminAccountService.updateAccount(
                20L,
                10L,
                request
        );

        InOrder order = inOrder(adminUserMapper, passwordEncoder);
        order.verify(adminUserMapper).findUserForUpdate(10L);
        order.verify(adminUserMapper).findUserForUpdate(20L);
        order.verify(passwordEncoder).matches(
                "actor-password",
                "actor-hash"
        );

        assertThat(response.reLoginRequired()).isFalse();
        verifyNoInteractions(refreshTokenMapper, eventPublisher);
    }

    @Test
    void locksSelfOnceAndInvalidatesSessionsForCredentialChange() {
        User account = admin(7L, "admin", "old-hash");
        AdminAccountUpdateRequest request = new AdminAccountUpdateRequest(
                "current-password",
                "admin2",
                null,
                "NewStrong1!",
                "NewStrong1!"
        );

        when(adminUserMapper.findUserForUpdate(7L)).thenReturn(account);
        when(passwordEncoder.matches("current-password", "old-hash"))
                .thenReturn(true);
        when(passwordEncoder.matches("NewStrong1!", "old-hash"))
                .thenReturn(false);
        when(passwordEncoder.encode("NewStrong1!"))
                .thenReturn("new-hash");
        when(userMapper.existByEmail("admin2")).thenReturn(0);
        when(adminUserMapper.updateAdminAccount(
                7L,
                "admin2",
                null,
                "new-hash",
                true
        )).thenReturn(1);

        AdminAccountUpdateResponse response = adminAccountService.updateAccount(
                7L,
                7L,
                request
        );

        assertThat(response.reLoginRequired()).isTrue();
        verify(adminUserMapper, times(1)).findUserForUpdate(7L);
        verify(refreshTokenMapper).deleteByUserId(7L);
        verify(eventPublisher).publishEvent(
                new UserCredentialsChangedEvent(7L)
        );
        verify(userMapper, never()).existByNickname("admin");
    }

    private User admin(Long userId, String nickname, String passwordHash) {
        User user = new User();
        user.setUserId(userId);
        user.setEmail(nickname);
        user.setNickname(nickname);
        user.setPasswordHash(passwordHash);
        user.setRole("ADMIN");
        user.setStatus(UserStatus.ACTIVE);
        user.setTokenVersion(1);
        return user;
    }
}
