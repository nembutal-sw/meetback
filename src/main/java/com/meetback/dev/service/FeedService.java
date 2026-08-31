package com.meetback.dev.service;

import com.meetback.dev.domain.Feed;
import com.meetback.dev.domain.FeedImage;
import com.meetback.dev.dto.feed.FeedCreateRequest;
import com.meetback.dev.dto.feed.FeedImageResponse;
import com.meetback.dev.dto.feed.FeedResponse;
import com.meetback.dev.dto.feed.FeedUpdateRequest;
import com.meetback.dev.repository.FeedImageMapper;
import com.meetback.dev.repository.FeedLikeMapper;
import com.meetback.dev.repository.FeedMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class FeedService {

    private static final Logger log =
            LoggerFactory.getLogger(
                    FeedService.class
            );

    private static final int MAX_IMAGE_COUNT =
            10;

    private static final long MAX_IMAGE_SIZE =
            5 * 1024 * 1024;

    private static final Set<String> ALLOWED_CONTENT_TYPES =
            Set.of(
                    "image/jpeg",
                    "image/png",
                    "image/gif",
                    "image/webp"
            );

    private final FeedMapper feedMapper;

    private final FeedImageMapper feedImageMapper;

    private final FeedLikeMapper feedLikeMapper;

    private final Path uploadDirectory;


    public FeedService(
            FeedMapper feedMapper,
            FeedImageMapper feedImageMapper,
            FeedLikeMapper feedLikeMapper,
            @Value("${feed.image.upload-dir}")
            String uploadDirectory
    ) {

        this.feedMapper =
                feedMapper;

        this.feedImageMapper =
                feedImageMapper;

        this.feedLikeMapper =
                feedLikeMapper;

        this.uploadDirectory =
                Paths.get(
                                uploadDirectory
                        )
                        .toAbsolutePath()
                        .normalize();
    }


    // ============================================================
    // 후기 등록
    // ============================================================

    @Transactional
    public FeedResponse createFeed(
            Long userId,
            FeedCreateRequest request,
            List<MultipartFile> images
    ) {

        validateUserId(
                userId
        );

        validateCreateRequest(
                request
        );

        validateImages(
                images
        );


        Feed feed =
                new Feed();


        feed.setUserId(
                userId
        );


        feed.setTitle(
                request
                        .getTitle()
                        .trim()
        );


        feed.setContent(
                request
                        .getContent()
                        .trim()
        );


        int insertedFeedCount =
                feedMapper.insert(
                        feed
                );


        if (
                insertedFeedCount != 1
        ) {

            throw new IllegalStateException(
                    "후기 등록에 실패했습니다."
            );
        }


        Long feedId =
                feed.getFeedId();


        if (
                feedId == null
        ) {

            throw new IllegalStateException(
                    "생성된 후기 ID를 확인할 수 없습니다."
            );
        }


        List<Path> savedFilePaths =
                new ArrayList<>();


        try {

            if (
                    images != null
            ) {

                int sortOrder =
                        0;


                for (
                        MultipartFile image
                        : images
                ) {

                    if (
                            image == null
                                    ||
                                    image.isEmpty()
                    ) {

                        continue;
                    }


                    FeedImage feedImage =
                            saveImage(
                                    feedId,
                                    image,
                                    sortOrder
                            );


                    savedFilePaths.add(
                            uploadDirectory.resolve(
                                    feedImage.getStoredName()
                            )
                    );


                    int insertedImageCount =
                            feedImageMapper.insert(
                                    feedImage
                            );


                    if (
                            insertedImageCount != 1
                    ) {

                        throw new IllegalStateException(
                                "이미지 정보 저장에 실패했습니다."
                        );
                    }


                    sortOrder++;
                }
            }


            return getFeed(
                    feedId
            );

        }
        catch (
                RuntimeException e
        ) {

            deleteSavedFiles(
                    savedFilePaths
            );

            throw e;
        }
    }


    // ============================================================
    // 후기 단건 조회
    // ============================================================

    @Transactional(readOnly = true)
    public FeedResponse getFeed(
            Long feedId
    ) {

        if (
                feedId == null
        ) {

            throw new IllegalArgumentException(
                    "후기 ID가 필요합니다."
            );
        }


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


        List<FeedImage> feedImages =
                feedImageMapper.findByFeedId(
                        feedId
                );


        return toFeedResponse(
                feed,
                feedImages
        );
    }


    // ============================================================
    // 후기 전체 조회
    // ============================================================

    @Transactional(readOnly = true)
    public List<FeedResponse> getFeeds(
            Long loginUserId
    ) {

        List<Feed> feeds =
                feedMapper.findAll();


        List<FeedResponse> responses =
                new ArrayList<>();


        for (
                Feed feed
                : feeds
        ) {

            List<FeedImage> images =
                    feedImageMapper.findByFeedId(
                            feed.getFeedId()
                    );


            responses.add(
                    toFeedResponse(
                            feed,
                            images,
                            loginUserId
                    )
            );
        }


        return responses;
    }


    // ============================================================
    // 후기 수정
    // ============================================================

    @Transactional
    public FeedResponse updateFeed(
            Long userId,
            Long feedId,
            FeedUpdateRequest request
    ) {

        validateUserId(
                userId
        );


        if (
                feedId == null
        ) {

            throw new IllegalArgumentException(
                    "후기 ID가 필요합니다."
            );
        }


        validateUpdateRequest(
                request
        );


        Feed existingFeed =
                feedMapper.findById(
                        feedId
                );


        if (
                existingFeed == null
        ) {

            throw new IllegalArgumentException(
                    "존재하지 않는 후기입니다."
            );
        }


        if (
                !userId.equals(
                        existingFeed.getUserId()
                )
        ) {

            throw new IllegalArgumentException(
                    "본인이 작성한 후기만 수정할 수 있습니다."
            );
        }


        Feed feed =
                new Feed();


        feed.setFeedId(
                feedId
        );


        feed.setUserId(
                userId
        );


        feed.setTitle(
                request
                        .getTitle()
                        .trim()
        );


        feed.setContent(
                request
                        .getContent()
                        .trim()
        );


        int updatedCount =
                feedMapper.update(
                        feed
                );


        if (
                updatedCount != 1
        ) {

            throw new IllegalStateException(
                    "후기 수정에 실패했습니다."
            );
        }


        return getFeed(
                feedId
        );
    }


    // ============================================================
    // 후기 삭제
    // ============================================================

    @Transactional
    public void deleteFeed(
            Long userId,
            Long feedId
    ) {

        validateUserId(
                userId
        );


        if (
                feedId == null
        ) {

            throw new IllegalArgumentException(
                    "후기 ID가 필요합니다."
            );
        }


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


        if (
                !userId.equals(
                        feed.getUserId()
                )
        ) {

            throw new IllegalArgumentException(
                    "본인이 작성한 후기만 삭제할 수 있습니다."
            );
        }


        int deletedCount =
                feedMapper.softDelete(
                        feedId,
                        userId
                );


        if (
                deletedCount != 1
        ) {

            throw new IllegalStateException(
                    "후기 삭제에 실패했습니다."
            );
        }
    }


    // ============================================================
    // 이미지 저장
    // ============================================================

    private FeedImage saveImage(
            Long feedId,
            MultipartFile image,
            int sortOrder
    ) {

        try {

            Files.createDirectories(
                    uploadDirectory
            );


            String originalName =
                    image.getOriginalFilename();


            if (
                    originalName == null
                            ||
                            originalName.isBlank()
            ) {

                throw new IllegalArgumentException(
                        "이미지 파일명이 올바르지 않습니다."
                );
            }


            originalName =
                    cleanOriginalName(
                            originalName
                    );


            if (
                    originalName.length()
                            > 255
            ) {

                throw new IllegalArgumentException(
                        "이미지 파일명이 너무 깁니다."
                );
            }


            String extension =
                    getExtension(
                            originalName
                    );


            String storedName =
                    UUID.randomUUID()
                            + extension;


            Path destination =
                    uploadDirectory
                            .resolve(
                                    storedName
                            )
                            .normalize();


            if (
                    !destination.startsWith(
                            uploadDirectory
                    )
            ) {

                throw new IllegalArgumentException(
                        "올바르지 않은 이미지 저장 경로입니다."
                );
            }


            try (
                    InputStream inputStream =
                            image.getInputStream()
            ) {

                Files.copy(
                        inputStream,
                        destination,
                        StandardCopyOption.REPLACE_EXISTING
                );
            }


            FeedImage feedImage =
                    new FeedImage();


            feedImage.setFeedId(
                    feedId
            );


            feedImage.setImageUrl(
                    "/uploads/feed/"
                            + storedName
            );


            feedImage.setOriginalName(
                    originalName
            );


            feedImage.setStoredName(
                    storedName
            );


            feedImage.setSortOrder(
                    sortOrder
            );


            feedImage.setCreatedAt(
                    LocalDateTime.now()
            );


            return feedImage;

        }
        catch (
                IOException e
        ) {

            log.error(
                    "피드 이미지 저장 실패. uploadDirectory={}, originalFilename={}",
                    uploadDirectory,
                    image.getOriginalFilename(),
                    e
            );


            throw new IllegalStateException(
                    "이미지 저장 중 오류가 발생했습니다.",
                    e
            );
        }
    }


    // ============================================================
    // Feed -> FeedResponse
    //
    // 단건 조회 / 등록 / 수정 응답용
    // ============================================================

    private FeedResponse toFeedResponse(
            Feed feed,
            List<FeedImage> images
    ) {

        return toFeedResponse(
                feed,
                images,
                null
        );
    }


    // ============================================================
    // Feed -> FeedResponse
    //
    // 목록 조회 시 로그인 사용자 ID를 받아
    // 본인 피드 여부(mine)까지 판단
    // ============================================================

    private FeedResponse toFeedResponse(
            Feed feed,
            List<FeedImage> images,
            Long loginUserId
    ) {

        List<FeedImageResponse> imageResponses =
                new ArrayList<>();


        for (
                FeedImage image
                : images
        ) {

            FeedImageResponse imageResponse =
                    new FeedImageResponse(
                            image.getFeedImageId(),
                            image.getImageUrl(),
                            image.getOriginalName(),
                            image.getSortOrder()
                    );


            imageResponses.add(
                    imageResponse
            );
        }


        // ========================================================
        // 작성자 닉네임 조회
        // FeedMapper에서 users 테이블과 JOIN해서 가져온 값
        // ========================================================

        String nickname =
                feed.getNickname();


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


        // ========================================================
        // 현재 로그인 사용자가 작성한 피드인지 확인
        // ========================================================

        boolean mine =
                loginUserId != null
                        &&
                        feed.getUserId() != null
                        &&
                        loginUserId.equals(
                                feed.getUserId()
                        );


        // ========================================================
        // 좋아요 개수 조회
        // ========================================================

        int likeCount =
                feedLikeMapper.countByFeedId(
                        feed.getFeedId()
                );


        // ========================================================
        // 현재 로그인 사용자가 좋아요를 눌렀는지 확인
        // ========================================================

        boolean liked =
                loginUserId != null
                        &&
                        feedLikeMapper.existsByFeedIdAndUserId(
                                feed.getFeedId(),
                                loginUserId
                        ) > 0;


        return new FeedResponse(
                feed.getFeedId(),
                feed.getUserId(),
                nickname,
                mine,
                feed.getTitle(),
                feed.getContent(),
                imageResponses,
                likeCount,
                liked,
                feed.getCreatedAt(),
                feed.getUpdatedAt()
        );
    }


    // ============================================================
    // 작성 요청 검증
    // ============================================================

    private void validateCreateRequest(
            FeedCreateRequest request
    ) {

        if (
                request == null
        ) {

            throw new IllegalArgumentException(
                    "후기 작성 정보가 필요합니다."
            );
        }


        validateTitleAndContent(
                request.getTitle(),
                request.getContent()
        );
    }


    // ============================================================
    // 수정 요청 검증
    // ============================================================

    private void validateUpdateRequest(
            FeedUpdateRequest request
    ) {

        if (
                request == null
        ) {

            throw new IllegalArgumentException(
                    "후기 수정 정보가 필요합니다."
            );
        }


        validateTitleAndContent(
                request.getTitle(),
                request.getContent()
        );
    }


    // ============================================================
    // 제목 / 내용 검증
    // ============================================================

    private void validateTitleAndContent(
            String title,
            String content
    ) {

        if (
                title == null
                        ||
                        title.isBlank()
        ) {

            throw new IllegalArgumentException(
                    "제목을 입력해주세요."
            );
        }


        if (
                title
                        .trim()
                        .length()
                        > 255
        ) {

            throw new IllegalArgumentException(
                    "제목은 255자 이하로 입력해주세요."
            );
        }


        if (
                content == null
                        ||
                        content.isBlank()
        ) {

            throw new IllegalArgumentException(
                    "후기 내용을 입력해주세요."
            );
        }
    }


    // ============================================================
    // 이미지 검증
    // ============================================================

    private void validateImages(
            List<MultipartFile> images
    ) {

        if (
                images == null
        ) {

            return;
        }


        int imageCount =
                0;


        for (
                MultipartFile image
                : images
        ) {

            if (
                    image == null
                            ||
                            image.isEmpty()
            ) {

                continue;
            }


            imageCount++;


            if (
                    imageCount
                            > MAX_IMAGE_COUNT
            ) {

                throw new IllegalArgumentException(
                        "이미지는 최대 "
                                + MAX_IMAGE_COUNT
                                + "장까지 업로드할 수 있습니다."
                );
            }


            if (
                    image.getSize()
                            > MAX_IMAGE_SIZE
            ) {

                throw new IllegalArgumentException(
                        "이미지 한 장의 크기는 5MB를 초과할 수 없습니다."
                );
            }


            String contentType =
                    image.getContentType();


            if (
                    contentType == null
                            ||
                            !ALLOWED_CONTENT_TYPES.contains(
                                    contentType
                            )
            ) {

                throw new IllegalArgumentException(
                        "JPG, PNG, GIF, WEBP 이미지만 업로드할 수 있습니다."
                );
            }
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
    // 원본 파일명 정리
    // ============================================================

    private String cleanOriginalName(
            String originalName
    ) {

        String cleanedName =
                originalName.replace(
                        "\\",
                        "/"
                );


        int lastSlash =
                cleanedName.lastIndexOf(
                        "/"
                );


        if (
                lastSlash >= 0
        ) {

            cleanedName =
                    cleanedName.substring(
                            lastSlash + 1
                    );
        }


        return cleanedName;
    }


    // ============================================================
    // 확장자 추출
    // ============================================================

    private String getExtension(
            String originalName
    ) {

        int dotIndex =
                originalName.lastIndexOf(
                        "."
                );


        if (
                dotIndex < 0
                        ||
                        dotIndex
                                == originalName.length() - 1
        ) {

            return "";
        }


        return originalName
                .substring(
                        dotIndex
                )
                .toLowerCase();
    }


    // ============================================================
    // 저장 실패 시 파일 삭제
    // ============================================================

    private void deleteSavedFiles(
            List<Path> savedFilePaths
    ) {

        for (
                Path path
                : savedFilePaths
        ) {

            try {

                Files.deleteIfExists(
                        path
                );

            }
            catch (
                    IOException e
            ) {

                log.warn(
                        "피드 이미지 롤백 삭제 실패. path={}",
                        path,
                        e
                );
            }
        }
    }
}