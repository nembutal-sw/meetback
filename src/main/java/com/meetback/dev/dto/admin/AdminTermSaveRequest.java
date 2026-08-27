package com.meetback.dev.dto.admin;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdminTermSaveRequest {
    private String termCode;
    private String termName;
    private String content;
    private String version;
    private Boolean required;
    private LocalDateTime effectiveAt;
}
