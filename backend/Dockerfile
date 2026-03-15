FROM maven:3.9.9-eclipse-temurin-25 AS build
WORKDIR /app

COPY pom.xml ./
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B clean package -DskipTests

FROM eclipse-temurin:25-jre-jammy
WORKDIR /app

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system appuser \
    && useradd --system --gid appuser --create-home --home-dir /home/appuser appuser \
    && mkdir -p /app/storage/documents \
    && chown -R appuser:appuser /app /home/appuser

COPY --from=build --chown=appuser:appuser /app/target/quarkus-app/ ./

USER appuser

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -fsS http://localhost:8080/q/health || exit 1

ENV JAVA_OPTS="-XX:+UseG1GC -XX:MaxRAMPercentage=75.0 --enable-preview"

CMD ["java", "-jar", "quarkus-run.jar"]
