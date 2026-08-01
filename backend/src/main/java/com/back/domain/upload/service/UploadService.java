package com.back.domain.upload.service;

import com.back.domain.upload.dto.PresignRequest;
import com.back.domain.upload.dto.PresignResponse;
import com.back.global.exception.CustomException;
import com.back.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UploadService {

    private static final List<String> ALLOWED_CONTENT_TYPES =
            List.of("image/png", "image/jpeg", "image/webp", "image/gif");
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final Duration PRESIGN_DURATION = Duration.ofMinutes(5);

    private final S3Presigner s3Presigner;

    @Value("${r2.bucket}")
    private String bucket;

    @Value("${r2.public-base-url}")
    private String publicBaseUrl;

    /**
     * 게시글 이미지 업로드용 Presigned URL 발급
     * - contentType, size 검증 후 R2에 PUT 가능한 임시 서명 URL 생성
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

    private String generateKey(Long userId, String filename) {
        String extension = extractExtension(filename);
        return "posts/%d/%s%s".formatted(userId, UUID.randomUUID(), extension);
    }

    private String extractExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        return dotIndex >= 0 ? filename.substring(dotIndex) : "";
    }
}
