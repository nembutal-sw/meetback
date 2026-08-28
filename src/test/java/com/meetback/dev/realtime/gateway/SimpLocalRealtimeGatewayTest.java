package com.meetback.dev.realtime.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class SimpLocalRealtimeGatewayTest {

    @Test
    void buildsServerOwnedMeetingDestination() {
        SimpMessagingTemplate messagingTemplate =
                mock(SimpMessagingTemplate.class);

        SimpLocalRealtimeGateway gateway =
                new SimpLocalRealtimeGateway(
                        messagingTemplate
                );

        Map<String, Object> payload =
                Map.of(
                        "messageType", "EVENT",
                        "eventType", "PARTICIPANT_KICKED"
                );

        gateway.broadcastToMeeting(
                7L,
                payload
        );

        verify(messagingTemplate).convertAndSend(
                "/topic/meetings/7/chat",
                (Object) payload
        );
    }
}
