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

    public JwtProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-expiration}") long accessTokenExpiration,
            @Value("${jwt.refresh-token-expiration}") long refreshTokenExpiration
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


    // Role 조회
    public String getRole(
            String token
    ) {

        Claims claims =
                getClaims(
                        token
                );


        return claims.get(
                "role",
                String.class
        );
    }


    // Token Type 조회
    public String getTokenType(
            String token
    ) {

        Claims claims =
                getClaims(
                        token
                );


        return claims.get(
                "tokenType",
                String.class
        );
    }


    // Token Version 조회
    public Integer getTokenVersion(
            String token
    ) {

        Claims claims =
                getClaims(
                        token
                );


        Object tokenVersion =
                claims.get(
                        "tokenVersion"
                );


        if (!(tokenVersion instanceof Number number)) {

            return null;
        }


        return number.intValue();
    }


    public long getRefreshTokenExpirationSeconds() {

        return refreshTokenExpiration
                / 1000;
    }


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