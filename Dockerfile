# syntax=docker/dockerfile:1.7

FROM maven:3.9.9-eclipse-temurin-21 AS builder
WORKDIR /workspace

# Copy only the dependency manifest first to leverage Docker layer caching.
COPY pom.xml ./
RUN mvn dependency:go-offline

# Copy the application source and build the Spring Boot jar.
COPY src ./src
RUN mvn package -DskipTests

FROM eclipse-temurin:21-jre-jammy

# Create a dedicated non-root user for the runtime container.
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl ca-certificates \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system spring \
    && useradd --system --gid spring --create-home --home-dir /app spring

WORKDIR /app

# Copy the jar built in the previous stage.
COPY --from=builder /workspace/target/*.jar /app/app.jar
RUN chown -R spring:spring /app

USER spring

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=45s --retries=3 \
  CMD curl -fsS http://localhost:8080/actuator/health || curl -fsS http://localhost:8080/ || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
