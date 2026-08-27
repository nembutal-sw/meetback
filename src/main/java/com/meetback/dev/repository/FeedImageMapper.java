package com.meetback.dev.repository;

import com.meetback.dev.domain.FeedImage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface FeedImageMapper {

    int insert(FeedImage feedImage);

    List<FeedImage> findByFeedId(
            @Param("feedId") Long feedId
    );

    int deleteByFeedId(
            @Param("feedId") Long feedId
    );
}
