package com.meetback.dev.repository;

import com.meetback.dev.domain.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserMapper {

    // 회원가입
    int insertUser(User user);

    // 이메일 중복 검사
    // User.email UNIQUE에 대응
    int existByEmail(
            @Param("email") String email
    );

    // 닉네임 중복검사
    int existByNickname(
            @Param("nickname") String nickname
    );

    // 이메일 회원 조회
    // 일반 로그인에 사용
    User selectByEmail(
            @Param("email") String email
    );

    // UserId로 회원 조회
    // JWT / RefreshToken 사용자 확인에 사용
    User selectById(
            @Param("userId") Long userId
    );

    // 회원탈퇴 요청
    int withdrawUser(
            @Param("userId") Long userId
    );

    // 회원탈퇴 취소
    int cancelWithdrawal(
            @Param("userId") Long userId
    );

    int increaseTokenVersion(
            @Param("userId") Long userId
    );

    User findById(Long userId);
}
