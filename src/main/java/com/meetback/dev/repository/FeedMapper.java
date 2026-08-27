package com.meetback.dev.repository;

import com.meetback.dev.domain.Feed;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface FeedMapper {

    int insert(Feed feed);

    Feed findById(@Param("feedId") Long feedId);

    List<Feed> findAll();

    int update(Feed feed);

    int softDelete(
            @Param("feedId") Long feedId,
            @Param("userId") Long userId
    );
}
