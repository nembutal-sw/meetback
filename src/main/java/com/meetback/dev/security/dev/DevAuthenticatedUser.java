package com.meetback.dev.security.dev;

public record DevAuthenticatedUser(
        Long userId,
        String role
) {

}
