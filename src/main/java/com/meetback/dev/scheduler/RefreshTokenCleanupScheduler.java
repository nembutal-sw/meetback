package com.meetback.dev.scheduler;

import com.meetback.dev.repository.RefreshTokenMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class RefreshTokenCleanupScheduler {

    private final RefreshTokenMapper refreshTokenMapper;

    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void deleteExpiredRefreshTokens()
    {
        refreshTokenMapper.deleteExpiredTokens();
    }
}
