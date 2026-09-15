package com.back.domain.notification.domain;

import com.back.domain.auth.domain.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 브라우저 Web Push 구독 정보 - push_subscriptions 테이블과 매핑
 * 한 유저가 여러 디바이스(브라우저)에서 구독할 수 있어 endpoint 단위로 저장
 */
@Entity
@Table(name = "push_subscriptions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PushSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 브라우저가 발급하는 구독 식별 URL. 디바이스(브라우저)당 유일
    @Column(nullable = false, unique = true, length = 500)
    private String endpoint;

    @Column(name = "p256dh_key", nullable = false, length = 255)
    private String p256dhKey;

    @Column(name = "auth_key", nullable = false, length = 255)
    private String authKey;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public PushSubscription(User user, String endpoint, String p256dhKey, String authKey) {
        this.user = user;
        this.endpoint = endpoint;
        this.p256dhKey = p256dhKey;
        this.authKey = authKey;
        this.createdAt = LocalDateTime.now();
    }

    /**
     * 같은 endpoint로 재구독 요청이 온 경우(키 회전, 다른 유저 로그인 등) 갱신
     */
    public void updateOwnerAndKeys(User user, String p256dhKey, String authKey) {
        this.user = user;
        this.p256dhKey = p256dhKey;
        this.authKey = authKey;
    }
}
