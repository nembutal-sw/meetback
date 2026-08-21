package com.meetback.dev.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgument(
            IllegalArgumentException e
    ) {
        return ResponseEntity
                .badRequest()
                .body(e.getMessage());
    }


    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<String> handleIllegalState(
            IllegalStateException e
    ) {
        String message =
                e.getMessage();
        if (message != null
                && message.contains(
                "PROXY_DAILY_LIMIT_EXHAUSTED"
        )) {
            return ResponseEntity
                    .status(
                            HttpStatus.TOO_MANY_REQUESTS
                    )
                    .body(
                            "오늘 사용할 수 있는 대중교통 조회 횟수를 모두 사용했습니다. "
                                    + "다음 날 다시 시도해주세요."
                    );
        }
        return ResponseEntity
                .status(
                        HttpStatus.CONFLICT
                )
                .body(
                        message
                );
    }
}