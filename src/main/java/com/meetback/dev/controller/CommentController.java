package com.meetback.dev.controller;

import com.meetback.dev.dto.comment.CommentCreateRequest;
import com.meetback.dev.dto.comment.CommentResponse;
import com.meetback.dev.dto.comment.CommentUpdateRequest;
import com.meetback.dev.security.AuthenticatedUser;
import com.meetback.dev.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;


    // ============================================================
    // 댓글 등록
    // ============================================================

    @PostMapping("/feeds/{feedId}/comments")
    public ResponseEntity<CommentResponse> createComment(
            @AuthenticationPrincipal
            AuthenticatedUser authenticatedUser,

            @PathVariable
            Long feedId,

            @RequestBody
            CommentCreateRequest request
    ) {

        CommentResponse response =
                commentService.createComment(
                        authenticatedUser.userId(),
                        feedId,
                        request
                );


        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }


    // ============================================================
    // 특정 피드 댓글 전체 조회
    // ============================================================

    @GetMapping("/feeds/{feedId}/comments")
    public ResponseEntity<List<CommentResponse>> getComments(
            @AuthenticationPrincipal
            AuthenticatedUser authenticatedUser,

            @PathVariable
            Long feedId
    ) {

        List<CommentResponse> response =
                commentService.getComments(
                        feedId,
                        authenticatedUser.userId()
                );


        return ResponseEntity.ok(
                response
        );
    }


    // ============================================================
    // 댓글 수정
    // ============================================================

    @PutMapping("/comments/{commentId}")
    public ResponseEntity<CommentResponse> updateComment(
            @AuthenticationPrincipal
            AuthenticatedUser authenticatedUser,

            @PathVariable
            Long commentId,

            @RequestBody
            CommentUpdateRequest request
    ) {

        CommentResponse response =
                commentService.updateComment(
                        authenticatedUser.userId(),
                        commentId,
                        request
                );


        return ResponseEntity.ok(
                response
        );
    }


    // ============================================================
    // 댓글 삭제
    // ============================================================

    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @AuthenticationPrincipal
            AuthenticatedUser authenticatedUser,

            @PathVariable
            Long commentId
    ) {

        commentService.deleteComment(
                authenticatedUser.userId(),
                commentId
        );


        return ResponseEntity.noContent().build();
    }
}