package com.meetback.dev.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class AdminTermResponse {
    private Long termId;
    private String termCode;
    private String termName;
    private String content;
    private String version;
    private Boolean required;
    private Boolean active;
    private LocalDateTime effectiveAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
