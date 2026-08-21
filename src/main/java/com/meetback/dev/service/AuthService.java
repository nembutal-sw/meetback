package com.meetback.dev.service;

import com.meetback.dev.domain.RefreshToken;
import com.meetback.dev.domain.Social;
import com.meetback.dev.domain.User;
import com.meetback.dev.dto.auth.*;
import com.meetback.dev.repository.RefreshTokenMapper;
import com.meetback.dev.repository.SocialMapper;
import com.meetback.dev.repository.UserMapper;
import com.meetback.dev.oauth.KakaoOAuthProvider;
import com.meetback.dev.oauth.KakaoUserInfo;
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


    // 회원가입

    public void signup(SignupRequest request) {

        // 이메일 중복 검사
        if (userMapper.existByEmail(request.getEmail()) > 0) {

            throw new IllegalArgumentException(
                    "이미 사용 중인 이메일입니다."
            );
        }


        // 닉네임 중복 검사
        if (userMapper.existByNickname(request.getNickname()) > 0) {

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


        // 비밀번호 BCrypt 해시
        user.setPasswordHash(
                passwordEncoder.encode(
                        request.getPassword()
                )
        );


        userMapper.insertUser(
                user
        );
    }

    // 일반 로그인

    public LoginResponse login(LoginRequest request) {

        User user =
                userMapper.selectByEmail(
                        request.getEmail()
                );


        if (user == null) {

            throw new IllegalArgumentException(
                    "이메일 또는 비밀번호가 일치하지 않습니다."
            );
        }


        // 탈퇴 요청 후 7일이 지난 회원만 로그인 차단
        if (user.getDeletedAt() != null) {

            LocalDateTime withdrawalDeadline =
                    user.getDeletedAt()
                            .plusDays(7);


            if (LocalDateTime.now()
                    .isAfter(withdrawalDeadline)) {

                throw new IllegalArgumentException(
                        "탈퇴 처리된 계정입니다."
                );
            }
        }


        // 비밀번호 확인

        if (user.getPasswordHash() == null
                || !passwordEncoder.matches(
                request.getPassword(),
                user.getPasswordHash()
        )) {

            throw new IllegalArgumentException(
                    "이메일 또는 비밀번호가 일치하지 않습니다."
            );
        }


        // Access Token 발급
        String accessToken =
                jwtProvider.createAccessToken(
                        user.getUserId(),
                        user.getRole()
                );


        // Refresh Token 발급
        String refreshToken =
                jwtProvider.createRefreshToken(
                        user.getUserId(),
                        user.getRole()
                );


        // Refresh Token DB 저장
        saveRefreshToken(
                user,
                refreshToken
        );


        LoginResponse response =
                new LoginResponse();


        response.setAccessToken(
                accessToken
        );

        response.setRefreshToken(
                refreshToken
        );

        response.setUserId(
                user.getUserId()
        );

        response.setRole(
                user.getRole()
        );


        return response;
    }


    // 카카오 로그인

    public KakaoLoginResponse kakaoLogin(
            KakaoLoginRequest request
    ) {

        // 카카오 인가 코드 → 카카오 Access Token
        String kakaoAccessToken =
                kakaoOAuthProvider.requestAccessToken(
                        request.getCode()
                );


        // 카카오 사용자 정보 조회
        KakaoUserInfo kakaoUserInfo =
                kakaoOAuthProvider.getUserInfo(
                        kakaoAccessToken
                );


        // 기존 카카오 계정 조회
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


            // users에도 없는 신규 회원
            if (user == null) {

                user = new User();


                user.setEmail(
                        kakaoUserInfo.getEmail()
                );


                user.setNickname(
                        kakaoUserInfo.getName()
                );


                userMapper.insertUser(
                        user
                );
            }


            // social 테이블 저장
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


        // 탈퇴 요청 후 7일 경과 확인
        if (user.getDeletedAt() != null) {

            LocalDateTime withdrawalDeadline =
                    user.getDeletedAt()
                            .plusDays(7);


            if (LocalDateTime.now()
                    .isAfter(withdrawalDeadline)) {

                throw new IllegalArgumentException(
                        "탈퇴 처리된 계정입니다."
                );
            }
        }


        // MeetBack 사설 Access Token 발급
        String accessToken =
                jwtProvider.createAccessToken(
                        user.getUserId(),
                        user.getRole()
                );


        // MeetBack 사설 Refresh Token 발급
        String refreshToken =
                jwtProvider.createRefreshToken(
                        user.getUserId(),
                        user.getRole()
                );


        // Refresh Token DB 저장
        saveRefreshToken(
                user,
                refreshToken
        );


        KakaoLoginResponse response =
                new KakaoLoginResponse();


        response.setAccessToken(
                accessToken
        );


        response.setRefreshToken(
                refreshToken
        );


        response.setUserId(
                user.getUserId()
        );


        response.setRole(
                user.getRole()
        );


        return response;
    }

    // Refresh Token 재발급

    public TokenRefreshResponse refresh(
            TokenRefreshRequest request
    ) {

        String requestRefreshToken =
                request.getRefreshToken();


        // JWT 자체 유효성 검사
        if (!jwtProvider.validateToken(
                requestRefreshToken
        )) {

            throw new IllegalArgumentException(
                    "유효하지 않은 Refresh Token입니다."
            );
        }


        // Refresh Token인지 확인
        if (!"REFRESH".equals(
                jwtProvider.getTokenType(
                        requestRefreshToken
                )
        )) {

            throw new IllegalArgumentException(
                    "Refresh Token이 아닙니다."
            );
        }


        // JWT에서 userId 추출
        Long userId =
                jwtProvider.getUserId(
                        requestRefreshToken
                );


        // 요청 Refresh Token 해시
        String tokenHash =
                hashToken(
                        requestRefreshToken
                );


        // DB Refresh Token 조회
        RefreshToken savedToken =
                refreshTokenMapper.selectByTokenHash(
                        tokenHash
                );


        if (savedToken == null) {

            throw new IllegalArgumentException(
                    "등록되지 않은 Refresh Token입니다."
            );
        }


        // Token 사용자 일치 확인
        if (!savedToken.getUserId()
                .equals(userId)) {

            throw new IllegalArgumentException(
                    "Refresh Token 사용자 정보가 일치하지 않습니다."
            );
        }


        // DB에 저장된 만료시간 확인
        if (savedToken.getExpiresAt() == null
                || savedToken.getExpiresAt()
                .isBefore(LocalDateTime.now())) {

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

            throw new IllegalArgumentException(
                    "사용자를 찾을 수 없습니다."
            );
        }


        // 탈퇴 요청 후 7일 경과 여부 확인
        if (user.getDeletedAt() != null) {

            LocalDateTime withdrawalDeadline =
                    user.getDeletedAt()
                            .plusDays(7);


            if (LocalDateTime.now()
                    .isAfter(withdrawalDeadline)) {

                refreshTokenMapper.deleteByUserId(
                        user.getUserId()
                );


                throw new IllegalArgumentException(
                        "회원탈퇴 유예기간이 만료되었습니다."
                );
            }
        }


        // 새로운 Access Token 발급
        String newAccessToken =
                jwtProvider.createAccessToken(
                        user.getUserId(),
                        user.getRole()
                );


        // 새로운 Refresh Token 발급
        String newRefreshToken =
                jwtProvider.createRefreshToken(
                        user.getUserId(),
                        user.getRole()
                );


        // DB Refresh Token 갱신
        saveRefreshToken(
                user,
                newRefreshToken
        );


        TokenRefreshResponse response =
                new TokenRefreshResponse();


        response.setAccessToken(
                newAccessToken
        );


        response.setRefreshToken(
                newRefreshToken
        );


        return response;
    }

    // 현재 로그인 사용자 조회

    public AuthCheckResponse getCurrentUser(
            Long userId
    ) {

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

    // 로그아웃

    public void logout(Long userId) {

        refreshTokenMapper.deleteByUserId(
                userId
        );
    }


    // 회원탈퇴 요청

    public void withdraw(Long userId) {

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


        /*
         * 회원탈퇴를 요청해도
         * Refresh Token은 즉시 삭제하지 않는다.
         *
         * deleted_at을 기준으로
         * 7일 동안 탈퇴 취소 가능.
         */
    }


    // 회원탈퇴 취소

    public void cancelWithdrawal(Long userId) {

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
                        .plusDays(7);


        // 7일이 지난 경우 탈퇴취소 불가능
        if (LocalDateTime.now()
                .isAfter(withdrawalDeadline)) {

            throw new IllegalArgumentException(
                    "회원탈퇴 취소 기간이 만료되었습니다."
            );
        }


        userMapper.cancelWithdrawal(
                userId
        );
    }


    // Refresh Token 저장 / 갱신

    private void saveRefreshToken(
            User user,
            String refreshToken
    ) {

        // DB에는 Refresh Token 원본이 아닌 SHA-256 Hash 저장
        String tokenHash =
                hashToken(
                        refreshToken
                );


        // 기존 Refresh Token 조회
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


        // 기본 Refresh Token 만료시간
        LocalDateTime expiresAt =
                LocalDateTime.now()
                        .plusSeconds(
                                jwtProvider
                                        .getRefreshTokenExpirationSeconds()
                        );


        /*
         * 탈퇴 요청 상태일 경우
         *
         * Refresh Token의 만료시간이
         * deletedAt + 7일보다 길어지지 않도록 제한
         */

        if (user.getDeletedAt() != null) {

            LocalDateTime withdrawalDeadline =
                    user.getDeletedAt()
                            .plusDays(7);


            if (expiresAt.isAfter(
                    withdrawalDeadline
            )) {

                expiresAt =
                        withdrawalDeadline;
            }
        }


        token.setExpiresAt(
                expiresAt
        );


        // 기존 Refresh Token이 없는 경우
        if (existingToken == null) {

            refreshTokenMapper.insertRefreshToken(
                    token
            );

        } else {

            // 기존 Refresh Token 갱신
            refreshTokenMapper.updateRefreshToken(
                    token
            );
        }
    }

    // Refresh Token SHA-256 Hash

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
}