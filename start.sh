#!/bin/bash
set -e

echo "========================================================="
echo "  🚀 Starting PlanMyTrip Unified Microservices Platform   "
echo "========================================================="

# 1. Database Setup: Check if external MYSQL_HOST is provided
if [ -z "$MYSQL_HOST" ] || [ "$MYSQL_HOST" = "localhost" ] || [ "$MYSQL_HOST" = "127.0.0.1" ]; then
    echo ">> No external MYSQL_HOST provided. Bootstrapping embedded MariaDB on localhost:3306..."
    
    # Initialize MariaDB data directory if not already created
    if [ ! -d "/var/lib/mysql/mysql" ]; then
        mysql_install_db --user=mysql --datadir=/var/lib/mysql > /dev/null 2>&1
    fi
    
    # Start MariaDB daemon in background with low memory settings
    mysqld_safe --skip-syslog --performance-schema=OFF --innodb-buffer-pool-size=16M --max-connections=30 &
    
    # Wait for MariaDB to be responsive
    echo ">> Waiting for MariaDB to start..."
    for i in {1..30}; do
        if mysqladmin ping -h localhost --silent; then
            break
        fi
        sleep 1
    done
    
    # Execute database and seed initialization
    if [ -f "/app/init-db.sql" ]; then
        echo ">> Initializing databases (user_db, trip_db, itinerary_db) from init-db.sql..."
        mysql -u root < /app/init-db.sql || true
    fi
    
    export MYSQL_HOST="127.0.0.1"
    export MYSQL_PORT="3306"
    export MYSQL_USER="root"
    export MYSQL_PASSWORD=""
else
    echo ">> Using external cloud MySQL database at ${MYSQL_HOST}:${MYSQL_PORT:-3306}"
fi

# 2. JVM Options for Render Free Tier (512MB RAM constraint)
JAVA_OPTS="-XX:+UseSerialGC -Xss256k -Xms48m -Xmx80m -XX:+ExitOnOutOfMemoryError"

# Internal microservice ports
export USER_SERVICE_URL="http://127.0.0.1:8081"
export TRIP_SERVICE_URL="http://127.0.0.1:8082"
export AI_ITINERARY_SERVICE_URL="http://127.0.0.1:8083"

echo ">> Starting User Service on port 8081..."
PORT=8081 java $JAVA_OPTS -jar /app/user-service.jar > /tmp/user-service.log 2>&1 &
USER_PID=$!

echo ">> Starting Trip Service on port 8082..."
PORT=8082 java $JAVA_OPTS -jar /app/trip-service.jar > /tmp/trip-service.log 2>&1 &
TRIP_PID=$!

echo ">> Starting AI Itinerary Service on port 8083..."
PORT=8083 java $JAVA_OPTS -jar /app/ai-itinerary-service.jar > /tmp/ai-service.log 2>&1 &
AI_PID=$!

# Wait for microservices to bind
echo ">> Waiting for core microservices to initialize..."
sleep 8

# 3. Start Gateway Service (Render listens on $PORT, typically 10000 or 8080)
PUBLIC_PORT=${PORT:-8080}
echo ">> Starting API Gateway on public port ${PUBLIC_PORT}..."
echo "========================================================="
echo "  ✅ PlanMyTrip Backend Gateway is READY to accept traffic"
echo "========================================================="

# Gateway runs in foreground to keep container alive and pipe logs to Render
exec java -XX:+UseSerialGC -Xss256k -Xms64m -Xmx100m -Dserver.port=${PUBLIC_PORT} -jar /app/gateway-service.jar
