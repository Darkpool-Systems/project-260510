package com.back.global.async;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * {@code @Async} 어노테이션이 동작하도록 비동기 실행 활성화
 * 알림 발송(PushNotificationService 등)이 요청 스레드를 막지 않도록 하는 데 사용
 */
@Configuration
@EnableAsync
public class AsyncConfig {
}
