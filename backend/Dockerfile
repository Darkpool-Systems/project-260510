# ─────────────────────────────────────────
# Stage 1 — builder: Gradle로 JAR 빌드
# ─────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Gradle Wrapper와 설정 파일만 먼저 복사
# → 소스 코드가 바뀌어도 의존성 목록이 같으면 이 레이어 캐시 재사용 (빌드 속도 향상)
COPY gradlew .
COPY gradle gradle
COPY build.gradle.kts settings.gradle.kts ./

RUN chmod +x gradlew

# 의존성 다운로드만 먼저 실행 (소스 없이)
# --no-daemon: CI/Docker 환경에서 Gradle 데몬을 띄우지 않아 메모리 절약
RUN ./gradlew dependencies --no-daemon

# 소스 복사 후 빌드
COPY src src
RUN ./gradlew bootJar -x test --no-daemon

# ─────────────────────────────────────────
# Stage 2 — runner: JRE만 있는 경량 실행 이미지
# ─────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# 보안: root 대신 전용 계정으로 실행
RUN addgroup --system --gid 1001 spring && \
    adduser --system --uid 1001 --ingroup spring spring

COPY --from=builder --chown=spring:spring /app/build/libs/*.jar app.jar

USER spring

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]