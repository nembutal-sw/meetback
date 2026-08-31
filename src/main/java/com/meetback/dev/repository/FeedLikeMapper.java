package com.meetback.dev.repository;

import com.meetback.dev.domain.FeedLike;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface FeedLikeMapper {

    // 좋아요 등록
    int insert(
            FeedLike feedLike
    );

    // 좋아요 취소
    int delete(
            @Param("feedId") Long feedId,
            @Param("userId") Long userId
    );

    // 특정 피드 좋아요 개수
    int countByFeedId(
            @Param("feedId") Long feedId
    );

    // 특정 사용자가 해당 피드에 좋아요를 눌렀는지 확인
    int existsByFeedIdAndUserId(
            @Param("feedId") Long feedId,
            @Param("userId") Long userId
    );
}