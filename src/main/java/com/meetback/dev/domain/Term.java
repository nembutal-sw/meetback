package com.meetback.dev.domain;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Term {

    private Long termId;
    private String termCode;
    private String termName;
    private String kakaoTag;
    private Boolean required;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updateAt;

}
