# syntax=docker/dockerfile:1

FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace

COPY service-botica/ ./service-botica/
COPY docs/cadena-farmacias-docs/database/ ./docs/cadena-farmacias-docs/database/

WORKDIR /workspace/service-botica
RUN chmod +x gradlew
RUN ./gradlew :bootstrap-app:bootJar --no-daemon

FROM eclipse-temurin:25-jre AS runtime
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*
RUN groupadd --system botica && useradd --system --gid botica --home /app botica
WORKDIR /app

COPY --from=build /workspace/service-botica/bootstrap-app/build/libs/service-botica.jar ./service-botica.jar

RUN chown -R botica:botica /app
USER botica

EXPOSE 8080

HEALTHCHECK --interval=15s --timeout=5s --start-period=60s --retries=5 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "service-botica.jar"]
