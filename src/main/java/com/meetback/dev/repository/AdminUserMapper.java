package com.meetback.dev.repository;

import com.meetback.dev.domain.User;
import com.meetback.dev.domain.UserStatus;
import com.meetback.dev.dto.admin.AdminUserDetail;
import com.meetback.dev.dto.admin.AdminUserListItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 관리자 회원 조회와 상태 변경 Mapper. */
@Mapper
public interface AdminUserMapper {
    List<AdminUserListItem> findUsers(
            @Param("query") String query,
            @Param("status") UserStatus status,
            @Param("deleted") Boolean deleted,
            @Param("offset") int offset,
            @Param("size") int size
    );

    long countUsers(
            @Param("query") String query,
            @Param("status") UserStatus status,
            @Param("deleted") Boolean deleted
    );

    AdminUserDetail findUserDetail(@Param("userId") Long userId);

    User findUserForUpdate(@Param("userId") Long userId);

    int suspendUser(@Param("userId") Long userId);

    int activateUser(@Param("userId") Long userId);

    int updateAdminAccount(
            @Param("userId") Long userId,
            @Param("loginId") String loginId,
            @Param("nickname") String nickname,
            @Param("passwordHash") String passwordHash,
            @Param("increaseTokenVersion") boolean increaseTokenVersion
    );
}
