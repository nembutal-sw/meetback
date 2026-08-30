package com.meetback.dev.service;

import com.meetback.dev.domain.Comment;
import com.meetback.dev.domain.Feed;
import com.meetback.dev.dto.comment.CommentCreateRequest;
import com.meetback.dev.dto.comment.CommentResponse;
import com.meetback.dev.dto.comment.CommentUpdateRequest;
import com.meetback.dev.repository.CommentMapper;
import com.meetback.dev.repository.FeedMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentMapper commentMapper;

    private final FeedMapper feedMapper;


    // ============================================================
    // 댓글 등록
    // ============================================================

    @Transactional
    public CommentResponse createComment(
            Long userId,
            Long feedId,
            CommentCreateRequest request
    ) {

        validateUserId(
                userId
        );

        validateFeedId(
                feedId
        );

        validateCreateRequest(
                request
        );


        // --------------------------------------------------------
        // 피드 존재 여부 확인
        // --------------------------------------------------------

        Feed feed =
                feedMapper.findById(
                        feedId
                );


        if (
                feed == null
        ) {

            throw new IllegalArgumentException(
                    "존재하지 않는 후기입니다."
            );
        }


        // --------------------------------------------------------
        // 댓글 생성
        // --------------------------------------------------------

        Comment comment =
                new Comment();


        comment.setFeedId(
                feedId
        );


        comment.setUserId(
                userId
        );


        comment.setContent(
                request
                        .getContent()
                        .trim()
        );


        int insertedCount =
                commentMapper.insert(
                        comment
                );


        if (
                insertedCount != 1
        ) {

            throw new IllegalStateException(
                    "댓글 등록에 실패했습니다."
            );
        }


        Long commentId =
                comment.getCommentId();


        if (
                commentId == null
        ) {

            throw new IllegalStateException(
                    "생성된 댓글 ID를 확인할 수 없습니다."
            );
        }


        // --------------------------------------------------------
        // JOIN된 닉네임까지 다시 조회
        // --------------------------------------------------------

        Comment savedComment =
                commentMapper.findById(
                        commentId
                );


        if (
                savedComment == null
        ) {

            throw new IllegalStateException(
                    "등록된 댓글을 확인할 수 없습니다."
            );
        }


        return toCommentResponse(
                savedComment,
                userId
        );
    }


    // ============================================================
    // 특정 피드 댓글 전체 조회
    // ============================================================

    @Transactional(readOnly = true)
    public List<CommentResponse> getComments(
            Long feedId,
            Long loginUserId
    ) {

        validateFeedId(
                feedId
        );


        Feed feed =
                feedMapper.findById(
                        feedId
                );


        if (
                feed == null
        ) {

            throw new IllegalArgumentException(
                    "존재하지 않는 후기입니다."
            );
        }


        List<Comment> comments =
                commentMapper.findByFeedId(
                        feedId
                );


        List<CommentResponse> responses =
                new ArrayList<>();


        for (
                Comment comment
                : comments
        ) {

            responses.add(
                    toCommentResponse(
                            comment,
                            loginUserId
                    )
            );
        }


        return responses;
    }


    // ============================================================
    // 댓글 수정
    // ============================================================

    @Transactional
    public CommentResponse updateComment(
            Long userId,
            Long commentId,
            CommentUpdateRequest request
    ) {

        validateUserId(
                userId
        );

        validateCommentId(
                commentId
        );

        validateUpdateRequest(
                request
        );


        Comment existingComment =
                commentMapper.findById(
                        commentId
                );


        if (
                existingComment == null
        ) {

            throw new IllegalArgumentException(
                    "존재하지 않는 댓글입니다."
            );
        }


        // --------------------------------------------------------
        // 본인 댓글인지 확인
        // --------------------------------------------------------

        if (
                existingComment.getUserId() == null
                        ||
                        !userId.equals(
                                existingComment.getUserId()
                        )
        ) {

            throw new IllegalArgumentException(
                    "본인이 작성한 댓글만 수정할 수 있습니다."
            );
        }


        int updatedCount =
                commentMapper.update(
                        commentId,
                        userId,
                        request
                                .getContent()
                                .trim()
                );


        if (
                updatedCount != 1
        ) {

            throw new IllegalStateException(
                    "댓글 수정에 실패했습니다."
            );
        }


        Comment updatedComment =
                commentMapper.findById(
                        commentId
                );


        if (
                updatedComment == null
        ) {

            throw new IllegalStateException(
                    "수정된 댓글을 확인할 수 없습니다."
            );
        }


        return toCommentResponse(
                updatedComment,
                userId
        );
    }


    // ============================================================
    // 댓글 삭제
    // ============================================================

    @Transactional
    public void deleteComment(
            Long userId,
            Long commentId
    ) {

        validateUserId(
                userId
        );

        validateCommentId(
                commentId
        );


        Comment comment =
                commentMapper.findById(
                        commentId
                );


        if (
                comment == null
        ) {

            throw new IllegalArgumentException(
                    "존재하지 않는 댓글입니다."
            );
        }


        // --------------------------------------------------------
        // 본인 댓글인지 확인
        // --------------------------------------------------------

        if (
                comment.getUserId() == null
                        ||
                        !userId.equals(
                                comment.getUserId()
                        )
        ) {

            throw new IllegalArgumentException(
                    "본인이 작성한 댓글만 삭제할 수 있습니다."
            );
        }


        int deletedCount =
                commentMapper.softDelete(
                        commentId,
                        userId
                );


        if (
                deletedCount != 1
        ) {

            throw new IllegalStateException(
                    "댓글 삭제에 실패했습니다."
            );
        }
    }


    // ============================================================
    // Comment -> CommentResponse
    // ============================================================

    private CommentResponse toCommentResponse(
            Comment comment,
            Long loginUserId
    ) {

        String nickname =
                comment.getNickname();


        if (
                nickname == null
                        ||
                        nickname.isBlank()
        ) {

            nickname =
                    "알 수 없음";

        }
        else {

            nickname =
                    nickname.trim();
        }


        boolean mine =
                loginUserId != null
                        &&
                        comment.getUserId() != null
                        &&
                        loginUserId.equals(
                                comment.getUserId()
                        );


        return new CommentResponse(
                comment.getCommentId(),
                comment.getFeedId(),
                comment.getUserId(),
                nickname,
                mine,
                comment.getContent(),
                comment.getCreatedAt(),
                comment.getUpdatedAt()
        );
    }


    // ============================================================
    // 댓글 등록 요청 검증
    // ============================================================

    private void validateCreateRequest(
            CommentCreateRequest request
    ) {

        if (
                request == null
        ) {

            throw new IllegalArgumentException(
                    "댓글 작성 정보가 필요합니다."
            );
        }


        validateContent(
                request.getContent()
        );
    }


    // ============================================================
    // 댓글 수정 요청 검증
    // ============================================================

    private void validateUpdateRequest(
            CommentUpdateRequest request
    ) {

        if (
                request == null
        ) {

            throw new IllegalArgumentException(
                    "댓글 수정 정보가 필요합니다."
            );
        }


        validateContent(
                request.getContent()
        );
    }


    // ============================================================
    // 댓글 내용 검증
    // ============================================================

    private void validateContent(
            String content
    ) {

        if (
                content == null
                        ||
                        content.isBlank()
        ) {

            throw new IllegalArgumentException(
                    "댓글 내용을 입력해주세요."
            );
        }
    }


    // ============================================================
    // 로그인 사용자 검증
    // ============================================================

    private void validateUserId(
            Long userId
    ) {

        if (
                userId == null
        ) {

            throw new IllegalArgumentException(
                    "로그인 정보가 필요합니다."
            );
        }
    }


    // ============================================================
    // 피드 ID 검증
    // ============================================================

    private void validateFeedId(
            Long feedId
    ) {

        if (
                feedId == null
        ) {

            throw new IllegalArgumentException(
                    "후기 ID가 필요합니다."
            );
        }
    }


    // ============================================================
    // 댓글 ID 검증
    // ============================================================

    private void validateCommentId(
            Long commentId
    ) {

        if (
                commentId == null
        ) {

            throw new IllegalArgumentException(
                    "댓글 ID가 필요합니다."
            );
        }
    }
}