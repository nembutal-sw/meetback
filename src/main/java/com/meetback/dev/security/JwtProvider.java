package com.meetback.dev.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtProvider {

    private final SecretKey secretKey;

    private final long accessTokenExpiration;
    private final long refreshTokenExpiration;

    private final long socialSignupTokenExpiration;
    private final long passwordResetTokenExpiration;


    public JwtProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-expiration}") long accessTokenExpiration,
            @Value("${jwt.refresh-token-expiration}") long refreshTokenExpiration,
            @Value("${jwt.social-signup-token-expiration}") long socialSignupTokenExpiration,
            @Value("${jwt.password-reset-token-expiration}") long passwordResetTokenExpiration
    ) {

        byte[] keyBytes =
                Decoders.BASE64.decode(
                        secret
                );

        this.secretKey =
                Keys.hmacShaKeyFor(
                        keyBytes
                );

        this.accessTokenExpiration =
                accessTokenExpiration;

        this.refreshTokenExpiration =
                refreshTokenExpiration;

        this.socialSignupTokenExpiration =
                socialSignupTokenExpiration;

        this.passwordResetTokenExpiration =
                passwordResetTokenExpiration;
    }


    // Access Token 생성
    public String createAccessToken(
            Long userId,
            String role,
            Integer tokenVersion
    ) {

        Date now =
                new Date();

        Date expiration =
                new Date(
                        now.getTime()
                                + accessTokenExpiration
                );

        return Jwts.builder()
                .subject(
                        String.valueOf(
                                userId
                        )
                )
                .claim(
                        "role",
                        role
                )
                .claim(
                        "tokenType",
                        "ACCESS"
                )
                .claim(
                        "tokenVersion",
                        tokenVersion
                )
                .issuedAt(
                        now
                )
                .expiration(
                        expiration
                )
                .signWith(
                        secretKey
                )
                .compact();
    }


    // Refresh Token 생성
    public String createRefreshToken(
            Long userId,
            String role,
            Integer tokenVersion
    ) {

        Date now =
                new Date();

        Date expiration =
                new Date(
                        now.getTime()
                                + refreshTokenExpiration
                );

        return Jwts.builder()
                .subject(
                        String.valueOf(
                                userId
                        )
                )
                .claim(
                        "role",
                        role
                )
                .claim(
                        "tokenType",
                        "REFRESH"
                )
                .claim(
                        "tokenVersion",
                        tokenVersion
                )
                .issuedAt(
                        now
                )
                .expiration(
                        expiration
                )
                .signWith(
                        secretKey
                )
                .compact();
    }


    // 소셜 회원가입 임시 Token 생성
    public String createSocialSignupToken(
            String provider,
            String providerId,
            String email,
            Boolean emailVerified,
            String profileName
    ) {

        Date now =
                new Date();

        Date expiration =
                new Date(
                        now.getTime()
                                + socialSignupTokenExpiration
                );

        return Jwts.builder()
                .subject(
                        providerId
                )
                .claim(
                        "provider",
                        provider
                )
                .claim(
                        "email",
                        email
                )
                .claim(
                        "emailVerified",
                        Boolean.TRUE.equals(
                                emailVerified
                        )
                )
                .claim(
                        "name",
                        profileName
                )
                .claim(
                        "tokenType",
                        "SOCIAL_SIGNUP"
                )
                .issuedAt(
                        now
                )
                .expiration(
                        expiration
                )
                .signWith(
                        secretKey
                )
                .compact();
    }


    // 비밀번호 재설정 Token 생성
    public String createPasswordResetToken(
            Long userId,
            Integer tokenVersion
    ) {

        Date now =
                new Date();

        Date expiration =
                new Date(
                        now.getTime()
                                + passwordResetTokenExpiration
                );

        return Jwts.builder()
                .subject(
                        String.valueOf(
                                userId
                        )
                )
                .claim(
                        "tokenType",
                        "PASSWORD_RESET"
                )
                .claim(
                        "tokenVersion",
                        tokenVersion
                )
                .issuedAt(
                        now
                )
                .expiration(
                        expiration
                )
                .signWith(
                        secretKey
                )
                .compact();
    }


    // Token 유효성 검사
    public boolean validateToken(
            String token
    ) {

        try {

            Jwts.parser()
                    .verifyWith(
                            secretKey
                    )
                    .build()
                    .parseSignedClaims(
                            token
                    );

            return true;

        } catch (JwtException | IllegalArgumentException e) {

            return false;
        }
    }


    // UserId 조회
    public Long getUserId(
            String token
    ) {

        Claims claims =
                getClaims(
                        token
                );

        String subject =
                claims.getSubject();

        if (subject == null
                || subject.isBlank()) {

            return null;
        }

        try {

            return Long.valueOf(
                    subject
            );

        } catch (NumberFormatException e) {

            return null;
        }
    }


    // Subject 조회
    public String getSubject(
            String token
    ) {

        return getClaims(
                token
        ).getSubject();
    }


    // Role 조회
    public String getRole(
            String token
    ) {

        return getClaims(
                token
        ).get(
                "role",
                String.class
        );
    }


    // Token Type 조회
    public String getTokenType(
            String token
    ) {

        return getClaims(
                token
        ).get(
                "tokenType",
                String.class
        );
    }


    // Token Version 조회
    public Integer getTokenVersion(
            String token
    ) {

        Object tokenVersion =
                getClaims(
                        token
                ).get(
                        "tokenVersion"
                );

        if (!(tokenVersion instanceof Number number)) {

            return null;
        }

        return number.intValue();
    }


    // 소셜 Provider 조회
    public String getProvider(
            String token
    ) {

        return getClaims(
                token
        ).get(
                "provider",
                String.class
        );
    }


    // 소셜 ProviderId 조회
    public String getProviderId(
            String token
    ) {

        return getSubject(
                token
        );
    }


    // 소셜 이메일 조회
    public String getEmail(
            String token
    ) {

        return getClaims(
                token
        ).get(
                "email",
                String.class
        );
    }


    // 소셜 이메일 인증 여부 조회
    public Boolean getEmailVerified(
            String token
    ) {

        return getClaims(
                token
        ).get(
                "emailVerified",
                Boolean.class
        );
    }


    // 소셜 프로필 이름 조회
    public String getName(
            String token
    ) {

        return getClaims(
                token
        ).get(
                "name",
                String.class
        );
    }


    // 기존 프로필 이름 조회 호환
    public String getProfileName(
            String token
    ) {

        return getName(
                token
        );
    }


    // Refresh Token 만료시간 조회
    public long getRefreshTokenExpirationSeconds() {

        return refreshTokenExpiration
                / 1000;
    }


    // Token Claims 조회
    private Claims getClaims(
            String token
    ) {

        return Jwts.parser()
                .verifyWith(
                        secretKey
                )
                .build()
                .parseSignedClaims(
                        token
                )
                .getPayload();
    }
}