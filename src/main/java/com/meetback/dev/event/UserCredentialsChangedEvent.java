package com.meetback.dev.event;

/** 로그인 자격 정보가 변경된 사용자 세션 종료 이벤트. */
public record UserCredentialsChangedEvent(Long userId) {
}
