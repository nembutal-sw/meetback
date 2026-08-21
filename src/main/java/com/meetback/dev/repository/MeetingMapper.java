package com.meetback.dev.repository;

import com.meetback.dev.domain.Meeting;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface MeetingMapper {

    Meeting findById(Long meetingId);

    int updateCalculationVersion(
            @Param("meetingId") Long meetingId,
            @Param("calculationVersion") Integer calculationVersion
    );
}