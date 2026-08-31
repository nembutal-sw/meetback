package com.meetback.dev.controller;

import com.meetback.dev.dto.feed.FeedLikeResponse;
import com.meetback.dev.security.AuthenticatedUser;
import com.meetback.dev.service.FeedLikeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/feeds/{feedId}/likes")
@RequiredArgsConstructor
public class FeedLikeController {

    private final FeedLikeService feedLikeService;


    // ============================================================
    // 좋아요 등록
    // ============================================================

    @PostMapping
    public ResponseEntity<FeedLikeResponse> addLike(
            @AuthenticationPrincipal
            AuthenticatedUser authenticatedUser,

            @PathVariable
            Long feedId
    ) {

        FeedLikeResponse response =
                feedLikeService.addLike(
                        authenticatedUser.userId(),
                        feedId
                );


        return ResponseEntity.ok(
                response
        );
    }


    // ============================================================
    // 좋아요 취소
    // ============================================================

    @DeleteMapping
    public ResponseEntity<FeedLikeResponse> removeLike(
            @AuthenticationPrincipal
            AuthenticatedUser authenticatedUser,

            @PathVariable
            Long feedId
    ) {

        FeedLikeResponse response =
                feedLikeService.removeLike(
                        authenticatedUser.userId(),
                        feedId
                );


        return ResponseEntity.ok(
                response
        );
    }


    // ============================================================
    // 좋아요 상태 조회
    // ============================================================

    @GetMapping
    public ResponseEntity<FeedLikeResponse> getLikeStatus(
            @AuthenticationPrincipal
            AuthenticatedUser authenticatedUser,

            @PathVariable
            Long feedId
    ) {

        FeedLikeResponse response =
                feedLikeService.getLikeStatus(
                        authenticatedUser.userId(),
                        feedId
                );


        return ResponseEntity.ok(
                response
        );
    }
}