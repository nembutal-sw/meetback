package com.meetback.dev.service;

import com.meetback.dev.domain.RefreshToken;
import com.meetback.dev.domain.Social;
import com.meetback.dev.domain.User;
import com.meetback.dev.domain.UserStatus;
import com.meetback.dev.dto.auth.*;
import com.meetback.dev.exception.AccountSuspendedException;
import com.meetback.dev.oauth.GoogleIdentityProvider;
import com.meetback.dev.oauth.GoogleUserInfo;
import com.meetback.dev.oauth.KakaoOAuthProvider;
import com.meetback.dev.oauth.KakaoUserInfo;
import com.meetback.dev.repository.RefreshTokenMapper;
import com.meetback.dev.repository.SocialMapper;
import com.meetback.dev.repository.UserMapper;
import com.meetback.dev.security.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

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
    private final MailService mailService;
    private final TermService termService;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;


    private static final Pattern EMAIL_PATTERN =
            Pattern.compile(
                    "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"
            );


    private static final Pattern PASSWORD_PATTERN =
            Pattern.compile(
                    "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[^A-Za-z0-9\\s])\\S{8,20}$"
            );


    private static final Pattern NICKNAME_PATTERN =
            Pattern.compile(
                    "^[가-힣A-Za-z0-9_]{2,12}$"
            );


    private static final List<String> BLOCKED_NICKNAME_WORDS =
            List.of(
                    "시발",
                    "씨발",
                    "시팔",
                    "씨팔",
                    "병신",
                    "븅신",
                    "개새끼",
                    "개새",
                    "새끼",
                    "미친",
                    "미친놈",
                    "미친년",
                    "좆",
                    "존나",
                    "졸라",
                    "지랄",
                    "염병",
                    "꺼져",
                    "닥쳐",
                    "fuck",
                    "fucking",
                    "shit",
                    "bitch"
            );


    private static final List<String> RESERVED_NICKNAMES =
            List.of(
                    "admin",
                    "administrator",
                    "manager",
                    "master",
                    "root",
                    "meetback",
                    "관리자",
                    "운영자",
                    "매니저",
                    "마스터"
            );


    // =========================================================
    // 회원가입
    // =========================================================

    public void signup(
            SignupRequest request
    ) {

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


        String normalizedEmail =
                normalizeEmail(
                        request.getEmail()
                );


        validateEmail(
                normalizedEmail
        );


        validatePassword(
                request.getPassword()
        );


        validateNickname(
                request.getNickname()
        );


        // 필수 약관 동의 검사
        termService.validateRequiredTerms(
                request.getAgreedTermIds()
        );


        // 이메일 중복 검사
        if (userMapper.existByEmail(
                normalizedEmail
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


        User user =
                new User();


        user.setEmail(
                normalizedEmail
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


        // 신규 계정은 정상 이용 상태로 시작한다.
        user.setStatus(
                UserStatus.ACTIVE
        );


        userMapper.insertUser(
                user
        );


        // 회원 약관 동의 저장
        termService.saveAgreements(
                user.getUserId(),
                request.getAgreedTermIds()
        );
    }


    // =========================================================
    // 이메일 중복 검사
    // =========================================================

    @Transactional(readOnly = true)
    public boolean isEmailAvailable(
            String email
    ) {

        if (email == null
                || email.isBlank()) {

            throw new IllegalArgumentException(
                    "이메일을 입력해주세요."
            );
        }


        String normalizedEmail =
                normalizeEmail(
                        email
                );


        validateEmail(
                normalizedEmail
        );


        return userMapper.existByEmail(
                normalizedEmail
        ) == 0;
    }


    // =========================================================
    // 닉네임 중복 검사
    // =========================================================

    @Transactional(readOnly = true)
    public boolean isNicknameAvailable(
            String nickname
    ) {

        if (!isValidNickname(
                nickname
        )) {

            return false;
        }


        return userMapper.existByNickname(
                nickname
        ) == 0;
    }


    // =========================================================
    // 아이디 찾기
    // =========================================================

    @Transactional(readOnly = true)
    public String findEmail(
            String nickname
    ) {

        if (nickname == null
                || nickname.isBlank()) {

            throw new IllegalArgumentException(
                    "닉네임을 입력해주세요."
            );
        }


        User user =
                userMapper.selectByNickname(
                        nickname.trim()
                );


        if (user == null
                || user.getEmail() == null
                || user.getEmail().isBlank()) {

            throw new IllegalArgumentException(
                    "일치하는 회원 정보를 찾을 수 없습니다."
            );
        }


        return maskEmail(
                user.getEmail()
        );
    }


    // =========================================================
    // 비밀번호 재설정 이메일 요청
    // =========================================================

    public void requestPasswordReset(
            String email
    ) {

        if (email == null
                || email.isBlank()) {

            throw new IllegalArgumentException(
                    "이메일을 입력해주세요."
            );
        }


        String normalizedEmail =
                normalizeEmail(
                        email
                );


        validateEmail(
                normalizedEmail
        );


        User user =
                userMapper.selectByEmail(
                        normalizedEmail
                );


        // 존재하지 않는 이메일 여부를 외부에 노출하지 않는다
        if (user == null) {
            return;
        }


        // 소셜 로그인 전용 계정
        if (user.getPasswordHash() == null
                || user.getPasswordHash().isBlank()) {

            return;
        }


        if (user.getTokenVersion() == null) {
            return;
        }


        if (isWithdrawalExpired(
                user
        )) {

            return;
        }


        String resetToken =
                jwtProvider.createPasswordResetToken(
                        user.getUserId(),
                        user.getTokenVersion()
                );


        String encodedToken =
                URLEncoder.encode(
                        resetToken,
                        StandardCharsets.UTF_8
                );


        String resetLink =
                baseUrl
                        + "/reset-password?token="
                        + encodedToken;


        mailService.sendPasswordResetEmail(
                user.getEmail(),
                resetLink
        );
    }


    // =========================================================
    // 비밀번호 재설정 완료
    // =========================================================

    public void confirmPasswordReset(
            String resetToken,
            String newPassword
    ) {

        if (resetToken == null
                || resetToken.isBlank()) {

            throw new IllegalArgumentException(
                    "비밀번호 재설정 Token이 없습니다."
            );
        }


        if (newPassword == null
                || newPassword.isBlank()) {

            throw new IllegalArgumentException(
                    "새 비밀번호를 입력해주세요."
            );
        }


        validatePassword(
                newPassword
        );


        if (!jwtProvider.validateToken(
                resetToken
        )) {

            throw new IllegalArgumentException(
                    "유효하지 않거나 만료된 비밀번호 재설정 링크입니다."
            );
        }


        if (!"PASSWORD_RESET".equals(
                jwtProvider.getTokenType(
                        resetToken
                )
        )) {

            throw new IllegalArgumentException(
                    "비밀번호 재설정 Token이 아닙니다."
            );
        }


        Long userId =
                jwtProvider.getUserId(
                        resetToken
                );


        Integer requestTokenVersion =
                jwtProvider.getTokenVersion(
                        resetToken
                );


        if (userId == null
                || requestTokenVersion == null) {

            throw new IllegalArgumentException(
                    "비밀번호 재설정 사용자 정보가 올바르지 않습니다."
            );
        }


        User user =
                userMapper.selectById(
                        userId
                );


        if (user == null
                || user.getTokenVersion() == null) {

            throw new IllegalArgumentException(
                    "사용자를 찾을 수 없습니다."
            );
        }


        validateWithdrawalStatus(
                user
        );


        if (!requestTokenVersion.equals(
                user.getTokenVersion()
        )) {

            throw new IllegalArgumentException(
                    "이미 사용되었거나 무효화된 비밀번호 재설정 링크입니다."
            );
        }


        String passwordHash =
                passwordEncoder.encode(
                        newPassword
                );


        int updatedRows =
                userMapper.updatePassword(
                        userId,
                        passwordHash
                );


        if (updatedRows != 1) {

            throw new IllegalStateException(
                    "비밀번호 변경에 실패했습니다."
            );
        }


        // 기존 Refresh Token 제거
        refreshTokenMapper.deleteByUserId(
                userId
        );


        // 기존 Access Token과 재설정 Token 무효화
        int versionUpdatedRows =
                userMapper.increaseTokenVersion(
                        userId
                );


        if (versionUpdatedRows != 1) {

            throw new IllegalStateException(
                    "Token Version 변경에 실패했습니다."
            );
        }
    }


    // =========================================================
    // 일반 로그인
    // =========================================================

    public LoginResponse login(
            LoginRequest request
    ) {

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


        String loginId =
                normalizeEmail(
                        request.getEmail()
                );


        User user =
                userMapper.selectByEmailForUpdate(
                        loginId
                );


        if (user == null) {

            throw new IllegalArgumentException(
                    "이메일 또는 비밀번호가 일치하지 않습니다."
            );
        }


        // 비이메일 로그인 ID는 관리자 계정에만 허용한다.
        if (!EMAIL_PATTERN.matcher(loginId).matches()
                && !isAdminRole(user.getRole())) {

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


        validateActiveStatus(
                user
        );


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

    public SocialLoginResponse kakaoLogin(
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


        String normalizedEmail =
                normalizeEmail(
                        kakaoUserInfo.getEmail()
                );


        Social social =
                socialMapper.selectByProviderAndProviderId(
                        "KAKAO",
                        kakaoUserInfo.getProviderId()
                );


        // 기존 카카오 사용자
        if (social != null) {

            User user =
                    userMapper.selectById(
                            social.getUserId()
                    );


            if (user == null) {

                throw new IllegalArgumentException(
                        "사용자 정보를 찾을 수 없습니다."
                );
            }


            validateWithdrawalStatus(
                    user
            );


            validateActiveStatus(
                    user
            );


            return createSocialLoginSuccessResponse(
                    user
            );
        }


        User existingUser =
                null;


        // 같은 이메일의 기존 회원 조회
        if (normalizedEmail != null
                && !normalizedEmail.isBlank()) {

            existingUser =
                    userMapper.selectByEmail(
                            normalizedEmail
                    );
        }


        // 기존 회원 + 카카오 이메일 일치
        if (existingUser != null) {

            if (!Boolean.TRUE.equals(
                    kakaoUserInfo.getEmailVerified()
            )) {

                throw new IllegalStateException(
                        "카카오 이메일 인증 여부를 확인할 수 없어 "
                                + "기존 계정과 연결할 수 없습니다."
                );
            }


            validateWithdrawalStatus(
                    existingUser
            );


            validateActiveStatus(
                    existingUser
            );


            Social newSocial =
                    new Social();


            newSocial.setUserId(
                    existingUser.getUserId()
            );


            newSocial.setProvider(
                    "KAKAO"
            );


            newSocial.setProviderId(
                    kakaoUserInfo.getProviderId()
            );


            newSocial.setEmail(
                    normalizedEmail
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


            return createSocialLoginSuccessResponse(
                    existingUser
            );
        }


        // 신규 카카오 사용자는 바로 저장하지 않는다
        String signupToken =
                jwtProvider.createSocialSignupToken(
                        "KAKAO",
                        kakaoUserInfo.getProviderId(),
                        normalizedEmail,
                        kakaoUserInfo.getEmailVerified(),
                        kakaoUserInfo.getName()
                );


        return createNicknameRequiredResponse(
                signupToken
        );
    }


    // =========================================================
    // 구글 로그인
    // =========================================================

    public SocialLoginResponse googleLogin(
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


        String normalizedEmail =
                normalizeEmail(
                        googleUserInfo.getEmail()
                );


        Social social =
                socialMapper.selectByProviderAndProviderId(
                        "GOOGLE",
                        googleUserInfo.getProviderId()
                );


        // 기존 Google 사용자
        if (social != null) {

            User user =
                    userMapper.selectById(
                            social.getUserId()
                    );


            if (user == null) {

                throw new IllegalArgumentException(
                        "사용자 정보를 찾을 수 없습니다."
                );
            }


            validateWithdrawalStatus(
                    user
            );


            validateActiveStatus(
                    user
            );


            return createSocialLoginSuccessResponse(
                    user
            );
        }


        User existingUser =
                null;


        if (normalizedEmail != null
                && !normalizedEmail.isBlank()) {

            existingUser =
                    userMapper.selectByEmail(
                            normalizedEmail
                    );
        }


        // 기존 Google 정책 유지
        // 동일 이메일의 기존 계정과 자동 연결하지 않는다.
        if (existingUser != null) {

            throw new IllegalStateException(
                    "같은 이메일의 기존 계정이 있습니다. "
                            + "기존 계정으로 로그인한 뒤 Google 계정을 연결해주세요."
            );
        }


        String signupToken =
                jwtProvider.createSocialSignupToken(
                        "GOOGLE",
                        googleUserInfo.getProviderId(),
                        normalizedEmail,
                        googleUserInfo.getEmailVerified(),
                        googleUserInfo.getNickname()
                );


        return createNicknameRequiredResponse(
                signupToken
        );
    }


    // =========================================================
    // 소셜 신규 회원 가입 완료
    // =========================================================

    public SocialLoginResponse completeSocialSignup(
            String signupToken,
            String nickname,
            List<Long> agreedTermIds
    ) {

        if (signupToken == null
                || signupToken.isBlank()) {

            throw new IllegalArgumentException(
                    "소셜 회원가입 Token이 없습니다."
            );
        }


        if (nickname == null
                || nickname.isBlank()) {

            throw new IllegalArgumentException(
                    "닉네임을 입력해주세요."
            );
        }


        validateNickname(
                nickname
        );


        if (!jwtProvider.validateToken(
                signupToken
        )) {

            throw new IllegalArgumentException(
                    "유효하지 않거나 만료된 소셜 회원가입 정보입니다."
            );
        }


        if (!"SOCIAL_SIGNUP".equals(
                jwtProvider.getTokenType(
                        signupToken
                )
        )) {

            throw new IllegalArgumentException(
                    "소셜 회원가입 Token이 아닙니다."
            );
        }


        String provider =
                jwtProvider.getProvider(
                        signupToken
                );


        String providerId =
                jwtProvider.getSubject(
                        signupToken
                );


        String email =
                normalizeEmail(
                        jwtProvider.getEmail(
                                signupToken
                        )
                );


        Boolean emailVerified =
                jwtProvider.getEmailVerified(
                        signupToken
                );


        String name =
                jwtProvider.getName(
                        signupToken
                );


        if (!"KAKAO".equals(
                provider
        )
                && !"GOOGLE".equals(
                provider
        )) {

            throw new IllegalArgumentException(
                    "지원하지 않는 소셜 로그인입니다."
            );
        }


        if (providerId == null
                || providerId.isBlank()) {

            throw new IllegalArgumentException(
                    "소셜 사용자 정보가 없습니다."
            );
        }


        // 필수 약관 동의 검사
        termService.validateRequiredTerms(
                agreedTermIds
        );


        Social existingSocial =
                socialMapper.selectByProviderAndProviderId(
                        provider,
                        providerId
                );


        if (existingSocial != null) {

            throw new IllegalArgumentException(
                    "이미 가입된 소셜 계정입니다."
            );
        }


        String selectedNickname =
                nickname;


        // 최종 가입 시 다시 닉네임 중복 검사
        if (userMapper.existByNickname(
                selectedNickname
        ) > 0) {

            throw new IllegalArgumentException(
                    "이미 사용 중인 닉네임입니다."
            );
        }


        if (email != null
                && !email.isBlank()
                && userMapper.existByEmail(
                email
        ) > 0) {

            throw new IllegalArgumentException(
                    "같은 이메일의 기존 계정이 있습니다."
            );
        }


        User user =
                new User();


        user.setEmail(
                email
        );


        user.setNickname(
                selectedNickname
        );


        user.setRole(
                "user"
        );


        user.setTokenVersion(
                0
        );


        // 소셜 신규 계정도 정상 이용 상태로 시작한다.
        user.setStatus(
                UserStatus.ACTIVE
        );


        userMapper.insertUser(
                user
        );


        Social social =
                new Social();


        social.setUserId(
                user.getUserId()
        );


        social.setProvider(
                provider
        );


        social.setProviderId(
                providerId
        );


        social.setEmail(
                email
        );


        social.setEmailVerified(
                emailVerified
        );


        social.setName(
                name
        );


        socialMapper.insertSocial(
                social
        );


        // 소셜 회원 약관 동의 저장
        termService.saveAgreements(
                user.getUserId(),
                agreedTermIds
        );


        return createSocialLoginSuccessResponse(
                user
        );
    }


    // =========================================================
    // Google 닉네임 생성
    // 기존 코드 보존
    // 신규 Google 가입에서는 더 이상 호출하지 않는다.
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


        validateActiveStatus(
                user
        );


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


        refreshTokenMapper.deleteByUserId(
                userId
        );


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
    // 소셜 로그인 성공 응답
    // =========================================================

    private SocialLoginResponse createSocialLoginSuccessResponse(
            User user
    ) {

        LoginToken loginToken =
                issueLoginToken(
                        user
                );


        SocialLoginResponse response =
                new SocialLoginResponse();


        response.setStatus(
                "LOGIN_SUCCESS"
        );


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
    // 소셜 신규가입 닉네임 요청 응답
    // =========================================================

    private SocialLoginResponse createNicknameRequiredResponse(
            String signupToken
    ) {

        SocialLoginResponse response =
                new SocialLoginResponse();


        response.setStatus(
                "NICKNAME_REQUIRED"
        );


        response.setSignupToken(
                signupToken
        );


        return response;
    }


    // =========================================================
    // 이메일 정규화
    // =========================================================

    private String normalizeEmail(
            String email
    ) {

        if (email == null) {
            return null;
        }


        return email
                .trim()
                .toLowerCase(
                        Locale.ROOT
                );
    }


    // =========================================================
    // 이메일 형식 검사
    // =========================================================

    private void validateEmail(
            String email
    ) {

        if (email == null
                || email.isBlank()) {

            throw new IllegalArgumentException(
                    "이메일을 입력해주세요."
            );
        }


        if (!EMAIL_PATTERN.matcher(
                email
        ).matches()) {

            throw new IllegalArgumentException(
                    "올바른 이메일 형식을 입력해주세요."
            );
        }
    }


    // =========================================================
    // 비밀번호 정책 검사
    // =========================================================

    private void validatePassword(
            String password
    ) {

        if (password == null
                || password.isBlank()) {

            throw new IllegalArgumentException(
                    "비밀번호를 입력해주세요."
            );
        }


        if (!PASSWORD_PATTERN.matcher(
                password
        ).matches()) {

            throw new IllegalArgumentException(
                    "비밀번호는 8~20자이며 대문자, 소문자, 숫자, 특수문자를 각각 1개 이상 포함해야 하며 공백은 사용할 수 없습니다."
            );
        }
    }


    // =========================================================
    // 닉네임 사용 가능 여부 검사
    // =========================================================

    private boolean isValidNickname(
            String nickname
    ) {

        if (nickname == null
                || nickname.isBlank()) {

            return false;
        }


        if (!NICKNAME_PATTERN.matcher(
                nickname
        ).matches()) {

            return false;
        }


        String normalizedNickname =
                normalizeNicknameForFilter(
                        nickname
                );


        for (String blockedWord : BLOCKED_NICKNAME_WORDS) {

            String normalizedBlockedWord =
                    normalizeNicknameForFilter(
                            blockedWord
                    );


            if (normalizedNickname.contains(
                    normalizedBlockedWord
            )) {

                return false;
            }
        }


        for (String reservedNickname : RESERVED_NICKNAMES) {

            String normalizedReservedNickname =
                    normalizeNicknameForFilter(
                            reservedNickname
                    );


            if (normalizedNickname.equals(
                    normalizedReservedNickname
            )) {

                return false;
            }
        }


        return true;
    }


    // =========================================================
    // 닉네임 정책 검사
    // =========================================================

    private void validateNickname(
            String nickname
    ) {

        if (nickname == null
                || nickname.isBlank()) {

            throw new IllegalArgumentException(
                    "닉네임을 입력해주세요."
            );
        }


        if (!NICKNAME_PATTERN.matcher(
                nickname
        ).matches()) {

            throw new IllegalArgumentException(
                    "닉네임은 2~12자의 한글, 영문, 숫자, 밑줄(_)만 사용할 수 있습니다."
            );
        }


        String normalizedNickname =
                normalizeNicknameForFilter(
                        nickname
                );


        for (String blockedWord : BLOCKED_NICKNAME_WORDS) {

            String normalizedBlockedWord =
                    normalizeNicknameForFilter(
                            blockedWord
                    );


            if (normalizedNickname.contains(
                    normalizedBlockedWord
            )) {

                throw new IllegalArgumentException(
                        "사용할 수 없는 닉네임입니다."
                );
            }
        }


        for (String reservedNickname : RESERVED_NICKNAMES) {

            String normalizedReservedNickname =
                    normalizeNicknameForFilter(
                            reservedNickname
                    );


            if (normalizedNickname.equals(
                    normalizedReservedNickname
            )) {

                throw new IllegalArgumentException(
                        "사용할 수 없는 닉네임입니다."
                );
            }
        }
    }


    // =========================================================
    // 닉네임 필터용 정규화
    // =========================================================

    private String normalizeNicknameForFilter(
            String nickname
    ) {

        return nickname
                .toLowerCase(
                        Locale.ROOT
                )
                .replace(
                        "_",
                        ""
                );
    }


    // =========================================================
    // 이메일 마스킹
    // =========================================================

    private String maskEmail(
            String email
    ) {

        if (email == null
                || !email.contains(
                "@"
        )) {

            throw new IllegalArgumentException(
                    "올바른 이메일 형식이 아닙니다."
            );
        }


        String[] parts =
                email.split(
                        "@",
                        2
                );


        String localPart =
                parts[0];


        String domain =
                parts[1];


        int visibleLength =
                Math.min(
                        3,
                        localPart.length()
                );


        String visible =
                localPart.substring(
                        0,
                        visibleLength
                );


        String masked =
                "*".repeat(
                        Math.max(
                                1,
                                localPart.length()
                                        - visibleLength
                        )
                );


        return visible
                + masked
                + "@"
                + domain;
    }


    // =========================================================
    // Access / Refresh Token 발급
    // =========================================================

    private LoginToken issueLoginToken(
            User user
    ) {

        validateTokenUser(
                user
        );


        // 새로운 로그인 시 tokenVersion 증가
        // 기존 Access Token 무효화
        int updatedRows =
                userMapper.increaseTokenVersion(
                        user.getUserId()
                );


        if (updatedRows == 0) {

            throw new IllegalArgumentException(
                    "사용자를 찾을 수 없습니다."
            );
        }


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


        validateActiveStatus(
                user
        );


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
    // 계정 이용 상태 확인
    // =========================================================

    private void validateActiveStatus(
            User user
    ) {

        if (user == null) {

            throw new IllegalArgumentException(
                    "사용자 정보를 확인할 수 없습니다."
            );
        }


        if (user.getStatus() != UserStatus.ACTIVE) {

            throw new AccountSuspendedException();
        }
    }


    private boolean isAdminRole(String role) {
        return role != null
                && ("ADMIN".equalsIgnoreCase(role)
                || "ROLE_ADMIN".equalsIgnoreCase(role));
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

            // 사용자별 Refresh Token 1개 유지
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
