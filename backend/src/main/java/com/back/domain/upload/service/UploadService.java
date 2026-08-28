package com.back.domain.upload.service;

import com.back.domain.auth.domain.User;
import com.back.domain.auth.repository.UserRepository;
import com.back.domain.upload.domain.UploadedImage;
import com.back.domain.upload.dto.PresignRequest;
import com.back.domain.upload.dto.PresignResponse;
import com.back.domain.upload.repository.UploadedImageRepository;
import com.back.global.exception.CustomException;
import com.back.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class UploadService {

    private static final List<String> ALLOWED_CONTENT_TYPES =
            List.of("image/png", "image/jpeg", "image/webp", "image/gif");
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final Duration PRESIGN_DURATION = Duration.ofMinutes(5);
    private static final Pattern IMG_SRC_PATTERN = Pattern.compile("<img[^>]+src=\"([^\"]+)\"");

    private final S3Presigner s3Presigner;
    private final UserRepository userRepository;
    private final UploadedImageRepository uploadedImageRepository;

    @Value("${r2.bucket}")
    private String bucket;

    @Value("${r2.public-base-url}")
    private String publicBaseUrl;

    /**
     * 게시글 이미지 업로드용 Presigned URL 발급
     * - contentType, size 검증 후 R2에 PUT 가능한 임시 서명 URL 생성
     * - 동시에 uploaded_image에 PENDING 상태로 기록 (고아 이미지 추적용)
     */
    public PresignResponse createPresignedUrl(Long userId, PresignRequest request) {
        validate(request);

        String key = generateKey(userId, request.getFilename());

        PutObjectRequest objectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(request.getContentType())
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(PRESIGN_DURATION)
                .putObjectRequest(objectRequest)
                .build();

        PresignedPutObjectRequest presigned = s3Presigner.presignPutObject(presignRequest);

        User uploader = userRepository.getReferenceById(userId);
        uploadedImageRepository.save(
                UploadedImage.builder()
                        .key(key)
                        .uploader(uploader)
                        .build()
        );

        return PresignResponse.builder()
                .uploadUrl(presigned.url().toString())
                .fileUrl(publicBaseUrl + "/" + key)
                .key(key)
                .expiresIn(PRESIGN_DURATION.toSeconds())
                .build();
    }

    private void validate(PresignRequest request) {
        if (!ALLOWED_CONTENT_TYPES.contains(request.getContentType())) {
            throw new CustomException(ErrorCode.UNSUPPORTED_MEDIA_TYPE);
        }
        if (request.getSize() > MAX_FILE_SIZE) {
            throw new CustomException(ErrorCode.FILE_TOO_LARGE);
        }
    }

    /**
     * 게시글 본문(content)에서 실제로 쓰인 우리 R2 이미지 key만 찾아 COMMITTED로 전환
     * - 작성/수정 시 PostService에서 호출
     * - 매칭 안 된(=본문에서 빠진) 이미지는 PENDING으로 남아 스케줄러가 나중에 정리
     */
    @Transactional
    public void commitImages(String content) {
        List<String> keys = extractKeys(content);
        if (keys.isEmpty()) {
            return;
        }

        List<UploadedImage> images = uploadedImageRepository.findAllByKeyIn(keys);
        images.forEach(UploadedImage::commit);
    }

    private List<String> extractKeys(String content) {
        String prefix = publicBaseUrl + "/";
        Matcher matcher = IMG_SRC_PATTERN.matcher(content);
        List<String> keys = new ArrayList<>();
        while (matcher.find()) {
            String src = matcher.group(1);
            if (src.startsWith(prefix)) {
                keys.add(src.substring(prefix.length()));
            }
        }
        return keys;
    }

    /**
     * 게시글 삭제 시, 그 게시글이 쓰던 이미지들을 다시 PENDING으로 되돌림
     * - 실제 R2/DB 삭제는 하지 않고, 스케줄러(OrphanImageCleaner)가 나중에 정리하도록 표시만 함
     */
    @Transactional
    public void uncommitImages(String content) {
        List<String> keys = extractKeys(content);
        if (keys.isEmpty()) {
            return;
        }

        List<UploadedImage> images = uploadedImageRepository.findAllByKeyIn(keys);
        images.forEach(UploadedImage::uncommit);
    }

    private String generateKey(Long userId, String filename) {
        String extension = extractExtension(filename);
        return "posts/%d/%s%s".formatted(userId, UUID.randomUUID(), extension);
    }

    private String extractExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        return dotIndex >= 0 ? filename.substring(dotIndex) : "";
    }
}
