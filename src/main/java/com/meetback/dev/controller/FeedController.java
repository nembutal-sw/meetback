package com.meetback.dev.controller;

import com.meetback.dev.dto.feed.FeedCreateRequest;
import com.meetback.dev.dto.feed.FeedPageResponse;
import com.meetback.dev.dto.feed.FeedResponse;
import com.meetback.dev.dto.feed.FeedUpdateRequest;
import com.meetback.dev.security.AuthenticatedUser;
import com.meetback.dev.service.FeedService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/feeds")
@RequiredArgsConstructor
public class FeedController {

    private final FeedService feedService;


    // ============================================================
    // 후기 작성
    // ============================================================

    @PostMapping(
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<FeedResponse> createFeed(
            @AuthenticationPrincipal
            AuthenticatedUser authenticatedUser,

            @ModelAttribute
            FeedCreateRequest request,

            @RequestParam(
                    value = "images",
                    required = false
            )
            List<MultipartFile> images
    ) {

        FeedResponse response =
                feedService.createFeed(
                        authenticatedUser.userId(),
                        request,
                        images
                );


        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }


    // ============================================================
    // 후기 페이징 조회
    // ============================================================

    @GetMapping
    public ResponseEntity<FeedPageResponse> getFeeds(
            @AuthenticationPrincipal
            AuthenticatedUser authenticatedUser,

            @RequestParam(
                    value = "page",
                    defaultValue = "0"
            )
            int page,

            @RequestParam(
                    value = "size",
                    defaultValue = "5"
            )
            int size
    ) {

        FeedPageResponse response =
                feedService.getFeeds(
                        authenticatedUser.userId(),
                        page,
                        size
                );


        return ResponseEntity.ok(
                response
        );
    }


    // ============================================================
    // 후기 단건 조회
    // ============================================================

    @GetMapping("/{feedId}")
    public ResponseEntity<FeedResponse> getFeed(
            @PathVariable
            Long feedId
    ) {

        FeedResponse response =
                feedService.getFeed(
                        feedId
                );


        return ResponseEntity.ok(
                response
        );
    }


    // ============================================================
    // 후기 수정
    //
    // 기존 이미지:
    // 삭제할 이미지 ID만 deleteImageIds로 전달
    //
    // 새 이미지:
    // images로 추가
    // ============================================================

    @PutMapping(
            value = "/{feedId}",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<FeedResponse> updateFeed(
            @AuthenticationPrincipal
            AuthenticatedUser authenticatedUser,

            @PathVariable
            Long feedId,

            @ModelAttribute
            FeedUpdateRequest request,

            @RequestParam(
                    value = "images",
                    required = false
            )
            List<MultipartFile> images,

            @RequestParam(
                    value = "deleteImageIds",
                    required = false
            )
            List<Long> deleteImageIds
    ) {

        FeedResponse response =
                feedService.updateFeed(
                        authenticatedUser.userId(),
                        feedId,
                        request,
                        images,
                        deleteImageIds
                );


        return ResponseEntity.ok(
                response
        );
    }


    // ============================================================
    // 후기 삭제
    // ============================================================

    @DeleteMapping("/{feedId}")
    public ResponseEntity<Void> deleteFeed(
            @AuthenticationPrincipal
            AuthenticatedUser authenticatedUser,

            @PathVariable
            Long feedId
    ) {

        feedService.deleteFeed(
                authenticatedUser.userId(),
                feedId
        );


        return ResponseEntity
                .noContent()
                .build();
    }
}