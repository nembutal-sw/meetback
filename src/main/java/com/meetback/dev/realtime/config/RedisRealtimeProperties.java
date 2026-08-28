package com.meetback.dev.realtime.config;

import com.meetback.dev.realtime.event.RealtimeChannel;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "meetback.realtime.redis")
public class RedisRealtimeProperties {

    private boolean enabled;

    private String meetingChannel =
            "meetback:realtime:meeting:v1";

    private String authChannel =
            "meetback:realtime:auth:v1";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getMeetingChannel() {
        return meetingChannel;
    }

    public void setMeetingChannel(String meetingChannel) {
        this.meetingChannel = requireText(
                meetingChannel,
                "meetingChannel"
        );
    }

    public String getAuthChannel() {
        return authChannel;
    }

    public void setAuthChannel(String authChannel) {
        this.authChannel = requireText(
                authChannel,
                "authChannel"
        );
    }

    public String channelName(RealtimeChannel channel) {
        return switch (channel) {
            case MEETING -> meetingChannel;
            case AUTH -> authChannel;
        };
    }

    private String requireText(
            String value,
            String fieldName
    ) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    fieldName + "은(는) 필수입니다."
            );
        }

        return value.trim();
    }
}
