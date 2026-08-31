# syntax=docker/dockerfile:1

FROM eclipse-temurin:17-jdk-jammy AS build

WORKDIR /workspace

# Resolve dependencies in a separate layer so source-only changes can reuse it.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

RUN chmod +x mvnw \
    && ./mvnw -B -ntp -DskipTests dependency:go-offline

COPY src/ src/

RUN ./mvnw -B -ntp -DskipTests package


FROM eclipse-temurin:17-jre-jammy AS runtime

ARG APP_UID=10001
ARG APP_GID=10001

RUN groupadd --system --gid "${APP_GID}" meetback \
    && useradd --system \
        --uid "${APP_UID}" \
        --gid meetback \
        --home-dir /app \
        --shell /usr/sbin/nologin \
        meetback \
    && mkdir -p /app/uploads/feed \
    && chown -R meetback:meetback /app

WORKDIR /app

COPY --from=build --chown=meetback:meetback \
    /workspace/target/*.jar \
    /app/app.jar

ENV FEED_IMAGE_UPLOAD_DIR=/app/uploads/feed \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"

USER meetback:meetback

EXPOSE 8080

STOPSIGNAL SIGTERM

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
