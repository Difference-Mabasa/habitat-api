# Multi-stage build — small runtime image, no Maven inside the final layer.

# ── Stage 1: build ──────────────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src src
RUN mvn -B -q package -DskipTests

# ── Stage 2: runtime ────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S habitat && adduser -S habitat -G habitat
WORKDIR /app
COPY --from=build /workspace/target/habitat-api-*.jar app.jar
USER habitat
EXPOSE 8080 8081
ENV SPRING_PROFILES_ACTIVE=prod
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
