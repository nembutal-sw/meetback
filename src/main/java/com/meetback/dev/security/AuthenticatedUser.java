package com.meetback.dev.security;

public record AuthenticatedUser(
        Long userId,
        String role,
        Integer tokenVersion
) {
}
