package com.meetback.dev.controller;

import com.meetback.dev.dto.feed.FeedCreateRequest;
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


    // 후기작성
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
    ){
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

    //후기 전체 조회
    @GetMapping
    public ResponseEntity<List<FeedResponse>> getFeeds(){

        List<FeedResponse> response =
                feedService.getFeeds();

        return ResponseEntity.ok(
                response
        );
    }

    // 후기 상세 조회
    @GetMapping("/{feedId}")
    public ResponseEntity<FeedResponse> getFeed(
            @PathVariable
            Long feedId
    ){

        FeedResponse response =
                feedService.getFeed(
                        feedId
                );

        return ResponseEntity.ok(
                response
        );
    }

    // 후기 수정
    @PutMapping("/{feedId}")
    public ResponseEntity<FeedResponse> updateFeed(
            @AuthenticationPrincipal
            AuthenticatedUser authenticatedUser,

            @PathVariable
            Long feedId,

            @RequestBody
            FeedUpdateRequest request

    ) {

        FeedResponse response =
                feedService.updateFeed(
                        authenticatedUser.userId(),
                        feedId,
                        request
                );

        return ResponseEntity.ok(response);
    }

    // 후기 삭제
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
