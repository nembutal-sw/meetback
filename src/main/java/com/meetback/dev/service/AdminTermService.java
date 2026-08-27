package com.meetback.dev.service;

import com.meetback.dev.domain.Term;
import com.meetback.dev.dto.admin.AdminTermCreateRequest;
import com.meetback.dev.dto.admin.AdminTermResponse;
import com.meetback.dev.repository.TermMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class AdminTermService {

    private static final int MAX_CODE_LENGTH = 100;
    private static final int MAX_NAME_LENGTH = 255;
    private static final int MAX_VERSION_LENGTH = 50;
    private static final int MAX_CONTENT_LENGTH = 1_000_000;
    private static final Pattern CODE_PATTERN = Pattern.compile(
            "^[A-Z][A-Z0-9_]*$"
    );
    private static final Pattern VERSION_PATTERN = Pattern.compile(
            "^[A-Za-z0-9][A-Za-z0-9._-]*$"
    );

    private final TermMapper termMapper;

    @Transactional(readOnly = true)
    public List<AdminTermResponse> findTerms() {
        return termMapper.findAllForAdmin()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public AdminTermResponse createTerm(
            AdminTermCreateRequest request
    ) {
        ValidatedTerm validated = validate(request);
        Term term;

        try {
            List<Term> versions = termMapper.findByTermCodeForUpdate(
                    validated.termCode()
            );
            boolean duplicated = versions.stream()
                    .anyMatch(existing -> validated.version()
                            .equalsIgnoreCase(existing.getVersion()));
            if (duplicated) {
                throw new IllegalArgumentException(
                        "이미 등록된 약관 코드와 버전입니다."
                );
            }

            LocalDateTime now = termMapper.findCurrentDateTime();
            if (now == null) {
                throw new IllegalStateException(
                        "데이터베이스 현재 시각을 확인하지 못했습니다."
                );
            }

            term = new Term();
            term.setTermCode(validated.termCode());
            term.setTermName(validated.termName());
            term.setContent(validated.content());
            term.setVersion(validated.version());
            term.setKakaoTag(null);
            term.setRequired(validated.required());
            term.setActive(true);
            term.setEffectiveAt(now);
            term.setCreatedAt(now);
            term.setUpdatedAt(now);

            termMapper.deactivateByTermCode(validated.termCode());
            int inserted = termMapper.insert(term);
            if (inserted != 1) {
                throw new IllegalStateException(
                        "약관 버전을 등록하지 못했습니다."
                );
            }
        }
        catch (DuplicateKeyException e) {
            throw new IllegalArgumentException(
                    "이미 등록된 약관 코드와 버전입니다."
            );
        }
        catch (ConcurrencyFailureException e) {
            throw new IllegalStateException(
                    "다른 약관 등록이 진행 중입니다. 다시 시도해주세요."
            );
        }

        return toResponse(term);
    }

    private ValidatedTerm validate(AdminTermCreateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("약관 정보를 입력해주세요.");
        }

        String termCode = normalizeCode(request.termCode());
        String termName = normalizeText(
                request.termName(),
                "약관 이름",
                MAX_NAME_LENGTH
        );
        String version = normalizeText(
                request.version(),
                "약관 버전",
                MAX_VERSION_LENGTH
        );
        String content = normalizeText(
                request.content(),
                "약관 내용",
                MAX_CONTENT_LENGTH
        );

        if (!VERSION_PATTERN.matcher(version).matches()) {
            throw new IllegalArgumentException(
                    "약관 버전은 영문 또는 숫자로 시작하고 영문, 숫자, 점, 밑줄, 하이픈만 사용할 수 있습니다."
            );
        }
        if (request.required() == null) {
            throw new IllegalArgumentException(
                    "필수 약관 여부를 선택해주세요."
            );
        }

        return new ValidatedTerm(
                termCode,
                termName,
                version,
                request.required(),
                content
        );
    }

    private String normalizeCode(String value) {
        String termCode = normalizeText(
                value,
                "약관 코드",
                MAX_CODE_LENGTH
        ).toUpperCase(Locale.ROOT);
        if (!CODE_PATTERN.matcher(termCode).matches()) {
            throw new IllegalArgumentException(
                    "약관 코드는 영문 대문자로 시작하고 영문 대문자, 숫자, 밑줄만 사용할 수 있습니다."
            );
        }
        return termCode;
    }

    private String normalizeText(
            String value,
            String field,
            int maxLength
    ) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    field + "을(를) 입력해주세요."
            );
        }

        String normalized = value.strip();
        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(
                    field + "은(는) " + maxLength + "자 이하여야 합니다."
            );
        }
        return normalized;
    }

    private AdminTermResponse toResponse(Term term) {
        return new AdminTermResponse(
                term.getTermId(),
                term.getTermCode(),
                term.getTermName(),
                term.getVersion(),
                term.getRequired(),
                term.getContent(),
                term.getActive(),
                term.getEffectiveAt(),
                term.getCreatedAt(),
                term.getUpdatedAt()
        );
    }

    private record ValidatedTerm(
            String termCode,
            String termName,
            String version,
            Boolean required,
            String content
    ) {
    }
}
