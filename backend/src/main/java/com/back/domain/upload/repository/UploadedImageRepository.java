package com.back.domain.upload.repository;

import com.back.domain.upload.domain.UploadedImage;
import com.back.domain.upload.domain.UploadStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UploadedImageRepository extends JpaRepository<UploadedImage, Long> {

    Optional<UploadedImage> findByKey(String key);

    List<UploadedImage> findAllByKeyIn(List<String> keys);

    List<UploadedImage> findAllByStatusAndCreatedAtBefore(UploadStatus status, LocalDateTime threshold);
}
