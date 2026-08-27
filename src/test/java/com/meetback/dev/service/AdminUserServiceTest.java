package com.meetback.dev.service;

import com.meetback.dev.domain.User;
import com.meetback.dev.domain.UserStatus;
import com.meetback.dev.event.UserSuspendedEvent;
import com.meetback.dev.repository.AdminUserMapper;
import com.meetback.dev.repository.RefreshTokenMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

    @Mock
    private AdminUserMapper userMapper;

    @Mock
    private RefreshTokenMapper tokenMapper;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private AdminUserService userService;

    @BeforeEach
    void setUp() {
        userService = new AdminUserService(
                userMapper,
                tokenMapper,
                eventPublisher
        );
    }

    @Test
    void cannotSuspendSelf() {
        User user = user(1L, "USER", UserStatus.ACTIVE);
        when(userMapper.findUserForUpdate(1L)).thenReturn(user);

        assertThatThrownBy(() -> userService.suspendUser(1L, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("자기 자신은 정지할 수 없습니다.");

        verify(userMapper, never()).suspendUser(1L);
        verifyNoInteractions(tokenMapper, eventPublisher);
    }

    @Test
    void cannotSuspendAnotherAdmin() {
        User user = user(2L, "ADMIN", UserStatus.ACTIVE);
        when(userMapper.findUserForUpdate(2L)).thenReturn(user);

        assertThatThrownBy(() -> userService.suspendUser(1L, 2L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("관리자 계정은 정지할 수 없습니다.");

        verify(userMapper, never()).suspendUser(2L);
        verifyNoInteractions(tokenMapper, eventPublisher);
    }

    @Test
    void cannotSuspendDeletedUser() {
        User user = user(2L, "USER", UserStatus.ACTIVE);
        user.setDeletedAt(LocalDateTime.now());
        when(userMapper.findUserForUpdate(2L)).thenReturn(user);

        assertThatThrownBy(() -> userService.suspendUser(1L, 2L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("탈퇴 처리 중인 사용자는 정지할 수 없습니다.");

        verify(userMapper, never()).suspendUser(2L);
        verifyNoInteractions(tokenMapper, eventPublisher);
    }

    @Test
    void suspensionRevokesRefreshTokenAndPublishesEvent() {
        User user = user(2L, "USER", UserStatus.ACTIVE);
        when(userMapper.findUserForUpdate(2L)).thenReturn(user);
        when(userMapper.suspendUser(2L)).thenReturn(1);

        userService.suspendUser(1L, 2L);

        verify(userMapper).suspendUser(2L);
        verify(tokenMapper).deleteByUserId(2L);

        ArgumentCaptor<Object> eventCaptor =
                ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue())
                .isEqualTo(new UserSuspendedEvent(2L));
    }

    @Test
    void activationDoesNotRollbackTokenVersionOrRestoreRefreshToken() {
        User user = user(2L, "USER", UserStatus.SUSPENDED);
        user.setTokenVersion(7);
        when(userMapper.findUserForUpdate(2L)).thenReturn(user);
        when(userMapper.activateUser(2L)).thenReturn(1);

        userService.activateUser(2L);

        verify(userMapper).activateUser(2L);
        verify(userMapper, never()).suspendUser(2L);
        verifyNoInteractions(tokenMapper, eventPublisher);
        assertThat(user.getTokenVersion()).isEqualTo(7);
    }

    private User user(Long userId, String role, UserStatus status) {
        User user = new User();
        user.setUserId(userId);
        user.setRole(role);
        user.setStatus(status);
        user.setTokenVersion(1);
        return user;
    }
}
