package com.meetback.dev.repository;

import com.meetback.dev.domain.Meeting;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MeetingMapper {
    int insertMeeting(Meeting meeting);

    Meeting selectByInviteCode(String inviteCode);

}
