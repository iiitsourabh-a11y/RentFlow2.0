# Multi-stage Dockerfile for RentFlow Backend
# Stage 1: Build JAR with Maven Wrapper
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /workspace

# Copy Maven wrapper and POM first for dependency caching
COPY mvnw mvnw.cmd pom.xml ./
COPY .mvn .mvn
RUN chmod +x ./mvnw && ./mvnw dependency:go-offline -B || true

# Copy source code and build production artifact (skipping tests for container build)
COPY src src
RUN ./mvnw clean package -DskipTests -B

# Stage 2: Minimal, secure JRE runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Run as non-root user for container security
RUN addgroup -S rentflow && adduser -S rentflow -G rentflow
USER rentflow:rentflow

# Copy the packaged JAR from builder stage
COPY --from=builder /workspace/target/*.jar app.jar

ENV PORT=8080
EXPOSE 8080

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
