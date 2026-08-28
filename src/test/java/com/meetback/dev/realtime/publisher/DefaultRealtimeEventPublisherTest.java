package com.meetback.dev.realtime.publisher;

import com.meetback.dev.realtime.config.RedisRealtimeProperties;
import com.meetback.dev.realtime.event.RealtimeEvent;
import com.meetback.dev.realtime.event.RealtimeEventEnvelope;
import com.meetback.dev.realtime.event.RealtimePublishResult;
import com.meetback.dev.realtime.gateway.LocalRealtimeDispatcher;
import com.meetback.dev.realtime.redis.LocalRealtimeEchoTracker;
import com.meetback.dev.realtime.redis.RealtimeEventCodec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DefaultRealtimeEventPublisherTest {

    private LocalRealtimeDispatcher localDispatcher;
    private StringRedisTemplate redisTemplate;
    private ObjectProvider<StringRedisTemplate>
            redisTemplateProvider;
    private RealtimeEventCodec eventCodec;
    private LocalRealtimeEchoTracker echoTracker;
    private RedisRealtimeProperties properties;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        localDispatcher = mock(LocalRealtimeDispatcher.class);
        redisTemplate = mock(StringRedisTemplate.class);
        redisTemplateProvider = mock(ObjectProvider.class);
        eventCodec = mock(RealtimeEventCodec.class);
        echoTracker = mock(LocalRealtimeEchoTracker.class);

        when(redisTemplateProvider.getIfAvailable())
                .thenReturn(redisTemplate);

        properties = new RedisRealtimeProperties();
        properties.setEnabled(true);
    }

    @Test
    void dispatchesLocallyAndPublishesToMeetingChannel() {
        when(eventCodec.encode(any()))
                .thenReturn("{\"schemaVersion\":1}");

        DefaultRealtimeEventPublisher publisher =
                publisher();

        RealtimePublishResult result =
                publisher.publish(meetingEvent());

        ArgumentCaptor<RealtimeEventEnvelope> eventCaptor =
                ArgumentCaptor.forClass(
                        RealtimeEventEnvelope.class
                );

        verify(localDispatcher).dispatch(
                eventCaptor.capture()
        );

        RealtimeEventEnvelope envelope =
                eventCaptor.getValue();

        assertThat(envelope.sourceInstanceId())
                .isEqualTo("instance-a");

        assertThat(envelope.eventType())
                .isEqualTo("VOTE_UPDATED");

        assertThat(result.eventId())
                .isEqualTo(envelope.eventId());

        assertThat(result.localDispatched()).isTrue();
        assertThat(result.redisCommandSucceeded()).isTrue();

        verify(redisTemplate).convertAndSend(
                properties.getMeetingChannel(),
                "{\"schemaVersion\":1}"
        );

        verify(echoTracker).mark(envelope.eventId());
    }

    @Test
    void keepsLocalDeliverySuccessfulWhenRedisPublishFails() {
        when(eventCodec.encode(any()))
                .thenReturn("{}");

        when(
                redisTemplate.convertAndSend(
                        properties.getMeetingChannel(),
                        "{}"
                )
        ).thenThrow(
                new IllegalStateException(
                        "redis unavailable"
                )
        );

        RealtimePublishResult result =
                publisher().publish(meetingEvent());

        assertThat(result.localDispatched()).isTrue();
        assertThat(result.redisCommandSucceeded()).isFalse();

        verify(localDispatcher).dispatch(any());
        verify(echoTracker).forget(result.eventId());
    }

    @Test
    void disabledModuleDoesNotTouchRedis() {
        properties.setEnabled(false);

        RealtimePublishResult result =
                publisher().publish(meetingEvent());

        assertThat(result.localDispatched()).isTrue();
        assertThat(result.redisCommandSucceeded()).isFalse();

        verify(localDispatcher).dispatch(any());
        verifyNoInteractions(
                redisTemplateProvider,
                eventCodec,
                redisTemplate,
                echoTracker
        );
    }

    private DefaultRealtimeEventPublisher publisher() {
        return new DefaultRealtimeEventPublisher(
                localDispatcher,
                redisTemplateProvider,
                eventCodec,
                echoTracker,
                properties,
                "instance-a"
        );
    }

    private RealtimeEvent meetingEvent() {
        return RealtimeEvent.meetingBroadcast(
                "VOTE_UPDATED",
                7L,
                10L,
                Map.of(
                        "messageType", "EVENT",
                        "eventType", "VOTE_UPDATED"
                )
        );
    }
}
