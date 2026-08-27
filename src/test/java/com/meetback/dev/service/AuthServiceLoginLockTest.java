package com.meetback.dev.service;

import com.meetback.dev.domain.User;
import com.meetback.dev.domain.UserStatus;
import com.meetback.dev.dto.auth.LoginRequest;
import com.meetback.dev.dto.auth.LoginResponse;
import com.meetback.dev.oauth.GoogleIdentityProvider;
import com.meetback.dev.oauth.KakaoOAuthProvider;
import com.meetback.dev.repository.RefreshTokenMapper;
import com.meetback.dev.repository.SocialMapper;
import com.meetback.dev.repository.UserMapper;
import com.meetback.dev.security.JwtProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceLoginLockTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private SocialMapper socialMapper;

    @Mock
    private RefreshTokenMapper refreshTokenMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private KakaoOAuthProvider kakaoOAuthProvider;

    @Mock
    private GoogleIdentityProvider googleIdentityProvider;

    @Mock
    private MailService mailService;

    @Mock
    private TermService termService;

    @InjectMocks
    private AuthService authService;

    @Test
    void normalLoginReadsUserWithRowLock() {
        LoginRequest request = new LoginRequest();
        request.setEmail("member@example.com");
        request.setPassword("Current1!");

        User lockedUser = activeUser(1L, 0);
        lockedUser.setPasswordHash("password-hash");
        User updatedUser = activeUser(1L, 1);

        when(userMapper.selectByEmailForUpdate("member@example.com"))
                .thenReturn(lockedUser);
        when(passwordEncoder.matches("Current1!", "password-hash"))
                .thenReturn(true);
        when(userMapper.increaseTokenVersion(1L)).thenReturn(1);
        when(userMapper.selectById(1L)).thenReturn(updatedUser);
        when(jwtProvider.createAccessToken(1L, "USER", 1))
                .thenReturn("access-token");
        when(jwtProvider.createRefreshToken(1L, "USER", 1))
                .thenReturn("refresh-token");
        when(jwtProvider.getRefreshTokenExpirationSeconds())
                .thenReturn(3600L);
        when(refreshTokenMapper.selectByUserId(1L)).thenReturn(null);

        LoginResponse response = authService.login(request);

        assertThat(response.getAccessToken()).isEqualTo("access-token");
        verify(userMapper).selectByEmailForUpdate("member@example.com");
        verify(userMapper, never()).selectByEmail("member@example.com");
        verify(refreshTokenMapper).insertRefreshToken(any());
    }

    private User activeUser(Long userId, int tokenVersion) {
        User user = new User();
        user.setUserId(userId);
        user.setEmail("member@example.com");
        user.setNickname("member");
        user.setRole("USER");
        user.setStatus(UserStatus.ACTIVE);
        user.setTokenVersion(tokenVersion);
        return user;
    }
}
