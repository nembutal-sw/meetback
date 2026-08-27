package com.meetback.dev.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AccountSuspendedException.class)
    public ResponseEntity<Map<String, String>> handleAccountSuspended(
            AccountSuspendedException e
    ) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(
                        Map.of(
                                "code", "ACCOUNT_SUSPENDED",
                                "message", e.getMessage()
                        )
                );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>>
    handleIllegalArgument(
            IllegalArgumentException e
    ) {
        return ResponseEntity
                .badRequest()
                .body(
                        Map.of(
                                "message",
                                e.getMessage()
                        )
                );
    }


    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>>
    handleIllegalState(
            IllegalStateException e
    ) {
        String message = e.getMessage();
        if (message != null
                && message.contains(
                "PROXY_DAILY_LIMIT_EXHAUSTED"
        )) {
            return ResponseEntity
                    .status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(
                            Map.of(
                                    "message",
                                    "오늘 사용할 수 있는 대중교통 조회 횟수를 모두 사용했습니다. "
                                            + "다음 날 다시 시도해주세요."
                            )
                    );
        }

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(
                        Map.of(
                                "message",
                                message
                        )
                );
    }
}
