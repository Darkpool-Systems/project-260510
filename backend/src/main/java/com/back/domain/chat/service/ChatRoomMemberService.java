package com.back.domain.chat.service;

import com.back.domain.auth.domain.User;
import com.back.domain.auth.repository.UserRepository;
import com.back.domain.chat.domain.ChatRoom;
import com.back.domain.chat.domain.ChatRoomMember;
import com.back.domain.chat.domain.ChatRoomMemberRole;
import com.back.domain.chat.domain.ChatRoomMemberStatus;
import com.back.domain.chat.dto.ChatRoomMemberResponse;
import com.back.domain.chat.dto.LiveKitTokenResponse;
import com.back.domain.chat.repository.ChatRoomMemberRepository;
import com.back.domain.chat.repository.ChatRoomRepository;
import com.back.global.exception.CustomException;
import com.back.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatRoomMemberService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final UserRepository userRepository;
    private final LiveKitTokenService liveKitTokenService;

    /**
     * 채팅방 참여 요청 - PENDING 상태로 생성
     */
    @Transactional
    public void requestJoin(Long userId, Long roomId) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHAT_ROOM_NOT_FOUND));

        if (room.getOwner().getId().equals(userId)) {
            throw new CustomException(ErrorCode.CHAT_ROOM_OWNER_CANNOT_JOIN);
        }

        if (chatRoomMemberRepository.existsByRoomIdAndUserId(roomId, userId)) {
            throw new CustomException(ErrorCode.CHAT_ROOM_ALREADY_REQUESTED);
        }

        User user = userRepository.getReferenceById(userId);

        chatRoomMemberRepository.save(
                ChatRoomMember.builder()
                        .room(room)
                        .user(user)
                        .role(ChatRoomMemberRole.MEMBER)
                        .build()
        );
    }

    /**
     * 대기 중인 참여 요청 목록 조회 - 방장만 가능
     */
    @Transactional(readOnly = true)
    public List<ChatRoomMemberResponse> getPendingRequests(Long ownerId, Long roomId) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHAT_ROOM_NOT_FOUND));

        validateOwner(room, ownerId);

        return chatRoomMemberRepository.findAllByRoomIdAndStatus(roomId, ChatRoomMemberStatus.PENDING)
                .stream()
                .map(ChatRoomMemberResponse::from)
                .toList();
    }

    /**
     * 참여 요청 수락 - 방장만 가능, 인원 초과 시 거부
     */
    @Transactional
    public void acceptRequest(Long ownerId, Long roomId, Long memberId) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHAT_ROOM_NOT_FOUND));

        validateOwner(room, ownerId);

        ChatRoomMember member = getPendingMember(roomId, memberId);

        long acceptedCount = chatRoomMemberRepository.countByRoomIdAndStatus(roomId, ChatRoomMemberStatus.ACCEPTED);
        if (acceptedCount >= room.getMaxUsers() - 1) {  // maxUsers는 방장 포함 총원이라 방장 1자리를 미리 뺌
            throw new CustomException(ErrorCode.CHAT_ROOM_FULL);
        }

        member.accept();
    }

    /**
     * 참여 요청 거절 - 방장만 가능
     */
    @Transactional
    public void rejectRequest(Long ownerId, Long roomId, Long memberId) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHAT_ROOM_NOT_FOUND));

        validateOwner(room, ownerId);

        ChatRoomMember member = getPendingMember(roomId, memberId);
        member.reject();
    }

    /**
     * 채팅방 입장 가능 여부 - 방장이거나 ACCEPTED 상태의 멤버여야 함
     */
    @Transactional(readOnly = true)
    public boolean canEnter(Long userId, Long roomId) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHAT_ROOM_NOT_FOUND));

        if (room.getOwner().getId().equals(userId)) {
            return true;
        }

        return chatRoomMemberRepository.existsByRoomIdAndUserIdAndStatus(
                roomId, userId, ChatRoomMemberStatus.ACCEPTED
        );
    }

    /**
     * LiveKit 입장 토큰 발급 - 방장이거나 ACCEPTED 상태의 멤버만 가능
     */
    @Transactional(readOnly = true)
    public LiveKitTokenResponse issueToken(Long userId, Long roomId) {
        if (!canEnter(userId, roomId)) {
            throw new CustomException(ErrorCode.CHAT_ROOM_FORBIDDEN);
        }

        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHAT_ROOM_NOT_FOUND));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        return liveKitTokenService.createToken(
                room.getLivekitRoomName(),
                "user-" + userId,
                user.getNickname()
        );
    }

    private void validateOwner(ChatRoom room, Long userId) {
        if (!room.getOwner().getId().equals(userId)) {
            throw new CustomException(ErrorCode.CHAT_ROOM_FORBIDDEN);
        }
    }

    private ChatRoomMember getPendingMember(Long roomId, Long memberId) {
        ChatRoomMember member = chatRoomMemberRepository.findByIdAndRoomId(memberId, roomId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHAT_ROOM_MEMBER_NOT_FOUND));

        if (!member.isPending()) {
            throw new CustomException(ErrorCode.CHAT_ROOM_MEMBER_NOT_PENDING);
        }

        return member;
    }
}
