package com.meetback.dev.scheduler;

import com.meetback.dev.service.MeetingService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MeetingCleanupScheduler {

    private final MeetingService meetingService;

    // 1시간마다 체크
    @Scheduled(fixedDelay = 60 * 60 * 1000)
    public void cleanupExpiredMeetings() {

        int deletedCount =
                meetingService.deleteExpiredMeetings();

        System.out.println(
                "[MEETING CLEANUP] 삭제된 모임 수: "
                        + deletedCount
        );
    }
}