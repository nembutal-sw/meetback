package com.meetback.dev.realtime.gateway;

public interface LocalRealtimeGateway {

    void broadcastToMeeting(
            Long meetingId,
            Object exactClientPayload
    );

    void broadcastToQuickLobby(
            Object exactClientPayload
    );
}
