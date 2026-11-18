# ==========================================
# FareX Backend Server - Multi-Stage Dockerfile
# ==========================================

# Stage 1: Build JARs
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder
WORKDIR /build

# Copy project definitions for layer caching
COPY pom.xml .
COPY farex-shared/pom.xml farex-shared/
COPY farex-server/pom.xml farex-server/
COPY farex-app/pom.xml farex-app/

# Copy sources for shared library and server
COPY farex-shared/src farex-shared/src
COPY farex-server/src farex-server/src

# Build farex-shared and farex-server packages
RUN mvn -B package -DskipTests -pl farex-shared,farex-server -am

# Stage 2: Minimal Runtime Image
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Install wget for healthcheck (included in alpine busybox, ensure curl/busybox tools available)
# Create a dedicated non-root system user and prepare directories
RUN addgroup -S farex && adduser -S farex -G farex \
    && mkdir -p /app/data /app/libs /app/default-data \
    && chown -R farex:farex /app

# Copy built JARs from builder stage
COPY --from=builder /build/farex-shared/target/farex-shared-*.jar /app/libs/
COPY --from=builder /build/farex-server/target/farex-server-*.jar /app/farex-server.jar

# Copy default initial data files for auto-seeding if an empty volume is mounted
COPY --chown=farex:farex data/ /app/default-data/

# Default environment configuration
ENV PORT=8080 \
    DATA_DIR=/app/data \
    FAKE_PAY=1

EXPOSE 8080

USER farex

# Healthcheck checking the public fares endpoint
HEALTHCHECK --interval=15s --timeout=5s --start-period=5s --retries=3 \
  CMD wget -q -O - http://localhost:${PORT}/fares/current || exit 1

# If data folder has no users.csv (fresh or empty volume), seed initial CSV data
CMD ["sh", "-c", "if [ ! -f /app/data/users.csv ] && [ -d /app/default-data ]; then cp -rn /app/default-data/* /app/data/ 2>/dev/null || true; fi && exec java -cp 'farex-server.jar:libs/*' farex.server.Main"]
