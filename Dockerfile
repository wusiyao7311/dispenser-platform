# --- Build stage -----------------------------------------------------------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml .
# Cache dependencies separately from source so a source-only change doesn't
# re-download the whole repository.
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B -DskipTests package

# --- Runtime stage -----------------------------------------------------------
FROM eclipse-temurin:17-jre-noble
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd -r app && useradd -r -g app app
WORKDIR /app
COPY --from=build /build/target/dispenser-platform.jar app.jar
USER app

EXPOSE 8080
ENV SPRING_PROFILES_ACTIVE=prod

HEALTHCHECK --interval=30s --timeout=3s --start-period=30s --retries=3 \
    CMD curl -fsS http://localhost:8080/actuator/health | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
