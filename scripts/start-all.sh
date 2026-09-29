#!/usr/bin/env bash
# Starts all four services locally (requires MongoDB already running on 27017).
# Each service is started in the background; logs go to /tmp/ridelink-<service>.log
set -e

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

echo "Installing parent POM..."
mvn -q -N install

SERVICES=(driver-vehicle-service fare-payment-service account-service ride-management-service)

for svc in "${SERVICES[@]}"; do
  echo "Starting $svc (log: /tmp/ridelink-$svc.log)..."
  (cd "$svc" && nohup mvn -q spring-boot:run > "/tmp/ridelink-$svc.log" 2>&1 &)
  sleep 3
done

echo ""
echo "All services starting. Swagger UIs:"
echo "  Account:          http://localhost:8081/swagger-ui.html"
echo "  Driver & Vehicle: http://localhost:8082/swagger-ui.html"
echo "  Ride Management:  http://localhost:8083/swagger-ui.html"
echo "  Fare & Payment:   http://localhost:8084/swagger-ui.html"
