# =========================================================================
# PlanMyTrip Platform - Production Dockerfile for Render Web Service
# =========================================================================

# Stage 1: Build stage
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copy root pom and module poms for optimal layer caching
COPY pom.xml .
COPY services/gateway-service/pom.xml services/gateway-service/
COPY services/user-service/pom.xml services/user-service/
COPY services/trip-service/pom.xml services/trip-service/
COPY services/ai-itinerary-service/pom.xml services/ai-itinerary-service/

# Copy all service sources
COPY services/ services/

# Compile and package all microservice JARs in a single pass
RUN mvn clean package -DskipTests

# Stage 2: Runtime stage
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Install MariaDB and networking tools for standalone/fallback support
RUN apt-get update && \
    DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends \
    mariadb-server \
    curl \
    bash \
    ca-certificates && \
    rm -rf /var/lib/apt/lists/*

# Copy built JARs
COPY --from=build /app/services/gateway-service/target/gateway-service-0.0.1-SNAPSHOT.jar /app/gateway-service.jar
COPY --from=build /app/services/user-service/target/user-service-0.0.1-SNAPSHOT.jar /app/user-service.jar
COPY --from=build /app/services/trip-service/target/trip-service-0.0.1-SNAPSHOT.jar /app/trip-service.jar
COPY --from=build /app/services/ai-itinerary-service/target/ai-itinerary-service-0.0.1-SNAPSHOT.jar /app/ai-itinerary-service.jar

# Copy database initialization script and startup script
COPY docker/init-db.sql /app/init-db.sql
COPY start.sh /app/start.sh

RUN chmod +x /app/start.sh

# Render listens on $PORT (defaults to 10000 or 8080)
EXPOSE 8080 10000

ENV SPRING_PROFILES_ACTIVE=prod

ENTRYPOINT ["/app/start.sh"]
