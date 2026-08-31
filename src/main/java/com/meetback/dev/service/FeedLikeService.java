package com.meetback.dev.service;

import com.meetback.dev.domain.Feed;
import com.meetback.dev.domain.FeedLike;
import com.meetback.dev.dto.feed.FeedLikeResponse;
import com.meetback.dev.repository.FeedLikeMapper;
import com.meetback.dev.repository.FeedMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FeedLikeService {

    private final FeedLikeMapper feedLikeMapper;

    private final FeedMapper feedMapper;


    // ============================================================
    // 좋아요 등록
    // ============================================================

    @Transactional
    public FeedLikeResponse addLike(
            Long userId,
            Long feedId
    ) {

        validateUserId(
                userId
        );

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
                    "존재하지 않는 피드입니다."
            );
        }


        int exists =
                feedLikeMapper.existsByFeedIdAndUserId(
                        feedId,
                        userId
                );


        // 이미 좋아요를 누른 경우
        // 중복 INSERT 하지 않고 현재 상태 반환
        if (
                exists > 0
        ) {

            return getLikeStatus(
                    userId,
                    feedId
            );
        }


        FeedLike feedLike =
                new FeedLike();


        feedLike.setFeedId(
                feedId
        );


        feedLike.setUserId(
                userId
        );


        int insertedCount =
                feedLikeMapper.insert(
                        feedLike
                );


        if (
                insertedCount != 1
        ) {

            throw new IllegalStateException(
                    "좋아요 등록에 실패했습니다."
            );
        }


        return getLikeStatus(
                userId,
                feedId
        );
    }


    // ============================================================
    // 좋아요 취소
    // ============================================================

    @Transactional
    public FeedLikeResponse removeLike(
            Long userId,
            Long feedId
    ) {

        validateUserId(
                userId
        );

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
                    "존재하지 않는 피드입니다."
            );
        }


        feedLikeMapper.delete(
                feedId,
                userId
        );


        return getLikeStatus(
                userId,
                feedId
        );
    }


    // ============================================================
    // 좋아요 상태 조회
    // ============================================================

    @Transactional(readOnly = true)
    public FeedLikeResponse getLikeStatus(
            Long userId,
            Long feedId
    ) {

        validateUserId(
                userId
        );

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
                    "존재하지 않는 피드입니다."
            );
        }


        int likeCount =
                feedLikeMapper.countByFeedId(
                        feedId
                );


        boolean liked =
                feedLikeMapper.existsByFeedIdAndUserId(
                        feedId,
                        userId
                ) > 0;


        return new FeedLikeResponse(
                feedId,
                likeCount,
                liked
        );
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
                    "피드 ID가 필요합니다."
            );
        }
    }
}