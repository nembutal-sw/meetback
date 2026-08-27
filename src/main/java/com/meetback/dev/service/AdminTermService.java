package com.meetback.dev.service;

import com.meetback.dev.domain.Term;
import com.meetback.dev.dto.admin.AdminTermResponse;
import com.meetback.dev.dto.admin.AdminTermSaveRequest;
import com.meetback.dev.dto.term.TermResponse;
import com.meetback.dev.repository.TermMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminTermService {

    private final TermMapper termMapper;


    // 약관 코드별 현재 적용 약관 조회
    @Transactional(readOnly = true)
    public AdminTermResponse getCurrentTerm(
            String termCode
    ) {

        validateTermCode(
                termCode
        );


        Term term =
                termMapper.findByTermCode(
                        termCode
                );


        if (term == null)
        {
            throw new IllegalArgumentException(
                    "현재 적용 중인 약관이 없습니다."
            );
        }


        return toResponse(
                term
        );
    }


    // 약관 코드별 전체 버전 이력 조회
    @Transactional(readOnly = true)
    public List<AdminTermResponse> getTermHistory(
            String termCode
    ) {

        validateTermCode(
                termCode
        );


        return termMapper
                .findAllByTermCode(
                        termCode
                )
                .stream()
                .map(
                        this::toResponse
                )
                .toList();
    }


    // 새 약관 버전 등록
    @Transactional
    public AdminTermResponse createNewVersion(
            AdminTermSaveRequest request
    ) {

        validateRequest(
                request
        );


        String termCode =
                request
                        .getTermCode()
                        .trim()
                        .toUpperCase();


        Term duplicatedTerm =
                termMapper.findByTermCodeAndVersion(
                        termCode,
                        request.getVersion().trim()
                );


        if (duplicatedTerm != null)
        {
            throw new IllegalArgumentException(
                    "이미 등록된 약관 버전입니다."
            );
        }


        // 기존 활성 버전 비활성화
        termMapper.deactivateByTermCode(
                termCode
        );


        Term term =
                new Term();


        term.setTermCode(
                termCode
        );


        term.setTermName(
                request
                        .getTermName()
                        .trim()
        );


        term.setContent(
                request.getContent()
        );


        term.setVersion(
                request
                        .getVersion()
                        .trim()
        );


        term.setKakaoTag(
                null
        );


        term.setRequired(
                request.getRequired()
        );


        term.setActive(
                true
        );


        term.setEffectiveAt(
                request.getEffectiveAt()
        );


        LocalDateTime now =
                LocalDateTime.now();


        term.setCreatedAt(
                now
        );


        term.setUpdatedAt(
                now
        );


        int inserted =
                termMapper.insert(
                        term
                );


        if (inserted != 1)
        {
            throw new IllegalStateException(
                    "약관 등록에 실패했습니다."
            );
        }


        return toResponse(
                term
        );
    }


    private void validateRequest(
            AdminTermSaveRequest request
    ) {

        if (request == null)
        {
            throw new IllegalArgumentException(
                    "약관 정보가 필요합니다."
            );
        }


        validateTermCode(
                request.getTermCode()
        );


        if (
                request.getTermName() == null
                        ||
                        request.getTermName().isBlank()
        )
        {
            throw new IllegalArgumentException(
                    "약관명을 입력해주세요."
            );
        }


        if (
                request.getVersion() == null
                        ||
                        request.getVersion().isBlank()
        )
        {
            throw new IllegalArgumentException(
                    "약관 버전을 입력해주세요."
            );
        }


        if (
                request.getContent() == null
                        ||
                        request.getContent().isBlank()
        )
        {
            throw new IllegalArgumentException(
                    "약관 내용을 입력해주세요."
            );
        }


        if (request.getRequired() == null)
        {
            throw new IllegalArgumentException(
                    "약관 동의 구분을 선택해주세요."
            );
        }


        if (request.getEffectiveAt() == null)
        {
            throw new IllegalArgumentException(
                    "약관 시행일을 입력해주세요."
            );
        }
    }


    private void validateTermCode(
            String termCode
    ) {

        if (
                termCode == null
                        ||
                        termCode.isBlank()
        )
        {
            throw new IllegalArgumentException(
                    "약관 코드가 필요합니다."
            );
        }


        String normalizedTermCode =
                termCode
                        .trim()
                        .toUpperCase();


        if (
                !normalizedTermCode.equals(
                        "SERVICE"
                )
                        &&
                        !normalizedTermCode.equals(
                                "PRIVACY"
                        )
                        &&
                        !normalizedTermCode.equals(
                                "LOCATION"
                        )
        )
        {
            throw new IllegalArgumentException(
                    "지원하지 않는 약관 코드입니다."
            );
        }
    }


    private AdminTermResponse toResponse(
            Term term
    )
    {

        return new AdminTermResponse(
                term.getTermId(),
                term.getTermCode(),
                term.getTermName(),
                term.getContent(),
                term.getVersion(),
                term.getRequired(),
                term.getActive(),
                term.getEffectiveAt(),
                term.getCreatedAt(),
                term.getUpdatedAt()
        );
    }
}