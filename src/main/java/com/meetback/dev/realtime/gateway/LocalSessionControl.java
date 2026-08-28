package com.meetback.dev.realtime.gateway;

/**
 * Optional SPI implemented by the WebSocket session owner.
 *
 * <p>The Redis module can request session termination, but it does not own the
 * physical WebSocketSession registry. Implementations must be idempotent
 * because a caller can retry publication after an uncertain failure.</p>
 */
public interface LocalSessionControl {

    void disconnectMeetingUser(
            Long meetingId,
            Long userId,
            String reason
    );

    void disconnectUserBeforeTokenVersion(
            Long userId,
            Integer minimumValidTokenVersion,
            String reason
    );
}
