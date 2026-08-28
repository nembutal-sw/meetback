package com.meetback.dev.realtime.redis;

import com.meetback.dev.realtime.config.RedisRealtimeProperties;
import com.meetback.dev.realtime.event.RealtimeEvent;
import com.meetback.dev.realtime.event.RealtimeEventEnvelope;
import com.meetback.dev.realtime.gateway.LocalRealtimeDispatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class RedisRealtimeMessageListenerTest {

    private RealtimeEventCodec eventCodec;
    private LocalRealtimeDispatcher localDispatcher;
    private LocalRealtimeEchoTracker echoTracker;
    private RedisRealtimeProperties properties;

    @BeforeEach
    void setUp() {
        eventCodec = mock(RealtimeEventCodec.class);
        localDispatcher = mock(LocalRealtimeDispatcher.class);
        echoTracker = mock(LocalRealtimeEchoTracker.class);
        properties = new RedisRealtimeProperties();
    }

    @Test
    void dispatchesEventPublishedByAnotherInstance() {
        RealtimeEventEnvelope event =
                remoteEvent("instance-b");

        when(eventCodec.decode("{}"))
                .thenReturn(event);

        listener().receive(
                properties.getMeetingChannel(),
                "{}"
        );

        verify(localDispatcher).dispatch(event);
    }

    @Test
    void ignoresEchoFromSameInstance() {
        RealtimeEventEnvelope event =
                remoteEvent("instance-a");

        when(eventCodec.decode("{}"))
                .thenReturn(event);

        when(echoTracker.consumeIfMarked(event.eventId()))
                .thenReturn(true);

        listener().receive(
                properties.getMeetingChannel(),
                "{}"
        );

        verifyNoInteractions(localDispatcher);
    }

    @Test
    void retriesSameInstanceEchoWhenLocalDeliveryWasNotMarked() {
        RealtimeEventEnvelope event =
                remoteEvent("instance-a");

        when(eventCodec.decode("{}"))
                .thenReturn(event);

        listener().receive(
                properties.getMeetingChannel(),
                "{}"
        );

        verify(localDispatcher).dispatch(event);
    }

    @Test
    void rejectsEnvelopePublishedOnWrongChannel() {
        RealtimeEventEnvelope event =
                remoteEvent("instance-b");

        when(eventCodec.decode("{}"))
                .thenReturn(event);

        listener().receive(
                properties.getAuthChannel(),
                "{}"
        );

        verifyNoInteractions(localDispatcher);
    }

    private RedisRealtimeMessageListener listener() {
        return new RedisRealtimeMessageListener(
                eventCodec,
                localDispatcher,
                echoTracker,
                properties,
                "instance-a"
        );
    }

    private RealtimeEventEnvelope remoteEvent(
            String sourceInstanceId
    ) {
        RealtimeEvent event =
                RealtimeEvent.meetingBroadcast(
                        "VOTE_UPDATED",
                        7L,
                        10L,
                        Map.of(
                                "messageType", "EVENT",
                                "eventType", "VOTE_UPDATED"
                        )
                );

        return RealtimeEventEnvelope.create(
                event,
                sourceInstanceId
        );
    }
}
