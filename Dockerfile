# Stage 1: Build
FROM gradle:8.12-jdk17 AS builder
WORKDIR /app
COPY . .
RUN gradle bootJar --no-daemon

# Stage 2: Run
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Graceful shutdown support
STOPSIGNAL SIGTERM

COPY --from=builder /app/build/libs/nte.jar app.jar

EXPOSE 7092

ENTRYPOINT ["java", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "app.jar"]
