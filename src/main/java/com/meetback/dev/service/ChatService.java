package com.meetback.dev.service;

import com.meetback.dev.domain.ChatMessage;
import com.meetback.dev.domain.MeetingParticipant;
import com.meetback.dev.dto.ChatMessageResponse;
import com.meetback.dev.dto.ChatSendRequest;
import com.meetback.dev.repository.ChatMapper;
import com.meetback.dev.repository.ParticipantMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatMapper chatMapper;
    private final ParticipantMapper participantMapper;


    @Transactional
    public ChatMessageResponse saveMessage(
            Long meetingId,
            Long userId,
            ChatSendRequest request
    ) {

        if (request.content() == null
                || request.content().isBlank()) {

            throw new IllegalArgumentException(
                    "메세지를 입력해주세요."
            );
        }


        MeetingParticipant participant =
                participantMapper.findByMeetingAndUser(
                        meetingId,
                        userId
                );


        if (participant == null) {

            throw new IllegalArgumentException(
                    "해당 모임의 참가자가 아닙니다."
            );
        }


        ChatMessage message =
                new ChatMessage();


        message.setMeetingId(
                meetingId
        );


        message.setParticipantId(
                participant.getParticipantId()
        );


        message.setMessageType(
                "CHAT"
        );


        message.setEventType(
                null
        );


        message.setContent(
                request.content().trim()
        );


        message.setCreatedAt(
                LocalDateTime.now()
        );


        chatMapper.insertMessage(
                message
        );


        // INSERT 후 generated key가
        // message.messageId에 들어와 있음
        return chatMapper.selectMessageById(
                message.getMessageId()
        );
    }


    public List<ChatMessageResponse> getMessages(
            Long meetingId,
            Long userId
    ) {

        MeetingParticipant participant =
                participantMapper.findByMeetingAndUser(
                        meetingId,
                        userId
                );


        if (participant == null) {

            throw new IllegalArgumentException(
                    "해당 모임의 참가자가 아닙니다."
            );
        }


        return chatMapper.selectMessages(
                meetingId
        );
    }
}