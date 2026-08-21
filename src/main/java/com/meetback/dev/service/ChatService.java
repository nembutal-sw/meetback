package com.meetback.dev.service;

import com.meetback.dev.domain.ChatMessage;
import com.meetback.dev.repository.ChatMapper;
import com.meetback.dev.repository.ParticipantMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatMapper chatMapper;
    private final ParticipantMapper participantMapper;


    @Transactional
    public ChatMessage saveMessage(
            ChatMessage message
    ) {

        int count =
                participantMapper
                        .countParticipantByIdAndMeeting(
                                message.getParticipantId(),
                                message.getMeetingId()
                        );

        if (count == 0) {
            throw new IllegalArgumentException(
                    "해당 모임의 참가자가 아닙니다."
            );
        }


        if (message.getContent() == null
                || message.getContent().isBlank()) {

            throw new IllegalArgumentException(
                    "메시지 내용을 입력해주세요."
            );
        }


        chatMapper.insertMessage(message);

        return message;
    }


    public List<ChatMessage> getMessages(
            Long meetingId
    ) {

        return chatMapper.selectMessages(
                meetingId
        );
    }

}
