package com.back.global.scheduling;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * {@code @Scheduled} 어노테이션이 동작하도록 스케줄링을 활성화
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
