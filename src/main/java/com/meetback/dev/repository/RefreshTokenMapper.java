package com.meetback.dev.repository;

import com.meetback.dev.domain.RefreshToken;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface RefreshTokenMapper {

    // 해당 사용자의 Refresh Token 존재 여부 / 정보 조회
    RefreshToken selectByUserId(
            @Param("userId") Long userId
    );

    // 전달받는 Refresh Token 검증시 사용
    RefreshToken selectByTokenHash(
            @Param("tokenHash") String tokenHash
    );

    // 최초 로그인 등 리프레쉬 토큰이 없는 경우 저장
    int insertRefreshToken(
            RefreshToken refreshToken
    );

    // 기존 리프래쉬토큰 갱신
    int updateRefreshToken(
            RefreshToken refreshToken
    );

    // 로그아웃 / 사용자 Refresh Token 삭제
    int deleteByUserId(
            @Param("userId") Long userId
    );

    // 유효기간이 지난 Refresh Token 전체 삭제
    int deleteExpiredTokens();

}
