package com.meetback.dev.domain;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PlaceVote {

    private Long voteId;

    private Long meetingId;
    private Long participantId;
    private Long candidateId;

    private Integer voteChangeCount;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}
