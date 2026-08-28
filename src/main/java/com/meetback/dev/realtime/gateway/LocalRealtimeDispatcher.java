package com.meetback.dev.realtime.gateway;

import com.meetback.dev.realtime.event.RealtimeDeliveryKind;
import com.meetback.dev.realtime.event.RealtimeEventEnvelope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;

import java.util.Objects;

public class LocalRealtimeDispatcher {

    private static final Logger log =
            LoggerFactory.getLogger(
                    LocalRealtimeDispatcher.class
            );

    private final LocalRealtimeGateway realtimeGateway;
    private final ObjectProvider<LocalSessionControl>
            sessionControlProvider;

    public LocalRealtimeDispatcher(
            LocalRealtimeGateway realtimeGateway,
            ObjectProvider<LocalSessionControl>
                    sessionControlProvider
    ) {
        this.realtimeGateway =
                Objects.requireNonNull(
                        realtimeGateway,
                        "realtimeGateway는 필수입니다."
                );

        this.sessionControlProvider =
                Objects.requireNonNull(
                        sessionControlProvider,
                        "sessionControlProvider는 필수입니다."
                );
    }

    public void dispatch(
            RealtimeEventEnvelope event
    ) {
        Objects.requireNonNull(event, "event는 필수입니다.");

        switch (event.deliveryKind()) {
            case ROOM_BROADCAST -> broadcast(event);
            case ROOM_BROADCAST_AND_DISCONNECT_TARGET ->
                    broadcastAndDisconnectTarget(event);
            case DISCONNECT_USER_BEFORE_TOKEN_VERSION ->
                    disconnectBeforeTokenVersion(event);
        }
    }

    private void broadcast(
            RealtimeEventEnvelope event
    ) {
        realtimeGateway.broadcastToMeeting(
                event.meetingId(),
                event.clientPayload()
        );
    }

    private void broadcastAndDisconnectTarget(
            RealtimeEventEnvelope event
    ) {
        broadcast(event);

        LocalSessionControl sessionControl =
                sessionControlProvider.getIfAvailable();

        if (sessionControl == null) {
            warnMissingSessionControl(event);
            return;
        }

        try {
            sessionControl.disconnectMeetingUser(
                    event.meetingId(),
                    event.targetUserId(),
                    event.eventType()
            );
        }
        catch (RuntimeException exception) {
            /*
             * The room payload has already been sent. Propagating this
             * exception would make the publisher treat the whole local
             * dispatch as failed and replay the same broadcast on its Redis
             * echo. Session control is an idempotent, best-effort integration
             * hook and is therefore reported separately.
             */
            log.error(
                    "모임 대상 세션 종료에 실패했습니다. "
                            + "eventId={}, meetingId={}, targetUserId={}",
                    event.eventId(),
                    event.meetingId(),
                    event.targetUserId(),
                    exception
            );
        }
    }

    private void disconnectBeforeTokenVersion(
            RealtimeEventEnvelope event
    ) {
        LocalSessionControl sessionControl =
                sessionControlProvider.getIfAvailable();

        if (sessionControl == null) {
            warnMissingSessionControl(event);
            return;
        }

        sessionControl.disconnectUserBeforeTokenVersion(
                event.targetUserId(),
                event.minimumValidTokenVersion(),
                event.reason()
        );
    }

    private void warnMissingSessionControl(
            RealtimeEventEnvelope event
    ) {
        log.warn(
                "LocalSessionControl 미구현으로 세션 종료를 생략합니다. "
                        + "eventId={}, eventType={}, targetUserId={}",
                event.eventId(),
                event.eventType(),
                event.targetUserId()
        );
    }
}
