package com.meetback.dev.realtime.event;

public enum AuthInvalidationReason {
    LOGIN_REPLACED,
    LOGOUT,
    PASSWORD_RESET,
    ROLE_CHANGED,
    ADMIN_REVOKE
}
