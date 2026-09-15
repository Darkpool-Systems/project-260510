package com.back.domain.notification.service;

import com.back.domain.notification.domain.PushSubscription;
import com.back.domain.notification.repository.PushSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import org.apache.http.HttpResponse;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 실제 Web Push 발송 담당
 * 호출부(댓글/대댓글 알림 리스너 등)는 "누구에게 어떤 내용을" 만 넘기면 됨
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PushNotificationService {

    private final PushService pushService;
    private final PushSubscriptionRepository pushSubscriptionRepository;

    /**
     * 특정 유저가 등록한 모든 디바이스(구독)에 푸시 발송
     * 구독이 없으면 조용히 무시 (알림 기능을 아직 켠 적 없는 유저)
     */
    public void sendToUser(Long userId, String title, String body, String url) {
        List<PushSubscription> subscriptions = pushSubscriptionRepository.findAllByUserId(userId);
        if (subscriptions.isEmpty()) {
            return;
        }

        byte[] payload = buildPayload(title, body, url);
        subscriptions.forEach(subscription -> send(subscription, payload));
    }

    private void send(PushSubscription subscription, byte[] payload) {
        try {
            Notification notification = new Notification(
                    subscription.getEndpoint(),
                    subscription.getP256dhKey(),
                    subscription.getAuthKey(),
                    payload
            );

            HttpResponse response = pushService.send(notification);
            int statusCode = response.getStatusLine().getStatusCode();

            // 410 Gone / 404 Not Found = 브라우저에서 구독이 만료/취소된 경우. 더 이상 재발송 시도하지 않도록 정리
            if (statusCode == 404 || statusCode == 410) {
                log.info("만료된 푸시 구독 삭제 - subscriptionId={}", subscription.getId());
                pushSubscriptionRepository.delete(subscription);
            } else if (statusCode >= 400) {
                log.warn("푸시 발송 실패 - subscriptionId={}, status={}", subscription.getId(), statusCode);
            }
        } catch (Exception e) {
            // 발송 실패가 댓글 작성 자체에 영향을 주면 안 되므로 예외를 삼키고 로그만 남김
            log.warn("푸시 발송 중 예외 발생 - subscriptionId={}, message={}", subscription.getId(), e.getMessage());
        }
    }

    /**
     * 프론트 Service Worker가 그대로 읽을 JSON payload
     * { "title": "...", "body": "...", "url": "..." }
     * Jackson 등 별도 라이브러리 없이 필드 3개짜리 고정 스키마라 직접 조립
     */
    private byte[] buildPayload(String title, String body, String url) {
        String json = "{"
                + "\"title\":\"" + escapeJson(title) + "\","
                + "\"body\":\"" + escapeJson(body) + "\","
                + "\"url\":\"" + escapeJson(url) + "\""
                + "}";
        return json.getBytes(StandardCharsets.UTF_8);
    }

    private String escapeJson(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
