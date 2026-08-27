package com.meetback.dev.service;

import com.meetback.dev.domain.Term;
import com.meetback.dev.dto.admin.AdminTermCreateRequest;
import com.meetback.dev.dto.admin.AdminTermResponse;
import com.meetback.dev.repository.TermMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminTermServiceTest {

    @Mock
    private TermMapper termMapper;

    @Test
    void returnsAllTermVersions() {
        Term term = term("SERVICE", "1.0");
        term.setTermId(3L);
        term.setTermName("서비스 이용약관");
        term.setContent("약관 내용");
        term.setRequired(true);
        term.setActive(true);
        term.setEffectiveAt(LocalDateTime.now());
        when(termMapper.findAllForAdmin()).thenReturn(List.of(term));

        AdminTermService service = new AdminTermService(termMapper);
        List<AdminTermResponse> result = service.findTerms();

        assertThat(result).singleElement().satisfies(response -> {
            assertThat(response.termId()).isEqualTo(3L);
            assertThat(response.termCode()).isEqualTo("SERVICE");
            assertThat(response.version()).isEqualTo("1.0");
            assertThat(response.active()).isTrue();
        });
    }

    @Test
    void deactivatesCurrentVersionBeforeInsertingNewVersion() {
        LocalDateTime databaseNow = LocalDateTime.of(
                2026,
                8,
                27,
                1,
                30
        );
        when(termMapper.findByTermCodeForUpdate("SERVICE"))
                .thenReturn(List.of(term("SERVICE", "1.0")));
        when(termMapper.findCurrentDateTime()).thenReturn(databaseNow);
        when(termMapper.deactivateByTermCode("SERVICE")).thenReturn(1);
        when(termMapper.insert(any(Term.class))).thenAnswer(invocation -> {
            Term inserted = invocation.getArgument(0);
            inserted.setTermId(7L);
            return 1;
        });

        AdminTermService service = new AdminTermService(termMapper);
        AdminTermResponse result = service.createTerm(new AdminTermCreateRequest(
                " service ",
                " 서비스 이용약관 ",
                "2.0",
                true,
                " 새 약관 내용 "
        ));

        InOrder order = inOrder(termMapper);
        order.verify(termMapper).findByTermCodeForUpdate("SERVICE");
        order.verify(termMapper).findCurrentDateTime();
        order.verify(termMapper).deactivateByTermCode("SERVICE");
        ArgumentCaptor<Term> captor = ArgumentCaptor.forClass(Term.class);
        order.verify(termMapper).insert(captor.capture());

        Term inserted = captor.getValue();
        assertThat(inserted.getTermCode()).isEqualTo("SERVICE");
        assertThat(inserted.getTermName()).isEqualTo("서비스 이용약관");
        assertThat(inserted.getContent()).isEqualTo("새 약관 내용");
        assertThat(inserted.getVersion()).isEqualTo("2.0");
        assertThat(inserted.getKakaoTag()).isNull();
        assertThat(inserted.getRequired()).isTrue();
        assertThat(inserted.getActive()).isTrue();
        assertThat(inserted.getEffectiveAt()).isEqualTo(databaseNow);
        assertThat(inserted.getCreatedAt()).isEqualTo(databaseNow);
        assertThat(inserted.getUpdatedAt()).isEqualTo(databaseNow);
        assertThat(result.termId()).isEqualTo(7L);
        assertThat(result.active()).isTrue();
    }

    @Test
    void rejectsDuplicateCodeAndVersionWithoutChangingRows() {
        when(termMapper.findByTermCodeForUpdate("PRIVACY"))
                .thenReturn(List.of(term("PRIVACY", "1.0")));

        AdminTermService service = new AdminTermService(termMapper);

        assertThatThrownBy(() -> service.createTerm(new AdminTermCreateRequest(
                "PRIVACY",
                "개인정보 처리방침",
                "1.0",
                true,
                "개인정보 약관 내용"
        )))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("이미 등록된");

        verify(termMapper, never()).deactivateByTermCode("PRIVACY");
        verify(termMapper, never()).insert(any(Term.class));
    }

    @Test
    void rejectsInvalidInputBeforeQueryingDatabase() {
        AdminTermService service = new AdminTermService(termMapper);

        assertThatThrownBy(() -> service.createTerm(new AdminTermCreateRequest(
                "bad code",
                "서비스 이용약관",
                "1.0",
                true,
                "약관 내용"
        )))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("약관 코드");

        assertThatThrownBy(() -> service.createTerm(new AdminTermCreateRequest(
                "SERVICE",
                "서비스 이용약관",
                "version 1",
                true,
                "약관 내용"
        )))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("약관 버전");

        assertThatThrownBy(() -> service.createTerm(new AdminTermCreateRequest(
                "SERVICE",
                "서비스 이용약관",
                "1.0",
                true,
                "   "
        )))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("약관 내용");

        assertThatThrownBy(() -> service.createTerm(new AdminTermCreateRequest(
                "SERVICE",
                "서비스 이용약관",
                "1.0",
                null,
                "약관 내용"
        )))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("필수 약관 여부");

        assertThatThrownBy(() -> service.createTerm(new AdminTermCreateRequest(
                "A".repeat(101),
                "서비스 이용약관",
                "1.0",
                true,
                "약관 내용"
        )))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("100자 이하");

        verifyNoInteractions(termMapper);
    }

    private Term term(String termCode, String version) {
        Term term = new Term();
        term.setTermCode(termCode);
        term.setVersion(version);
        return term;
    }
}
