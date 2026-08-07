package com.back.domain.upload.scheduler;

import com.back.domain.upload.domain.UploadStatus;
import com.back.domain.upload.domain.UploadedImage;
import com.back.domain.upload.repository.UploadedImageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 오래 방치된 PENDING 이미지를 주기적으로 찾아 R2와 DB에서 함께 정리
 * - 게시글에 커밋되지 못한 채(작성 취소 등) 남겨진 고아 이미지 대상
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrphanImageCleaner {

    private final UploadedImageRepository uploadedImageRepository;
    private final S3Client s3Client;

    @Value("${r2.bucket}")
    private String bucket;

    @Value("${orphan.pending-expiry-days:14}")
    private int pendingExpiryDays;

    @Scheduled(cron = "${orphan.cleanup-cron:0 0 4 * * *}")
    public void cleanup() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(pendingExpiryDays);
        List<UploadedImage> expired =
                uploadedImageRepository.findAllByStatusAndCreatedAtBefore(UploadStatus.PENDING, threshold);

        if (expired.isEmpty()) {
            return;
        }

        log.info("고아 이미지 청소 시작 - 대상 {}건", expired.size());

        for (UploadedImage image : expired) {
            try {
                s3Client.deleteObject(DeleteObjectRequest.builder()
                        .bucket(bucket)
                        .key(image.getKey())
                        .build());
                uploadedImageRepository.delete(image);
            } catch (Exception e) {
                log.error("고아 이미지 삭제 실패 - key={}", image.getKey(), e);
            }
        }

        log.info("고아 이미지 청소 완료 - {}건 삭제", expired.size());
    }
}
