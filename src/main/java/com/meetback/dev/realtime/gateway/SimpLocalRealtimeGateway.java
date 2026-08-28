package com.meetback.dev.realtime.gateway;

import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Objects;

public class SimpLocalRealtimeGateway
        implements LocalRealtimeGateway {

    private final SimpMessagingTemplate messagingTemplate;

    public SimpLocalRealtimeGateway(
            SimpMessagingTemplate messagingTemplate
    ) {
        this.messagingTemplate =
                Objects.requireNonNull(
                        messagingTemplate,
                        "messagingTemplate은 필수입니다."
                );
    }

    @Override
    public void broadcastToMeeting(
            Long meetingId,
            Object exactClientPayload
    ) {
        if (meetingId == null || meetingId <= 0) {
            throw new IllegalArgumentException(
                    "meetingId는 양수여야 합니다."
            );
        }

        Objects.requireNonNull(
                exactClientPayload,
                "exactClientPayload는 필수입니다."
        );

        messagingTemplate.convertAndSend(
                "/topic/meetings/"
                        + meetingId
                        + "/chat",
                exactClientPayload
        );
    }
}
