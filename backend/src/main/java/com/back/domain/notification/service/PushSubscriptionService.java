package com.back.domain.notification.service;

import com.back.domain.auth.domain.User;
import com.back.domain.auth.repository.UserRepository;
import com.back.domain.notification.domain.PushSubscription;
import com.back.domain.notification.dto.PushSubscribeRequest;
import com.back.domain.notification.repository.PushSubscriptionRepository;
import com.back.global.exception.CustomException;
import com.back.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PushSubscriptionService {

    private final PushSubscriptionRepository pushSubscriptionRepository;
    private final UserRepository userRepository;

    /**
     * 구독 등록 (upsert)
     * 이미 저장된 endpoint면 소유 유저/키를 갱신 - 같은 브라우저에서 재구독하거나
     * 다른 계정으로 로그인한 뒤 다시 구독 요청이 오는 경우를 대비
     */
    @Transactional
    public void subscribe(Long userId, PushSubscribeRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        pushSubscriptionRepository.findByEndpoint(request.getEndpoint())
                .ifPresentOrElse(
                        existing -> existing.updateOwnerAndKeys(
                                user,
                                request.getKeys().getP256dh(),
                                request.getKeys().getAuth()
                        ),
                        () -> pushSubscriptionRepository.save(
                                PushSubscription.builder()
                                        .user(user)
                                        .endpoint(request.getEndpoint())
                                        .p256dhKey(request.getKeys().getP256dh())
                                        .authKey(request.getKeys().getAuth())
                                        .build()
                        )
                );
    }

    /**
     * 구독 해제 - 없는 endpoint여도 조용히 무시 (이미 정리된 경우 포함)
     */
    @Transactional
    public void unsubscribe(String endpoint) {
        pushSubscriptionRepository.deleteByEndpoint(endpoint);
    }
}
