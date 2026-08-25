package com.meetback.dev.service;

import com.meetback.dev.domain.Social;
import com.meetback.dev.domain.User;
import com.meetback.dev.dto.auth.GoogleLoginRequest;
import com.meetback.dev.dto.auth.LoginResponse;
import com.meetback.dev.oauth.GoogleIdentityProvider;
import com.meetback.dev.oauth.GoogleUserInfo;
import com.meetback.dev.oauth.KakaoOAuthProvider;
import com.meetback.dev.repository.RefreshTokenMapper;
import com.meetback.dev.repository.SocialMapper;
import com.meetback.dev.repository.UserMapper;
import com.meetback.dev.security.JwtProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceGoogleLoginTest {

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

    private AuthService authService;

    @BeforeEach
    void setUp() {

        authService =
                new AuthService(
                        userMapper,
                        socialMapper,
                        refreshTokenMapper,
                        passwordEncoder,
                        jwtProvider,
                        kakaoOAuthProvider,
                        googleIdentityProvider
                );
    }

    @Test
    void newGoogleUserStoresProfileNameAsNicknameAndIssuesMeetBackTokens() {

        GoogleUserInfo googleUserInfo =
                googleUserInfo(
                        "google-sub-12345678",
                        "google-user@example.com",
                        "Google Nickname"
                );

        when(googleIdentityProvider.verifyIdToken("google-id-token"))
                .thenReturn(googleUserInfo);

        when(socialMapper.selectByProviderAndProviderId(
                "GOOGLE",
                googleUserInfo.getProviderId()
        )).thenReturn(null);

        when(userMapper.selectByEmail(googleUserInfo.getEmail()))
                .thenReturn(null);

        when(userMapper.existByNickname("Google Nickname"))
                .thenReturn(0);

        // MyBatis가 INSERT 후 생성된 user_id를 객체에 채우는 동작을 재현한다.
        doAnswer(invocation -> {

            User user =
                    invocation.getArgument(0);

            user.setUserId(10L);

            return 1;

        }).when(userMapper).insertUser(any(User.class));

        stubTokenIssue(10L);


        LoginResponse response =
                authService.googleLogin(
                        googleLoginRequest()
                );


        ArgumentCaptor<User> userCaptor =
                ArgumentCaptor.forClass(User.class);

        verify(userMapper).insertUser(
                userCaptor.capture()
        );

        assertThat(userCaptor.getValue().getEmail())
                .isEqualTo("google-user@example.com");

        assertThat(userCaptor.getValue().getNickname())
                .isEqualTo("Google Nickname");

        assertThat(userCaptor.getValue().getPasswordHash())
                .isNull();


        ArgumentCaptor<Social> socialCaptor =
                ArgumentCaptor.forClass(Social.class);

        verify(socialMapper).insertSocial(
                socialCaptor.capture()
        );

        assertThat(socialCaptor.getValue().getProvider())
                .isEqualTo("GOOGLE");

        assertThat(socialCaptor.getValue().getProviderId())
                .isEqualTo("google-sub-12345678");

        // Google 프로필 이름은 social.name이 아니라 users.nickname에만 저장한다.
        assertThat(socialCaptor.getValue().getName())
                .isNull();

        assertThat(response.getAccessToken())
                .isEqualTo("meetback-access-token");

        assertThat(response.getRefreshToken())
                .isEqualTo("meetback-refresh-token");

        assertThat(response.getRole())
                .isEqualTo("USER");
    }

    @Test
    void returningGoogleUserLogsInWithoutCreatingAnotherUser() {

        GoogleUserInfo googleUserInfo =
                googleUserInfo(
                        "existing-google-sub",
                        "existing@example.com",
                        "Existing User"
                );

        Social social =
                new Social();

        social.setUserId(20L);


        when(googleIdentityProvider.verifyIdToken("google-id-token"))
                .thenReturn(googleUserInfo);

        when(socialMapper.selectByProviderAndProviderId(
                "GOOGLE",
                googleUserInfo.getProviderId()
        )).thenReturn(social);

        stubTokenIssue(20L);


        LoginResponse response =
                authService.googleLogin(
                        googleLoginRequest()
                );


        assertThat(response.getUserId())
                .isEqualTo(20L);

        verify(userMapper, never())
                .insertUser(any(User.class));

        verify(socialMapper, never())
                .insertSocial(any(Social.class));
    }

    @Test
    void sameEmailAccountIsNotLinkedWithoutExistingAccountVerification() {

        GoogleUserInfo googleUserInfo =
                googleUserInfo(
                        "new-google-sub",
                        "already-used@example.com",
                        "Google User"
                );

        User existingUser =
                new User();

        existingUser.setUserId(30L);


        when(googleIdentityProvider.verifyIdToken("google-id-token"))
                .thenReturn(googleUserInfo);

        when(socialMapper.selectByProviderAndProviderId(
                "GOOGLE",
                googleUserInfo.getProviderId()
        )).thenReturn(null);

        when(userMapper.selectByEmail(googleUserInfo.getEmail()))
                .thenReturn(existingUser);


        assertThatThrownBy(() ->
                authService.googleLogin(
                        googleLoginRequest()
                )
        )
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("같은 이메일의 기존 계정");

        verify(userMapper, never())
                .insertUser(any(User.class));

        verify(socialMapper, never())
                .insertSocial(any(Social.class));
    }

    @Test
    void duplicateGoogleProfileNameGetsStableProviderSuffix() {

        GoogleUserInfo googleUserInfo =
                googleUserInfo(
                        "1234567890123456",
                        "duplicate-name@example.com",
                        "Same Name"
                );

        when(googleIdentityProvider.verifyIdToken("google-id-token"))
                .thenReturn(googleUserInfo);

        when(socialMapper.selectByProviderAndProviderId(
                "GOOGLE",
                googleUserInfo.getProviderId()
        )).thenReturn(null);

        when(userMapper.selectByEmail(googleUserInfo.getEmail()))
                .thenReturn(null);

        when(userMapper.existByNickname("Same Name"))
                .thenReturn(1);

        when(userMapper.existByNickname("Same Name_g90123456"))
                .thenReturn(0);

        doAnswer(invocation -> {

            User user =
                    invocation.getArgument(0);

            user.setUserId(40L);

            return 1;

        }).when(userMapper).insertUser(any(User.class));

        stubTokenIssue(40L);


        authService.googleLogin(
                googleLoginRequest()
        );


        ArgumentCaptor<User> userCaptor =
                ArgumentCaptor.forClass(User.class);

        verify(userMapper).insertUser(
                userCaptor.capture()
        );

        assertThat(userCaptor.getValue().getNickname())
                .isEqualTo("Same Name_g90123456");
    }

    private GoogleUserInfo googleUserInfo(
            String providerId,
            String email,
            String nickname
    ) {

        GoogleUserInfo userInfo =
                new GoogleUserInfo();

        userInfo.setProviderId(providerId);
        userInfo.setEmail(email);
        userInfo.setEmailVerified(true);
        userInfo.setNickname(nickname);

        return userInfo;
    }

    private GoogleLoginRequest googleLoginRequest() {

        GoogleLoginRequest request =
                new GoogleLoginRequest();

        request.setCredential(
                "google-id-token"
        );

        return request;
    }

    private void stubTokenIssue(
            Long userId
    ) {

        User updatedUser =
                new User();

        updatedUser.setUserId(userId);
        updatedUser.setRole("USER");
        updatedUser.setTokenVersion(1);

        when(userMapper.increaseTokenVersion(userId))
                .thenReturn(1);

        when(userMapper.selectById(userId))
                .thenReturn(updatedUser);

        when(jwtProvider.createAccessToken(
                userId,
                "USER",
                1
        )).thenReturn("meetback-access-token");

        when(jwtProvider.createRefreshToken(
                userId,
                "USER",
                1
        )).thenReturn("meetback-refresh-token");

        when(jwtProvider.getRefreshTokenExpirationSeconds())
                .thenReturn(3600L);

        when(refreshTokenMapper.selectByUserId(userId))
                .thenReturn(null);
    }
}
