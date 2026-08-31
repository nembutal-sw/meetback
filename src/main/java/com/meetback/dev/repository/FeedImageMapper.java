package com.meetback.dev.repository;

import com.meetback.dev.domain.FeedImage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface FeedImageMapper {


    // ============================================================
    // 이미지 등록
    // ============================================================

    int insert(
            FeedImage feedImage
    );


    // ============================================================
    // 피드 이미지 전체 조회
    // ============================================================

    List<FeedImage> findByFeedId(
            @Param("feedId")
            Long feedId
    );


    // ============================================================
    // 특정 이미지 삭제
    //
    // feedId까지 같이 확인해서
    // 다른 피드 이미지를 잘못 삭제하지 못하게 처리
    // ============================================================

    int deleteByIdAndFeedId(
            @Param("feedImageId")
            Long feedImageId,

            @Param("feedId")
            Long feedId
    );


    // ============================================================
    // 특정 피드 이미지 전체 삭제
    //
    // 기존 기능 유지
    // ============================================================

    int deleteByFeedId(
            @Param("feedId")
            Long feedId
    );
}