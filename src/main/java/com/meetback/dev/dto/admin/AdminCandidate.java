package com.meetback.dev.dto.admin;

import lombok.Data;

/** 관리자용 장소 후보 조회 항목. */
@Data
public class AdminCandidate {
    private Long candidateId;
    private String placeName;
    private String address;
    private boolean active;
    private Long proposerUserId;
    private String proposerNickname;
    private long voteCount;
}
