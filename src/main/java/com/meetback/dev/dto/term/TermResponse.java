package com.meetback.dev.dto.term;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class TermResponse {
    private Long termId;
    private String termCode;
    private String termName;
    private String content;
    private String version;
    private Boolean required;
    private LocalDateTime effectiveAt;
}
