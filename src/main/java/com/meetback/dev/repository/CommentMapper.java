package com.meetback.dev.repository;

import com.meetback.dev.domain.Comment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CommentMapper {

    // 댓글 등록
    int insert(Comment comment);

    // 댓글 단건 조회
    Comment findById(
            @Param("commentId") Long commentId
    );

    // 특정 피드 댓글 전체 조회
    List<Comment> findByFeedId(
            @Param("feedId") Long feedId
    );

    // 댓글 수정
    int update(
            @Param("commentId") Long commentId,
            @Param("userId") Long userId,
            @Param("content") String content
    );

    // 댓글 삭제
    int softDelete(
            @Param("commentId") Long commentId,
            @Param("userId") Long userId
    );

    // 특정 피드 댓글 개수
    int countByFeedId(
            @Param("feedId") Long feedId
    );
}