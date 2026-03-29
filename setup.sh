#!/bin/bash
# ─────────────────────────────────────────────────────────────
#  Hermes — Notification Engine  |  Setup Script (Linux/macOS)
# ─────────────────────────────────────────────────────────────

set -e

echo ""
echo "⚡ Hermes — Notification Engine Setup"
echo "────────────────────────────────────────"

# Check Docker is installed
if ! command -v docker &> /dev/null; then
  echo "❌ Docker is not installed. Please install Docker Desktop and try again."
  echo "   https://www.docker.com/products/docker-desktop/"
  exit 1
fi

# Check Docker daemon is running
if ! docker info &> /dev/null; then
  echo "❌ Docker daemon is not running. Please start Docker Desktop and try again."
  exit 1
fi

echo "✅ Docker found: $(docker --version)"

# Check docker compose (v2 plugin or v1 standalone)
if docker compose version &> /dev/null; then
  COMPOSE="docker compose"
elif command -v docker-compose &> /dev/null; then
  COMPOSE="docker-compose"
else
  echo "❌ Docker Compose not found. Please install Docker Compose."
  exit 1
fi

echo "✅ Docker Compose found"
echo ""

# Stop any existing containers for this project
echo "🛑 Stopping any existing containers..."
$COMPOSE down --remove-orphans 2>/dev/null || true

# Build and start all services
echo ""
echo "🔨 Building and starting services (MySQL, Redis, Kafka, App)..."
echo "   This may take a few minutes on first run."
echo ""
$COMPOSE up --build -d

# Wait for app to be healthy
echo ""
echo "⏳ Waiting for the app to start..."
ATTEMPTS=0
MAX_ATTEMPTS=30

until curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/notify 2>/dev/null | grep -qE "^(4|2)"; do
  ATTEMPTS=$((ATTEMPTS + 1))
  if [ "$ATTEMPTS" -ge "$MAX_ATTEMPTS" ]; then
    echo ""
    echo "⚠️  App did not start within expected time. Check logs with:"
    echo "   $COMPOSE logs app"
    exit 1
  fi
  printf "."
  sleep 3
done

echo ""
echo ""
echo "────────────────────────────────────────"
echo "✅ Hermes is running!"
echo ""
echo "   API:    http://localhost:8080/notify"
echo ""
echo "   Try it:"
echo '   curl -X POST http://localhost:8080/notify \'
echo '     -H "Content-Type: application/json" \'
echo '     -d '"'"'{"tenantId":"tenant1","eventType":"ORDER_PLACED","referenceId":"order-001","payload":{"email":"user@example.com"}}'"'"
echo ""
echo "   Logs:   $COMPOSE logs -f app"
echo "   Stop:   $COMPOSE down"
echo "────────────────────────────────────────"
