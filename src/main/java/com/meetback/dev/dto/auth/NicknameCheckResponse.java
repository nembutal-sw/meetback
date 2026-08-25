package com.meetback.dev.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class NicknameCheckResponse {
    private boolean available;
}
