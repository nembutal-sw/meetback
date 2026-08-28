package com.meetback.dev.repository;

import com.meetback.dev.domain.ParticipantKickHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ParticipantKickHistoryMapper {

    int insertKickHistory(
            ParticipantKickHistory history
    );

    int cancelLatestKickHistory(
            @Param("participantId") Long participantId,
            @Param("canceledByUserId") Long canceledByUserId
    );

}
