# ── Stage 1: Build ──────────────────────────────────────────────
FROM --platform=linux/amd64 eclipse-temurin:11-jdk-alpine AS builder
WORKDIR /app

# 의존성 레이어 캐싱: 소스 변경 시에도 의존성 재다운로드 방지
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon

# 소스 복사 및 빌드 (테스트 스킵 - CI에서 별도 실행)
COPY src src
RUN ./gradlew bootJar --no-daemon -x test

# ── Stage 2: Run ────────────────────────────────────────────────
FROM --platform=linux/amd64 eclipse-temurin:11-jre-alpine
WORKDIR /app

# 비root 유저로 실행 (보안)
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", \
  "-Dspring.profiles.active=prod", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-jar", "app.jar"]
