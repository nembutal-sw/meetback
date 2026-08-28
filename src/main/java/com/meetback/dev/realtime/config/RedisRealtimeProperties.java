package com.meetback.dev.realtime.config;

import com.meetback.dev.realtime.event.RealtimeChannel;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Redis 실시간 기능의 활성화 여부와 논리 채널별 실제 Redis 채널명을
 * 설정값에 바인딩한다.
 * 채널명은 setter에서 공백 값을 거부하고 앞뒤 공백을 제거한다.
 */
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

    /**
     * envelope의 논리 채널을 현재 환경에 설정된
     * 실제 Redis 채널명으로 변환한다.
     */
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
