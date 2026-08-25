package com.meetback.dev.controller;

import com.meetback.dev.dto.ChatMessageResponse;
import com.meetback.dev.security.AuthenticatedUser;
import com.meetback.dev.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;


    @GetMapping("/meetings/{meetingId}/messages")
    public List<ChatMessageResponse> getMessages(
            @PathVariable Long meetingId,

            // =====================================================
            // [TEMP-BKW-AUTH]
            //
            // 범석 Security 병합 시
            // Principal 타입만 변경하면 됨.
            //
            // ChatService에는 Long userId만 전달
            // =====================================================
            @AuthenticationPrincipal
            AuthenticatedUser user
    ) {

        return chatService.getMessages(
                meetingId,
                user.userId()
        );
    }
}