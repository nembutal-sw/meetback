package com.meetback.dev.controller;

import com.meetback.dev.dto.auth.LoginRequest;
import com.meetback.dev.dto.auth.LoginResponse;
import com.meetback.dev.dto.auth.SignupRequest;
import com.meetback.dev.security.JwtProvider;
import com.meetback.dev.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtProvider jwtProvider;

    //회원가입

    @PostMapping("/signup")
    public ResponseEntity<Void> signup(
            @RequestBody SignupRequest request
    ) {

        authService.signup(request);

        return  ResponseEntity
                .status(HttpStatus.CREATED)
                .build();
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @RequestBody LoginRequest request
            ) {
                LoginResponse response =
                        authService.login(request);

            return ResponseEntity.ok(
                    response
            );
    }
}
