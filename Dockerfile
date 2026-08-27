# syntax=docker/dockerfile:1

# Maven Wrapper로 Java 17 실행 파일을 생성한다.
FROM eclipse-temurin:17-jdk-jammy AS builder
WORKDIR /workspace

# 의존성 계층을 분리해 소스 변경 시 다운로드 캐시를 재사용한다.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -ntp dependency:go-offline

COPY src/ src/
RUN ./mvnw -B -ntp -DskipTests package \
    && JAR_FILE="$(find target -maxdepth 1 -type f -name '*.jar' -print -quit)" \
    && test -n "$JAR_FILE" \
    && cp "$JAR_FILE" /workspace/app.jar

# 런타임에는 JRE와 애플리케이션 파일만 포함한다.
FROM eclipse-temurin:17-jre-jammy
ARG APP_UID=1000
RUN groupadd --gid 10001 meetback \
    && useradd --uid "${APP_UID}" --gid meetback --no-create-home --shell /usr/sbin/nologin meetback

WORKDIR /app
RUN mkdir -p /app/config \
    && chown meetback:meetback /app/config
COPY --from=builder --chown=meetback:meetback /workspace/app.jar /app/app.jar

# 호스트의 .env를 런타임에만 읽고 이미지에는 포함하지 않는다.
ENV SPRING_CONFIG_IMPORT=optional:file:/app/config/.env[.properties]

USER meetback:meetback
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
