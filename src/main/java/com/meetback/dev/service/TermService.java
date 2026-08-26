package com.meetback.dev.service;

import com.meetback.dev.domain.Term;
import com.meetback.dev.domain.UserTermAgreement;
import com.meetback.dev.dto.term.TermResponse;
import com.meetback.dev.repository.TermMapper;
import com.meetback.dev.repository.UserTermAgreementMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TermService {

    private final TermMapper termMapper;
    private final UserTermAgreementMapper userTermAgreementMapper;

    @Transactional(readOnly = true)
    public List<TermResponse> getActiveTerms() {

        return termMapper.findAllActiveTerms()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // 회원가입 전 필수 약관 동의 검증
    @Transactional(readOnly = true)
    public void validateRequiredTerms(
            List<Long> agreedTermIds
    ) {

        if (agreedTermIds == null
                || agreedTermIds.isEmpty()) {

            throw new IllegalArgumentException(
                    "필수 약관에 동의해주세요."
            );
        }

        List<Term> requiredTerms =
                termMapper.findAllRequiredActiveTerms();

        if (requiredTerms.isEmpty()) {

            throw new IllegalStateException(
                    "현재 적용 중인 필수 약관이 없습니다."
            );
        }

        for (Term requiredTerm : requiredTerms) {

            if (!agreedTermIds.contains(
                    requiredTerm.getTermId()
            )) {

                throw new IllegalArgumentException(
                        "필수 약관에 모두 동의해주세요."
                );
            }
        }

        for (Long termId : agreedTermIds) {

            if (termId == null) {

                throw new IllegalArgumentException(
                        "잘못된 약관 정보입니다."
                );
            }

            Term term =
                    termMapper.findByTermId(
                            termId
                    );

            if (term == null) {

                throw new IllegalArgumentException(
                        "존재하지 않는 약관입니다."
                );
            }

            if (!Boolean.TRUE.equals(
                    term.getActive()
            )) {

                throw new IllegalArgumentException(
                        "현재 사용할 수 없는 약관입니다."
                );
            }

            if (term.getEffectiveAt() != null
                    && term.getEffectiveAt()
                    .isAfter(LocalDateTime.now())) {

                throw new IllegalArgumentException(
                        "아직 적용되지 않은 약관입니다."
                );
            }
        }
    }

    // 회원의 약관 동의 내역 저장
    @Transactional
    public void saveAgreements(
            Long userId,
            List<Long> agreedTermIds
    ) {

        if (userId == null) {

            throw new IllegalArgumentException(
                    "회원 정보가 없습니다."
            );
        }

        validateRequiredTerms(
                agreedTermIds
        );

        LocalDateTime now =
                LocalDateTime.now();

        List<Long> uniqueTermIds =
                agreedTermIds
                        .stream()
                        .distinct()
                        .toList();

        for (Long termId : uniqueTermIds) {

            UserTermAgreement existingAgreement =
                    userTermAgreementMapper
                            .findByUserIdAndTermId(
                                    userId,
                                    termId
                            );

            if (existingAgreement != null) {
                continue;
            }

            UserTermAgreement agreement =
                    new UserTermAgreement();

            agreement.setUserId(
                    userId
            );

            agreement.setTermId(
                    termId
            );

            agreement.setAgreed(
                    true
            );

            agreement.setAgreedAt(
                    now
            );

            agreement.setRevokedAt(
                    null
            );

            agreement.setCreatedAt(
                    now
            );

            int inserted =
                    userTermAgreementMapper.insert(
                            agreement
                    );

            if (inserted != 1) {

                throw new IllegalStateException(
                        "약관 동의 저장에 실패했습니다."
                );
            }
        }
    }

    private TermResponse toResponse(
            Term term
    ) {

        return new TermResponse(
                term.getTermId(),
                term.getTermCode(),
                term.getTermName(),
                term.getContent(),
                term.getVersion(),
                term.getRequired(),
                term.getEffectiveAt()
        );
    }
}