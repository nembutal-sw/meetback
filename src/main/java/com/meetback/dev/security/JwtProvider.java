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
                Decoders.BASE64.decode(secret);

        this.secretKey =
                Keys.hmacShaKeyFor(keyBytes);

        this.accessTokenExpiration =
                accessTokenExpiration;

        this.refreshTokenExpiration =
                refreshTokenExpiration;
    }


    // Access Token 생성
    public String createAccessToken(
            Long userId,
            String role
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
                        String.valueOf(userId)
                )
                .claim(
                        "role",
                        role
                )
                .claim(
                        "tokenType",
                        "ACCESS"
                )
                .issuedAt(now)
                .expiration(expiration)
                .signWith(secretKey)
                .compact();
    }


    // Refresh Token 생성
    public String createRefreshToken(
            Long userId,
            String role
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
                        String.valueOf(userId)
                )
                .claim(
                        "role",
                        role
                )
                .claim(
                        "tokenType",
                        "REFRESH"
                )
                .issuedAt(now)
                .expiration(expiration)
                .signWith(secretKey)
                .compact();
    }


    // Token 유효성 검사
    public boolean validateToken(
            String token
    ) {

        try {

            Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);

            return true;

        } catch (JwtException
                 | IllegalArgumentException e) {

            return false;
        }
    }


    // userId 추출
    public Long getUserId(
            String token
    ) {

        Claims claims =
                getClaims(token);

        return Long.valueOf(
                claims.getSubject()
        );
    }


    // role 추출
    public String getRole(
            String token
    ) {

        Claims claims =
                getClaims(token);

        return claims.get(
                "role",
                String.class
        );
    }


    // Access / Refresh 구분
    public String getTokenType(
            String token
    ) {

        Claims claims =
                getClaims(token);

        return claims.get(
                "tokenType",
                String.class
        );
    }


    // Refresh Token 만료시간 초 단위 반환
    public long getRefreshTokenExpirationSeconds() {

        return refreshTokenExpiration / 1000;
    }


    private Claims getClaims(
            String token
    ) {

        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}