package com.meetback.dev.repository;

import com.meetback.dev.domain.ChatMessage;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ChatMapper {

    int insertMessage(ChatMessage message);

    List<ChatMessage> selectMessages(
            Long meetingId
    );

}
