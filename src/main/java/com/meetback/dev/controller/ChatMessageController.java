package com.meetback.dev.controller;

import com.meetback.dev.domain.MeetingEventType;
import com.meetback.dev.dto.ChatMessageResponse;
import com.meetback.dev.dto.ChatSendRequest;
import com.meetback.dev.realtime.event.RealtimeEvent;
import com.meetback.dev.realtime.publisher.RealtimeEventPublisher;
import com.meetback.dev.security.AuthenticatedUser;
import com.meetback.dev.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import com.meetback.dev.service.MeetingPresenceService;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.security.access.AccessDeniedException;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class ChatMessageController {

    private final ChatService chatService;

    private final RealtimeEventPublisher realtimeEventPublisher;
    private final MeetingPresenceService meetingPresenceService;

    @MessageMapping("/meetings/{meetingId}/chat")
    public void sendMessage(
            @DestinationVariable Long meetingId,
            ChatSendRequest request,
            Principal principal,
            @Header("simpSessionId")
            String sessionId
    )
    {

        // =====================================================
        // [TEMP-BKW-AUTH]
        // 범석 Security 코드 병합 시
        // DevAuthenticatedUser를 최종 Principal 타입으로 교체.
        // ChatService는 Long userId만 받으므로 수정 필요 없음.
        // =====================================================

        if(!(principal instanceof  Authentication authentication))
        {
            throw new IllegalStateException(
                    "인증된 사용자가 아닙니다."
            );
        }

        Object principalObject = authentication.getPrincipal();

        if(!(principalObject instanceof AuthenticatedUser user)){
            throw new IllegalStateException(
                    "인증 사용자 정보를 확인할 수 없습니다."
            );
        }

        if(
                !meetingPresenceService.isActiveSession(
                        sessionId,
                        meetingId,
                        user.userId()
                )
        )
        {
            throw new AccessDeniedException(
                    "다른 탭에서 이 모임을 열어 현재 탭의 채팅 연결이 종료되었습니다."
            );
        }

        ChatMessageResponse saved =
                chatService.saveMessage(
                        meetingId,
                        user.userId(),
                        request
                );

        realtimeEventPublisher.publish(
                RealtimeEvent.meetingBroadcast(
                        MeetingEventType.CHAT_MESSAGE.name(),
                        meetingId,
                        user.userId(),
                        saved
                )
        );
    }



}
