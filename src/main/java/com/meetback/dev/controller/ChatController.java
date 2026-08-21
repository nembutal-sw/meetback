package com.meetback.dev.controller;

import com.meetback.dev.domain.ChatMessage;
import com.meetback.dev.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/meetings")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @GetMapping("/{meetingId}/messages")
    public List<ChatMessage> getMessages(
            @PathVariable Long meetingId
    ) {

        return chatService.getMessages(meetingId);
    }

}
