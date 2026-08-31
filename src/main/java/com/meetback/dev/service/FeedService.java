package com.meetback.dev.service;

import com.meetback.dev.domain.Feed;
import com.meetback.dev.domain.FeedImage;
import com.meetback.dev.dto.feed.FeedCreateRequest;
import com.meetback.dev.dto.feed.FeedImageResponse;
import com.meetback.dev.dto.feed.FeedPageResponse;
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
    // 후기 페이징 조회
    // ============================================================

    @Transactional(readOnly = true)
    public FeedPageResponse getFeeds(
            Long loginUserId,
            int page,
            int size
    ) {

        validateUserId(
                loginUserId
        );


        if (
                page < 0
        ) {

            throw new IllegalArgumentException(
                    "페이지 번호는 0 이상이어야 합니다."
            );
        }


        if (
                size <= 0
                        ||
                        size > 50
        ) {

            throw new IllegalArgumentException(
                    "페이지 크기는 1~50 사이여야 합니다."
            );
        }


        // ========================================================
        // 전체 피드 개수
        //
        // 반드시 long
        // ========================================================

        long totalElements =
                feedMapper.countAll();


        int totalPages;


        if (
                totalElements == 0
        ) {

            totalPages =
                    0;

        }
        else {

            totalPages =
                    (int) (
                            (totalElements + size - 1)
                                    /
                                    size
                    );
        }


        int offset =
                page * size;


        List<Feed> feeds =
                feedMapper.findPage(
                        offset,
                        size
                );


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


        return new FeedPageResponse(
                responses,
                page,
                size,
                totalElements,
                totalPages
        );
    }


    // ============================================================
    // 후기 수정
    //
    // 기존 이미지
    // → 기본적으로 그대로 유지
    //
    // 사용자가 X 버튼을 누른 기존 이미지
    // → deleteImageIds에 담아서 해당 이미지만 삭제
    //
    // 새로 선택한 이미지
    // → 기존 이미지 뒤에 추가
    // ============================================================

    @Transactional
    public FeedResponse updateFeed(
            Long userId,
            Long feedId,
            FeedUpdateRequest request,
            List<MultipartFile> images,
            List<Long> deleteImageIds
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


        // ========================================================
        // 새로 추가할 이미지 자체 검증
        // ========================================================

        validateImages(
                images
        );


        // ========================================================
        // 기존 피드 조회
        // ========================================================

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


        // ========================================================
        // 작성자 확인
        // ========================================================

        if (
                existingFeed.getUserId() == null
                        ||
                        !userId.equals(
                                existingFeed.getUserId()
                        )
        ) {

            throw new IllegalArgumentException(
                    "본인이 작성한 후기만 수정할 수 있습니다."
            );
        }


        // ========================================================
        // 현재 DB에 저장된 기존 이미지 조회
        // ========================================================

        List<FeedImage> existingImages =
                feedImageMapper.findByFeedId(
                        feedId
                );


        // ========================================================
        // 실제 삭제 대상 이미지 찾기
        //
        // 클라이언트가 deleteImageIds를 보내더라도
        // 현재 피드에 실제로 속해있는 이미지만 삭제 대상으로 인정
        // ========================================================

        List<FeedImage> imagesToDelete =
                new ArrayList<>();


        if (
                deleteImageIds != null
                        &&
                        !deleteImageIds.isEmpty()
        ) {

            for (
                    FeedImage existingImage
                    : existingImages
            ) {

                if (
                        existingImage.getFeedImageId() == null
                ) {

                    continue;
                }


                if (
                        deleteImageIds.contains(
                                existingImage.getFeedImageId()
                        )
                ) {

                    imagesToDelete.add(
                            existingImage
                    );
                }
            }
        }


        // ========================================================
        // 새로 추가하는 이미지 개수
        // ========================================================

        int newImageCount =
                countNewImages(
                        images
                );


        // ========================================================
        // 최종 이미지 개수 계산
        //
        // 기존 이미지
        // - 삭제할 이미지
        // + 새 이미지
        // ========================================================

        int finalImageCount =
                existingImages.size()
                        -
                        imagesToDelete.size()
                        +
                        newImageCount;


        if (
                finalImageCount > MAX_IMAGE_COUNT
        ) {

            throw new IllegalArgumentException(
                    "이미지는 최대 "
                            + MAX_IMAGE_COUNT
                            + "장까지 등록할 수 있습니다."
            );
        }


        // ========================================================
        // 제목 / 내용 수정
        // ========================================================

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


        // ========================================================
        // 사용자가 직접 삭제한 기존 이미지 DB 삭제
        // ========================================================

        for (
                FeedImage imageToDelete
                : imagesToDelete
        ) {

            int deletedImageCount =
                    feedImageMapper.deleteByIdAndFeedId(
                            imageToDelete.getFeedImageId(),
                            feedId
                    );


            if (
                    deletedImageCount != 1
            ) {

                throw new IllegalStateException(
                        "기존 이미지 삭제에 실패했습니다."
                );
            }
        }


        // ========================================================
        // 삭제 후 현재 남아있는 이미지 조회
        // ========================================================

        List<FeedImage> remainingImages =
                feedImageMapper.findByFeedId(
                        feedId
                );


        // ========================================================
        // 새 이미지 sortOrder 시작값
        //
        // 기존 이미지 중 가장 큰 sortOrder 다음부터 추가
        // ========================================================

        int nextSortOrder =
                0;


        for (
                FeedImage remainingImage
                : remainingImages
        ) {

            if (
                    remainingImage.getSortOrder() != null
                            &&
                            remainingImage.getSortOrder() >= nextSortOrder
            ) {

                nextSortOrder =
                        remainingImage.getSortOrder()
                                + 1;
            }
        }


        // ========================================================
        // 새로 저장된 파일 경로
        //
        // 새 이미지 처리 실패 시 실제 파일 롤백용
        // ========================================================

        List<Path> newlySavedFilePaths =
                new ArrayList<>();


        try {

            // ====================================================
            // 새 이미지 추가
            // ====================================================

            if (
                    images != null
            ) {

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


                    FeedImage newFeedImage =
                            saveImage(
                                    feedId,
                                    image,
                                    nextSortOrder
                            );


                    newlySavedFilePaths.add(
                            uploadDirectory.resolve(
                                    newFeedImage.getStoredName()
                            )
                    );


                    int insertedImageCount =
                            feedImageMapper.insert(
                                    newFeedImage
                            );


                    if (
                            insertedImageCount != 1
                    ) {

                        throw new IllegalStateException(
                                "새 이미지 추가에 실패했습니다."
                        );
                    }


                    nextSortOrder++;
                }
            }


            // ====================================================
            // DB 작업과 새 이미지 저장 완료 후
            // 사용자가 삭제한 기존 실제 파일 제거
            // ====================================================

            deleteFeedImageFiles(
                    imagesToDelete
            );


            return getFeed(
                    feedId
            );

        }
        catch (
                RuntimeException e
        ) {

            // ====================================================
            // 새로 저장했던 파일만 제거
            //
            // DB 작업은 @Transactional에 의해 롤백
            // ====================================================

            deleteSavedFiles(
                    newlySavedFilePaths
            );


            throw e;
        }
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
                feed.getUserId() == null
                        ||
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
                    originalName.length() > 255
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
    // 로그인 사용자가 있으면
    // mine / liked 계산
    // ============================================================

    private FeedResponse toFeedResponse(
            Feed feed,
            List<FeedImage> images,
            Long loginUserId
    ) {

        List<FeedImageResponse> imageResponses =
                new ArrayList<>();


        if (
                images != null
        ) {

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
        }


        // ========================================================
        // 닉네임
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
        // 본인 피드 여부
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
        // 좋아요 개수
        // ========================================================

        int likeCount =
                feedLikeMapper.countByFeedId(
                        feed.getFeedId()
                );


        // ========================================================
        // 현재 사용자가 좋아요를 눌렀는지
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
                        .length() > 255
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
                    imageCount > MAX_IMAGE_COUNT
            ) {

                throw new IllegalArgumentException(
                        "이미지는 최대 "
                                + MAX_IMAGE_COUNT
                                + "장까지 업로드할 수 있습니다."
                );
            }


            if (
                    image.getSize() > MAX_IMAGE_SIZE
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
    // 로그인 사용자 ID 검증
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
    // 새로 추가하는 이미지 개수
    // ============================================================

    private int countNewImages(
            List<MultipartFile> images
    ) {

        if (
                images == null
        ) {

            return 0;
        }


        int count =
                0;


        for (
                MultipartFile image
                : images
        ) {

            if (
                    image != null
                            &&
                            !image.isEmpty()
            ) {

                count++;
            }
        }


        return count;
    }


    // ============================================================
    // 기존 피드 이미지 실제 파일 삭제
    // ============================================================

    private void deleteFeedImageFiles(
            List<FeedImage> images
    ) {

        if (
                images == null
        ) {

            return;
        }


        for (
                FeedImage image
                : images
        ) {

            if (
                    image == null
                            ||
                            image.getStoredName() == null
                            ||
                            image.getStoredName().isBlank()
            ) {

                continue;
            }


            Path imagePath =
                    uploadDirectory
                            .resolve(
                                    image.getStoredName()
                            )
                            .normalize();


            if (
                    !imagePath.startsWith(
                            uploadDirectory
                    )
            ) {

                log.warn(
                        "기존 피드 이미지 삭제 경로가 올바르지 않습니다. path={}",
                        imagePath
                );


                continue;
            }


            try {

                Files.deleteIfExists(
                        imagePath
                );

            }
            catch (
                    IOException e
            ) {

                log.warn(
                        "기존 피드 이미지 파일 삭제 실패. path={}",
                        imagePath,
                        e
                );
            }
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
                        dotIndex == originalName.length() - 1
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
    // 새 이미지 저장 실패 시 파일 롤백
    // ============================================================

    private void deleteSavedFiles(
            List<Path> savedFilePaths
    ) {

        if (
                savedFilePaths == null
        ) {

            return;
        }


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