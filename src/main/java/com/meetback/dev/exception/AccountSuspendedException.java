package com.meetback.dev.exception;

public class AccountSuspendedException extends RuntimeException {

    public AccountSuspendedException() {
        super("이용이 정지된 계정입니다.");
    }
}
