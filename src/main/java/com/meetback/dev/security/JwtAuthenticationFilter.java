package com.meetback.dev.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String authorization = request.getHeader("Authorization");

        if (authorization == null
                || !authorization.startsWith("Bearer ")) {

            filterChain.doFilter(request, response);
            return;
        }

        String token = authorization.substring(7);

        if (!jwtProvider.validateToken(token)) {

            SecurityContextHolder.clearContext();

            filterChain.doFilter(request, response);
            return;
        }

        String tokenType = jwtProvider.getTokenType(token);

        if (!"ACCESS".equals(tokenType)) {

            SecurityContextHolder.clearContext();

            filterChain.doFilter(request, response);
            return;
        }

        Long userId = jwtProvider.getUserId(token);
        String role = jwtProvider.getRole(token);

        if (userId == null
                || role == null
                || role.isBlank()) {

            SecurityContextHolder.clearContext();

            filterChain.doFilter(request, response);
            return;
        }

        AuthenticatedUser authenticatedUser =
                new AuthenticatedUser(
                        userId,
                        role
                );

        String authority = role.startsWith("ROLE_")
                ? role
                : "ROLE_" + role;

        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(
                        authenticatedUser,
                        null,
                        List.of(
                                new SimpleGrantedAuthority(
                                        authority
                                )
                        )
                );

        SecurityContextHolder
                .getContext()
                .setAuthentication(authenticationToken);

        filterChain.doFilter(request, response);
    }
}