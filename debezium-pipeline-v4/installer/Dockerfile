# ===============================================================
# Debezium AI v4.0.1 — Docker Image
# ===============================================================
# Multi-stage build for optimized production images.
# ===============================================================

# --- Build Stage ---
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

# Copy dependency descriptors
COPY pom.xml ./
COPY core/pom.xml ./core/
COPY ai/pom.xml ./ai/
COPY api/pom.xml ./api/
COPY monitoring/pom.xml ./monitoring/
COPY plugins/pom.xml ./plugins/

# Download dependencies (cached layer)
RUN mvn dependency:go-offline -B -q -DskipTests || true

# Copy source and build
COPY . .
RUN mvn clean package -DskipTests -Dquick -q

# --- Runtime Stage ---
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Create non-root user
RUN addgroup -S debezium && adduser -S debezium -G debezium

# Copy built artifacts
COPY --from=build /app/core/target/*.jar /app/core.jar
COPY --from=build /app/ai/target/*.jar /app/ai.jar
COPY --from=build /app/api/target/*.jar /app/api.jar
COPY --from=build /app/monitoring/target/*.jar /app/monitoring.jar
COPY --from=build /app/plugins/target/*.jar /app/plugins.jar

# Copy configuration
COPY application.properties /app/application.properties

# Permissions
RUN chown -R debezium:debezium /app

USER debezium

EXPOSE 8080

ENV JAVA_OPTS="-Xms256m -Xmx1g"
ENV QUARKUS_HTTP_PORT=8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --retries=3 \
    CMD wget -qO- http://localhost:8080/q/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/api.jar"]
