package com.back.domain.chat.controller;

import com.back.domain.chat.dto.ChatRoomMemberResponse;
import com.back.domain.chat.service.ChatRoomMemberService;
import com.back.global.exception.CustomException;
import com.back.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat-rooms")
@RequiredArgsConstructor
public class ChatRoomController {

    private final ChatRoomMemberService chatRoomMemberService;

    /**
     * POST /api/chat-rooms/{roomId}/join-requests
     * 채팅방 참여 요청
     */
    @PostMapping("/{roomId}/join-requests")
    public ResponseEntity<Void> requestJoin(
            @PathVariable Long roomId,
            Authentication authentication
    ) {
        Long userId = getUserId(authentication);
        chatRoomMemberService.requestJoin(userId, roomId);
        return ResponseEntity.noContent().build();
    }

    /**
     * GET /api/chat-rooms/{roomId}/join-requests
     * 대기 중인 참여 요청 목록 조회 (방장만)
     */
    @GetMapping("/{roomId}/join-requests")
    public ResponseEntity<List<ChatRoomMemberResponse>> getPendingRequests(
            @PathVariable Long roomId,
            Authentication authentication
    ) {
        Long userId = getUserId(authentication);
        List<ChatRoomMemberResponse> response = chatRoomMemberService.getPendingRequests(userId, roomId);
        return ResponseEntity.ok(response);
    }

    /**
     * PATCH /api/chat-rooms/{roomId}/join-requests/{memberId}/accept
     * 참여 요청 수락 (방장만)
     */
    @PatchMapping("/{roomId}/join-requests/{memberId}/accept")
    public ResponseEntity<Void> acceptRequest(
            @PathVariable Long roomId,
            @PathVariable Long memberId,
            Authentication authentication
    ) {
        Long userId = getUserId(authentication);
        chatRoomMemberService.acceptRequest(userId, roomId, memberId);
        return ResponseEntity.noContent().build();
    }

    /**
     * PATCH /api/chat-rooms/{roomId}/join-requests/{memberId}/reject
     * 참여 요청 거절 (방장만)
     */
    @PatchMapping("/{roomId}/join-requests/{memberId}/reject")
    public ResponseEntity<Void> rejectRequest(
            @PathVariable Long roomId,
            @PathVariable Long memberId,
            Authentication authentication
    ) {
        Long userId = getUserId(authentication);
        chatRoomMemberService.rejectRequest(userId, roomId, memberId);
        return ResponseEntity.noContent().build();
    }

    private Long getUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }
        return (Long) authentication.getPrincipal();
    }
}
