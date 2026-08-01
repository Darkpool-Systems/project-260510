package com.back.domain.upload.controller;

import com.back.domain.upload.dto.PresignRequest;
import com.back.domain.upload.dto.PresignResponse;
import com.back.domain.upload.service.UploadService;
import com.back.global.exception.CustomException;
import com.back.global.exception.ErrorCode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/uploads")
@RequiredArgsConstructor
public class UploadController {

    private final UploadService uploadService;

    /**
     * POST /api/uploads/presign
     * 게시글 이미지 업로드용 Presigned URL 발급
     */
    @PostMapping("/presign")
    public ResponseEntity<PresignResponse> presign(
            @Valid @RequestBody PresignRequest request,
            Authentication authentication
    ) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }

        Long userId = (Long) authentication.getPrincipal();
        PresignResponse response = uploadService.createPresignedUrl(userId, request);

        return ResponseEntity.ok(response);
    }
}
