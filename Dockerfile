# syntax=docker/dockerfile:1.7

# ---- Build stage ----
FROM gradle:8.8-jdk17 AS builder
WORKDIR /workspace

COPY settings.gradle build.gradle ./
COPY src ./src

RUN gradle --no-daemon clean bootJar -x test

# ---- Runtime stage ----
FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=builder /workspace/build/libs/*.jar app.jar

EXPOSE 8080

ENV JAVA_OPTS=""
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
