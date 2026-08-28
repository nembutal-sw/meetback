package com.meetback.dev.realtime.gateway;

import com.meetback.dev.realtime.event.RealtimeEvent;
import com.meetback.dev.realtime.event.RealtimeEventEnvelope;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.beans.factory.ObjectProvider;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LocalRealtimeDispatcherTest {

    @Test
    @SuppressWarnings("unchecked")
    void broadcastsBeforeRequestingTargetDisconnect() {
        LocalRealtimeGateway gateway =
                mock(LocalRealtimeGateway.class);

        LocalSessionControl sessionControl =
                mock(LocalSessionControl.class);

        ObjectProvider<LocalSessionControl> provider =
                mock(ObjectProvider.class);

        when(provider.getIfAvailable())
                .thenReturn(sessionControl);

        RealtimeEventEnvelope event = kickEvent();

        new LocalRealtimeDispatcher(gateway, provider)
                .dispatch(event);

        InOrder order = inOrder(gateway, sessionControl);

        order.verify(gateway).broadcastToMeeting(
                event.meetingId(),
                event.clientPayload()
        );

        order.verify(sessionControl).disconnectMeetingUser(
                event.meetingId(),
                event.targetUserId(),
                event.eventType()
        );
    }

    @Test
    @SuppressWarnings("unchecked")
    void doesNotFailCompletedBroadcastWhenSessionControlFails() {
        LocalRealtimeGateway gateway =
                mock(LocalRealtimeGateway.class);

        LocalSessionControl sessionControl =
                mock(LocalSessionControl.class);

        ObjectProvider<LocalSessionControl> provider =
                mock(ObjectProvider.class);

        when(provider.getIfAvailable())
                .thenReturn(sessionControl);

        RealtimeEventEnvelope event = kickEvent();

        doThrow(new IllegalStateException("session close failed"))
                .when(sessionControl)
                .disconnectMeetingUser(
                        event.meetingId(),
                        event.targetUserId(),
                        event.eventType()
                );

        LocalRealtimeDispatcher dispatcher =
                new LocalRealtimeDispatcher(
                        gateway,
                        provider
                );

        assertThatCode(() -> dispatcher.dispatch(event))
                .doesNotThrowAnyException();
    }

    private RealtimeEventEnvelope kickEvent() {
        RealtimeEvent event =
                RealtimeEvent
                        .meetingBroadcastAndDisconnectTarget(
                                "PARTICIPANT_KICKED",
                                7L,
                                10L,
                                30L,
                                22L,
                                Map.of(
                                        "messageType", "EVENT",
                                        "eventType", "PARTICIPANT_KICKED"
                                )
                        );

        return RealtimeEventEnvelope.create(
                event,
                "instance-a"
        );
    }
}
