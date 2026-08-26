package com.meetback.dev.repository;

import com.meetback.dev.domain.UserTermAgreement;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserTermAgreementMapper {

    int insert(UserTermAgreement agreement);

    List<UserTermAgreement> findByUserId(Long userId);

    UserTermAgreement findByUserIdAndTermId(
            @Param("userId") Long userId,
            @Param("termId") Long termid
    );
}
