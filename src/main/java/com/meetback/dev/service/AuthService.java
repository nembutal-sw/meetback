package com.meetback.dev.service;

import com.meetback.dev.domain.RefreshToken;
import com.meetback.dev.domain.Social;
import com.meetback.dev.domain.User;
import com.meetback.dev.dto.auth.*;
import com.meetback.dev.oauth.GoogleIdentityProvider;
import com.meetback.dev.oauth.GoogleUserInfo;
import com.meetback.dev.oauth.KakaoOAuthProvider;
import com.meetback.dev.oauth.KakaoUserInfo;
import com.meetback.dev.repository.RefreshTokenMapper;
import com.meetback.dev.repository.SocialMapper;
import com.meetback.dev.repository.UserMapper;
import com.meetback.dev.security.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final UserMapper userMapper;
    private final SocialMapper socialMapper;
    private final RefreshTokenMapper refreshTokenMapper;

    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final KakaoOAuthProvider kakaoOAuthProvider;
    private final GoogleIdentityProvider googleIdentityProvider;


    // =========================================================
    // 회원가입
    // =========================================================

    public void signup(SignupRequest request) {

        if (request == null) {
            throw new IllegalArgumentException(
                    "회원가입 요청이 없습니다."
            );
        }

        if (request.getEmail() == null
                || request.getEmail().isBlank()) {

            throw new IllegalArgumentException(
                    "이메일을 입력해주세요."
            );
        }

        if (request.getNickname() == null
                || request.getNickname().isBlank()) {

            throw new IllegalArgumentException(
                    "닉네임을 입력해주세요."
            );
        }

        if (request.getPassword() == null
                || request.getPassword().isBlank()) {

            throw new IllegalArgumentException(
                    "비밀번호를 입력해주세요."
            );
        }

        // 이메일 중복 검사
        if (userMapper.existByEmail(
                request.getEmail()
        ) > 0) {

            throw new IllegalArgumentException(
                    "이미 사용 중인 이메일입니다."
            );
        }

        // 닉네임 중복 검사
        if (userMapper.existByNickname(
                request.getNickname()
        ) > 0) {

            throw new IllegalArgumentException(
                    "이미 사용 중인 닉네임입니다."
            );
        }

        User user = new User();

        user.setEmail(
                request.getEmail()
        );

        user.setNickname(
                request.getNickname()
        );

        user.setPasswordHash(
                passwordEncoder.encode(
                        request.getPassword()
                )
        );

        user.setRole(
                "user"
        );

        // 최초 가입 시 0
        user.setTokenVersion(
                0
        );

        userMapper.insertUser(
                user
        );
    }


    // =========================================================
    // 일반 로그인
    // =========================================================

    public LoginResponse login(LoginRequest request) {

        if (request == null) {
            throw new IllegalArgumentException(
                    "로그인 요청이 없습니다."
            );
        }

        if (request.getEmail() == null
                || request.getEmail().isBlank()
                || request.getPassword() == null
                || request.getPassword().isBlank()) {

            throw new IllegalArgumentException(
                    "이메일 또는 비밀번호가 일치하지 않습니다."
            );
        }

        User user =
                userMapper.selectByEmail(
                        request.getEmail()
                );

        if (user == null) {
            throw new IllegalArgumentException(
                    "이메일 또는 비밀번호가 일치하지 않습니다."
            );
        }

        validateWithdrawalStatus(
                user
        );

        if (user.getPasswordHash() == null
                || !passwordEncoder.matches(
                request.getPassword(),
                user.getPasswordHash()
        )) {

            throw new IllegalArgumentException(
                    "이메일 또는 비밀번호가 일치하지 않습니다."
            );
        }

        /*
         * ★ 여기서 issueLoginToken() 호출
         *
         * issueLoginToken 안에서
         * tokenVersion을 증가시킨 뒤
         * Access / Refresh Token을 발급한다.
         */
        LoginToken loginToken =
                issueLoginToken(
                        user
                );

        LoginResponse response =
                new LoginResponse();

        response.setAccessToken(
                loginToken.accessToken()
        );

        response.setRefreshToken(
                loginToken.refreshToken()
        );

        response.setUserId(
                user.getUserId()
        );

        response.setRole(
                user.getRole()
        );

        return response;
    }


    // =========================================================
    // 카카오 로그인
    // =========================================================

    public KakaoLoginResponse kakaoLogin(
            KakaoLoginRequest request
    ) {

        if (request == null
                || request.getCode() == null
                || request.getCode().isBlank()) {

            throw new IllegalArgumentException(
                    "카카오 로그인 인가 코드가 없습니다."
            );
        }

        String kakaoAccessToken =
                kakaoOAuthProvider.requestAccessToken(
                        request.getCode()
                );

        KakaoUserInfo kakaoUserInfo =
                kakaoOAuthProvider.getUserInfo(
                        kakaoAccessToken
                );

        if (kakaoUserInfo == null
                || kakaoUserInfo.getProviderId() == null
                || kakaoUserInfo.getProviderId().isBlank()) {

            throw new IllegalArgumentException(
                    "카카오 사용자 정보를 확인할 수 없습니다."
            );
        }

        Social social =
                socialMapper.selectByProviderAndProviderId(
                        "KAKAO",
                        kakaoUserInfo.getProviderId()
                );

        User user;

        // 기존 카카오 사용자
        if (social != null) {

            user =
                    userMapper.selectById(
                            social.getUserId()
                    );

            if (user == null) {
                throw new IllegalArgumentException(
                        "사용자 정보를 찾을 수 없습니다."
                );
            }

        } else {

            // 같은 이메일의 기존 회원 조회
            user =
                    userMapper.selectByEmail(
                            kakaoUserInfo.getEmail()
                    );

            // 신규 카카오 회원
            if (user == null) {

                user =
                        new User();

                user.setEmail(
                        kakaoUserInfo.getEmail()
                );

                user.setNickname(
                        kakaoUserInfo.getName()
                );

                user.setRole(
                        "user"
                );

                user.setTokenVersion(
                        0
                );

                userMapper.insertUser(
                        user
                );
            }

            Social newSocial =
                    new Social();

            newSocial.setUserId(
                    user.getUserId()
            );

            newSocial.setProvider(
                    "KAKAO"
            );

            newSocial.setProviderId(
                    kakaoUserInfo.getProviderId()
            );

            newSocial.setEmail(
                    kakaoUserInfo.getEmail()
            );

            newSocial.setEmailVerified(
                    kakaoUserInfo.getEmailVerified()
            );

            newSocial.setName(
                    kakaoUserInfo.getName()
            );

            socialMapper.insertSocial(
                    newSocial
            );
        }

        validateWithdrawalStatus(
                user
        );

        /*
         * ★ 카카오 로그인도 여기서
         * tokenVersion 증가
         */
        LoginToken loginToken =
                issueLoginToken(
                        user
                );

        KakaoLoginResponse response =
                new KakaoLoginResponse();

        response.setAccessToken(
                loginToken.accessToken()
        );

        response.setRefreshToken(
                loginToken.refreshToken()
        );

        response.setUserId(
                user.getUserId()
        );

        response.setRole(
                user.getRole()
        );

        return response;
    }


    // =========================================================
    // 구글 로그인
    // =========================================================

    public LoginResponse googleLogin(
            GoogleLoginRequest request
    ) {

        if (request == null) {
            throw new IllegalArgumentException(
                    "Google 로그인 요청이 없습니다."
            );
        }

        if (request.getCredential() == null
                || request.getCredential().isBlank()) {

            throw new IllegalArgumentException(
                    "Google 인증 정보가 없습니다."
            );
        }

        GoogleUserInfo googleUserInfo =
                googleIdentityProvider.verifyIdToken(
                        request.getCredential()
                );

        if (googleUserInfo == null
                || googleUserInfo.getProviderId() == null
                || googleUserInfo.getProviderId().isBlank()) {

            throw new IllegalArgumentException(
                    "Google 사용자 정보를 확인할 수 없습니다."
            );
        }

        Social social =
                socialMapper.selectByProviderAndProviderId(
                        "GOOGLE",
                        googleUserInfo.getProviderId()
                );

        User user;

        // 기존 Google 사용자
        if (social != null) {

            user =
                    userMapper.selectById(
                            social.getUserId()
                    );

            if (user == null) {
                throw new IllegalArgumentException(
                        "사용자 정보를 찾을 수 없습니다."
                );
            }

        } else {

            User existingUser =
                    userMapper.selectByEmail(
                            googleUserInfo.getEmail()
                    );

            if (existingUser != null) {
                throw new IllegalStateException(
                        "같은 이메일의 기존 계정이 있습니다. "
                                + "기존 계정으로 로그인한 뒤 Google 계정을 연결해주세요."
                );
            }

            user =
                    new User();

            user.setEmail(
                    googleUserInfo.getEmail()
            );

            user.setNickname(
                    createGoogleNickname(
                            googleUserInfo
                    )
            );

            user.setRole(
                    "user"
            );

            user.setTokenVersion(
                    0
            );

            userMapper.insertUser(
                    user
            );

            Social newSocial =
                    new Social();

            newSocial.setUserId(
                    user.getUserId()
            );

            newSocial.setProvider(
                    "GOOGLE"
            );

            newSocial.setProviderId(
                    googleUserInfo.getProviderId()
            );

            newSocial.setEmail(
                    googleUserInfo.getEmail()
            );

            newSocial.setEmailVerified(
                    googleUserInfo.getEmailVerified()
            );

            socialMapper.insertSocial(
                    newSocial
            );
        }

        validateWithdrawalStatus(
                user
        );

        /*
         * ★ 구글 로그인도 여기서
         * tokenVersion 증가
         */
        LoginToken loginToken =
                issueLoginToken(
                        user
                );

        LoginResponse response =
                new LoginResponse();

        response.setAccessToken(
                loginToken.accessToken()
        );

        response.setRefreshToken(
                loginToken.refreshToken()
        );

        response.setUserId(
                user.getUserId()
        );

        response.setRole(
                user.getRole()
        );

        return response;
    }


    // =========================================================
    // Google 닉네임 생성
    // =========================================================

    private String createGoogleNickname(
            GoogleUserInfo googleUserInfo
    ) {

        String baseNickname =
                googleUserInfo.getNickname();

        if (baseNickname == null
                || baseNickname.isBlank()) {

            baseNickname =
                    "GoogleUser";
        }

        baseNickname =
                baseNickname.trim();

        if (baseNickname.length() > 255) {

            baseNickname =
                    baseNickname.substring(
                            0,
                            255
                    );
        }

        if (userMapper.existByNickname(
                baseNickname
        ) == 0) {

            return baseNickname;
        }

        String providerId =
                googleUserInfo.getProviderId();

        String idSuffix =
                providerId.substring(
                        Math.max(
                                0,
                                providerId.length() - 8
                        )
                );

        String suffix =
                "_g" + idSuffix;

        int maxBaseLength =
                255 - suffix.length();

        String uniqueNickname =
                baseNickname.substring(
                        0,
                        Math.min(
                                baseNickname.length(),
                                maxBaseLength
                        )
                )
                        + suffix;

        if (userMapper.existByNickname(
                uniqueNickname
        ) > 0) {

            throw new IllegalStateException(
                    "Google 프로필 이름으로 닉네임을 생성할 수 없습니다."
            );
        }

        return uniqueNickname;
    }


    // =========================================================
    // Refresh Token 재발급
    // =========================================================

    public TokenRefreshResponse refresh(
            TokenRefreshRequest request
    ) {

        if (request == null
                || request.getRefreshToken() == null
                || request.getRefreshToken().isBlank()) {

            throw new IllegalArgumentException(
                    "Refresh Token이 필요합니다."
            );
        }

        String requestRefreshToken =
                request.getRefreshToken();

        if (!jwtProvider.validateToken(
                requestRefreshToken
        )) {

            throw new IllegalArgumentException(
                    "유효하지 않은 Refresh Token입니다."
            );
        }

        if (!"REFRESH".equals(
                jwtProvider.getTokenType(
                        requestRefreshToken
                )
        )) {

            throw new IllegalArgumentException(
                    "Refresh Token이 아닙니다."
            );
        }

        Long userId =
                jwtProvider.getUserId(
                        requestRefreshToken
                );

        Integer requestTokenVersion =
                jwtProvider.getTokenVersion(
                        requestRefreshToken
                );

        if (userId == null
                || requestTokenVersion == null) {

            throw new IllegalArgumentException(
                    "Refresh Token 사용자 정보가 올바르지 않습니다."
            );
        }

        String tokenHash =
                hashToken(
                        requestRefreshToken
                );

        RefreshToken savedToken =
                refreshTokenMapper.selectByTokenHash(
                        tokenHash
                );

        /*
         * 다른 기기에서 새로 로그인하면
         * DB Refresh Token이 새 토큰으로 교체되기 때문에
         * 기존 기기의 Refresh Token은 여기서 걸린다.
         */
        if (savedToken == null) {

            throw new IllegalArgumentException(
                    "등록되지 않은 Refresh Token입니다."
            );
        }

        if (savedToken.getUserId() == null
                || !savedToken.getUserId().equals(
                userId
        )) {

            throw new IllegalArgumentException(
                    "Refresh Token 사용자 정보가 일치하지 않습니다."
            );
        }

        if (savedToken.getExpiresAt() == null
                || !savedToken.getExpiresAt()
                .isAfter(
                        LocalDateTime.now()
                )) {

            refreshTokenMapper.deleteByUserId(
                    userId
            );

            throw new IllegalArgumentException(
                    "만료된 Refresh Token입니다."
            );
        }

        User user =
                userMapper.selectById(
                        userId
                );

        if (user == null) {

            refreshTokenMapper.deleteByUserId(
                    userId
            );

            throw new IllegalArgumentException(
                    "사용자를 찾을 수 없습니다."
            );
        }

        // ★ Token Version 확인
        if (user.getTokenVersion() == null
                || !requestTokenVersion.equals(
                user.getTokenVersion()
        )) {

            refreshTokenMapper.deleteByUserId(
                    userId
            );

            throw new IllegalArgumentException(
                    "무효화된 Refresh Token입니다."
            );
        }

        if (isWithdrawalExpired(
                user
        )) {

            refreshTokenMapper.deleteByUserId(
                    user.getUserId()
            );

            throw new IllegalArgumentException(
                    "회원탈퇴 유예기간이 만료되었습니다."
            );
        }

        LoginToken loginToken =
                issueRotatedToken(
                        user,
                        tokenHash
                );

        TokenRefreshResponse response =
                new TokenRefreshResponse();

        response.setAccessToken(
                loginToken.accessToken()
        );

        response.setRefreshToken(
                loginToken.refreshToken()
        );

        return response;
    }


    // =========================================================
    // 현재 로그인 사용자 조회
    // =========================================================

    public AuthCheckResponse getCurrentUser(
            Long userId
    ) {

        if (userId == null) {
            throw new IllegalArgumentException(
                    "사용자 정보가 없습니다."
            );
        }

        User user =
                userMapper.selectById(
                        userId
                );

        if (user == null) {
            throw new IllegalArgumentException(
                    "사용자를 찾을 수 없습니다."
            );
        }

        return new AuthCheckResponse(
                user.getUserId(),
                user.getEmail(),
                user.getNickname(),
                user.getRole()
        );
    }


    // =========================================================
    // 로그아웃
    // =========================================================

    public void logout(
            Long userId
    ) {

        if (userId == null) {
            throw new IllegalArgumentException(
                    "사용자 정보가 없습니다."
            );
        }

        // Refresh Token 제거
        refreshTokenMapper.deleteByUserId(
                userId
        );

        /*
         * ★ 로그아웃 시에도 버전 증가
         *
         * 이미 발급되어 있던 Access Token을
         * 무효화시키기 위한 처리
         */
        int updatedRows =
                userMapper.increaseTokenVersion(
                        userId
                );

        if (updatedRows == 0) {
            throw new IllegalArgumentException(
                    "사용자를 찾을 수 없습니다."
            );
        }
    }


    // =========================================================
    // 회원탈퇴 요청
    // =========================================================

    public void withdraw(
            Long userId
    ) {

        if (userId == null) {
            throw new IllegalArgumentException(
                    "사용자 정보가 없습니다."
            );
        }

        User user =
                userMapper.selectById(
                        userId
                );

        if (user == null) {
            throw new IllegalArgumentException(
                    "존재하지 않는 사용자입니다."
            );
        }

        if (user.getDeletedAt() != null) {
            throw new IllegalArgumentException(
                    "이미 회원탈퇴가 요청된 상태입니다."
            );
        }

        userMapper.withdrawUser(
                userId
        );
    }


    // =========================================================
    // 회원탈퇴 취소
    // =========================================================

    public void cancelWithdrawal(
            Long userId
    ) {

        if (userId == null) {
            throw new IllegalArgumentException(
                    "사용자 정보가 없습니다."
            );
        }

        User user =
                userMapper.selectById(
                        userId
                );

        if (user == null) {
            throw new IllegalArgumentException(
                    "존재하지 않는 사용자입니다."
            );
        }

        if (user.getDeletedAt() == null) {
            throw new IllegalArgumentException(
                    "회원탈퇴 요청 상태가 아닙니다."
            );
        }

        LocalDateTime withdrawalDeadline =
                user.getDeletedAt()
                        .plusDays(
                                7
                        );

        if (!LocalDateTime.now()
                .isBefore(
                        withdrawalDeadline
                )) {

            throw new IllegalArgumentException(
                    "회원탈퇴 취소 기간이 만료되었습니다."
            );
        }

        userMapper.cancelWithdrawal(
                userId
        );
    }


    // =========================================================
    // Access / Refresh Token 발급
    // ★ 여기 핵심 수정
    // =========================================================

    private LoginToken issueLoginToken(
            User user
    ) {

        validateTokenUser(
                user
        );

        /*
         * ★★★★★★★★★★★★★★★★★★★★★★★★★★★★★
         *
         * 새 로그인이 발생할 때마다
         * DB token_version + 1
         *
         * A 로그인 version = 5
         * B 로그인 version = 6
         *
         * 그러면 A의 기존 토큰은
         * DB version과 달라져서 무효화된다.
         *
         * ★★★★★★★★★★★★★★★★★★★★★★★★★★★★★
         */
        int updatedRows =
                userMapper.increaseTokenVersion(
                        user.getUserId()
                );

        if (updatedRows == 0) {
            throw new IllegalArgumentException(
                    "사용자를 찾을 수 없습니다."
            );
        }

        /*
         * ★ 반드시 DB에서 다시 조회
         *
         * 기존 user 객체에는
         * 증가 전 tokenVersion이 들어 있기 때문
         */
        User updatedUser =
                userMapper.selectById(
                        user.getUserId()
                );

        if (updatedUser == null) {
            throw new IllegalArgumentException(
                    "사용자를 찾을 수 없습니다."
            );
        }

        validateTokenUser(
                updatedUser
        );

        // 기존 user 객체에도 최신 버전 반영
        user.setTokenVersion(
                updatedUser.getTokenVersion()
        );

        String accessToken =
                jwtProvider.createAccessToken(
                        updatedUser.getUserId(),
                        updatedUser.getRole(),
                        updatedUser.getTokenVersion()
                );

        String refreshToken =
                jwtProvider.createRefreshToken(
                        updatedUser.getUserId(),
                        updatedUser.getRole(),
                        updatedUser.getTokenVersion()
                );

        /*
         * 같은 userId의 RefreshToken이 이미 존재하면
         * saveRefreshToken()에서 UPDATE 된다.
         *
         * 따라서 A의 RefreshToken도 교체된다.
         */
        saveRefreshToken(
                updatedUser,
                refreshToken
        );

        return new LoginToken(
                accessToken,
                refreshToken
        );
    }


    // =========================================================
    // Refresh Token Rotation 발급
    // =========================================================

    private LoginToken issueRotatedToken(
            User user,
            String oldTokenHash
    ) {

        validateTokenUser(
                user
        );

        /*
         * Refresh 재발급에서는 tokenVersion을 올리지 않는다.
         *
         * 같은 로그인 세션 안에서
         * Refresh Token만 Rotation 하는 것이기 때문.
         */
        String accessToken =
                jwtProvider.createAccessToken(
                        user.getUserId(),
                        user.getRole(),
                        user.getTokenVersion()
                );

        String refreshToken =
                jwtProvider.createRefreshToken(
                        user.getUserId(),
                        user.getRole(),
                        user.getTokenVersion()
                );

        String newTokenHash =
                hashToken(
                        refreshToken
                );

        LocalDateTime expiresAt =
                createRefreshTokenExpiresAt(
                        user
                );

        int updatedRows =
                refreshTokenMapper.rotateRefreshToken(
                        user.getUserId(),
                        oldTokenHash,
                        newTokenHash,
                        expiresAt
                );

        if (updatedRows != 1) {
            throw new IllegalArgumentException(
                    "이미 사용되었거나 무효화된 Refresh Token입니다."
            );
        }

        return new LoginToken(
                accessToken,
                refreshToken
        );
    }


    // =========================================================
    // Token 발급 사용자 확인
    // =========================================================

    private void validateTokenUser(
            User user
    ) {

        if (user == null
                || user.getUserId() == null) {

            throw new IllegalArgumentException(
                    "사용자 정보를 확인할 수 없습니다."
            );
        }

        if (user.getRole() == null
                || user.getRole().isBlank()) {

            throw new IllegalStateException(
                    "사용자 권한 정보가 없습니다."
            );
        }

        if (user.getTokenVersion() == null) {

            throw new IllegalStateException(
                    "사용자 Token Version 정보가 없습니다."
            );
        }
    }


    // =========================================================
    // 탈퇴 상태 확인
    // =========================================================

    private void validateWithdrawalStatus(
            User user
    ) {

        if (isWithdrawalExpired(
                user
        )) {

            throw new IllegalArgumentException(
                    "탈퇴 처리된 계정입니다."
            );
        }
    }


    // =========================================================
    // 탈퇴 유예기간 만료 여부
    // =========================================================

    private boolean isWithdrawalExpired(
            User user
    ) {

        if (user == null
                || user.getDeletedAt() == null) {

            return false;
        }

        LocalDateTime withdrawalDeadline =
                user.getDeletedAt()
                        .plusDays(
                                7
                        );

        return !LocalDateTime.now()
                .isBefore(
                        withdrawalDeadline
                );
    }


    // =========================================================
    // Refresh Token 저장 / 갱신
    // =========================================================

    private void saveRefreshToken(
            User user,
            String refreshToken
    ) {

        String tokenHash =
                hashToken(
                        refreshToken
                );

        RefreshToken existingToken =
                refreshTokenMapper.selectByUserId(
                        user.getUserId()
                );

        RefreshToken token =
                new RefreshToken();

        token.setUserId(
                user.getUserId()
        );

        token.setTokenHash(
                tokenHash
        );

        token.setExpiresAt(
                createRefreshTokenExpiresAt(
                        user
                )
        );

        if (existingToken == null) {

            refreshTokenMapper.insertRefreshToken(
                    token
            );

        } else {

            /*
             * ★ B가 로그인하면
             * A의 Refresh Token을
             * 여기서 B의 Refresh Token으로 교체
             */
            refreshTokenMapper.updateRefreshToken(
                    token
            );
        }
    }


    // =========================================================
    // Refresh Token 만료시간 생성
    // =========================================================

    private LocalDateTime createRefreshTokenExpiresAt(
            User user
    ) {

        LocalDateTime expiresAt =
                LocalDateTime.now()
                        .plusSeconds(
                                jwtProvider
                                        .getRefreshTokenExpirationSeconds()
                        );

        if (user.getDeletedAt() != null) {

            LocalDateTime withdrawalDeadline =
                    user.getDeletedAt()
                            .plusDays(
                                    7
                            );

            if (expiresAt.isAfter(
                    withdrawalDeadline
            )) {

                expiresAt =
                        withdrawalDeadline;
            }
        }

        return expiresAt;
    }


    // =========================================================
    // Refresh Token SHA-256 Hash
    // =========================================================

    private String hashToken(
            String token
    ) {

        try {

            MessageDigest digest =
                    MessageDigest.getInstance(
                            "SHA-256"
                    );

            byte[] hash =
                    digest.digest(
                            token.getBytes(
                                    StandardCharsets.UTF_8
                            )
                    );

            return HexFormat.of()
                    .formatHex(
                            hash
                    );

        } catch (NoSuchAlgorithmException e) {

            throw new IllegalStateException(
                    "Refresh Token 해시 생성에 실패했습니다.",
                    e
            );
        }
    }


    // =========================================================
    // 내부 Token 객체
    // =========================================================

    private record LoginToken(
            String accessToken,
            String refreshToken
    ) {
    }
}