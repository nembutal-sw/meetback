package com.meetback.dev.repository;

import com.meetback.dev.domain.ChatMessage;
import com.meetback.dev.dto.ChatMessageResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ChatMapper {

    /*
     * 채팅 저장
     *
     * INSERT할 때는 DB Entity인
     * ChatMessage를 사용
     */
    int insertMessage(
            ChatMessage message
    );


    /*
     * 방금 저장한 메시지 1건 조회
     *
     * participant + users JOIN을 통해
     * nickname까지 포함한 DTO 반환
     */
    ChatMessageResponse selectMessageById(
            @Param("messageId") Long messageId
    );


    /*
     * 특정 모임의 채팅 내역 조회
     *
     * 기존:
     * List<ChatMessage>
     *
     * 변경:
     * List<ChatMessageResponse>
     */
    List<ChatMessageResponse> selectMessages(
            @Param("meetingId") Long meetingId
    );
}