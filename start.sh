#!/bin/bash
set -e

echo "========================================================="
echo "  🚀 Starting PlanMyTrip Unified Microservices Platform   "
echo "========================================================="

# 1. Database Setup: Check if external MYSQL_HOST is provided
if [ -n "$MYSQL_HOST" ] && [ "$MYSQL_HOST" != "localhost" ] && [ "$MYSQL_HOST" != "127.0.0.1" ]; then
    echo ">> Using external cloud MySQL database at ${MYSQL_HOST}:${MYSQL_PORT:-3306}"
else
    echo ">> No external DB provided. Bootstrapping embedded MariaDB..."
    if [ ! -d "/var/lib/mysql/mysql" ]; then
        mysql_install_db --user=mysql --datadir=/var/lib/mysql > /dev/null 2>&1
    fi
    mysqld_safe --skip-syslog --performance-schema=OFF --innodb-buffer-pool-size=16M --max-connections=20 &
    for i in {1..30}; do
        if mysqladmin ping -h localhost --silent; then
            break
        fi
        sleep 1
    done
    if [ -f "/app/init-db.sql" ]; then
        mysql -u root < /app/init-db.sql || true
    fi
    export MYSQL_HOST="127.0.0.1"
    export MYSQL_PORT="3306"
    export MYSQL_USER="root"
    export MYSQL_PASSWORD=""
fi

# 2. Optimized JVM flags for Render Free Tier (512MB RAM constraint)
# -XX:TieredStopAtLevel=1 disables C2 compiler, cutting startup memory and CPU by >50%
JVM_OPTS="-XX:+UseSerialGC -XX:TieredStopAtLevel=1 -Xss256k -Xms16m -Xmx72m -XX:MaxMetaspaceSize=85m"
GATEWAY_OPTS="-XX:+UseSerialGC -XX:TieredStopAtLevel=1 -Xss256k -Xms16m -Xmx64m -XX:MaxMetaspaceSize=85m"

export USER_SERVICE_URL="http://127.0.0.1:8081"
export TRIP_SERVICE_URL="http://127.0.0.1:8082"
export AI_ITINERARY_SERVICE_URL="http://127.0.0.1:8083"

PUBLIC_PORT=${PORT:-10000}

# 3. Start Gateway FIRST on public port so Render detects it immediately
echo ">> Starting API Gateway on public port ${PUBLIC_PORT}..."
java $GATEWAY_OPTS -Dserver.port=${PUBLIC_PORT} -jar /app/gateway-service.jar &
GATEWAY_PID=$!

# Brief pause to allow Gateway to initialize
sleep 4

# 4. Start downstream microservices sequentially
echo ">> Starting User Service on port 8081..."
java $JVM_OPTS -Dserver.port=8081 -jar /app/user-service.jar &
sleep 3

echo ">> Starting Trip Service on port 8082..."
java $JVM_OPTS -Dserver.port=8082 -jar /app/trip-service.jar &
sleep 3

echo ">> Starting AI Itinerary Service on port 8083..."
java $JVM_OPTS -Dserver.port=8083 -jar /app/ai-itinerary-service.jar &

echo "========================================================="
echo "  ✅ PlanMyTrip Backend Platform initialized successfully"
echo "========================================================="

# Keep container alive by waiting for Gateway process
wait $GATEWAY_PID


