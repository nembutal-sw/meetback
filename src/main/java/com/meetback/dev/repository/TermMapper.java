package com.meetback.dev.repository;

import com.meetback.dev.domain.Term;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TermMapper {

    // 현재 적용 중인 약관 전체 조회
    List<Term> findAllActiveTerms();

    // 현재 적용 중인 필수 약관 조회
    List<Term> findAllRequiredActiveTerms();

    // 약관 ID로 조회
    Term findByTermId(Long termId);

    // 현재 적용 중인 약관 코드로 조회
    Term findByTermCode(String termCode);

    // 약관 코드와 버전으로 조회
    Term findByTermCodeAndVersion(
            @Param("termCode") String termCode,
            @Param("version") String version
    );

    // 새 약관 또는 새 버전 등록
    int insert(Term term);

    // 기존 약관 비활성화
    int deactivateByTermCode(
            @Param("termCode") String termCode
    );
}