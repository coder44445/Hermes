#!/bin/bash
set -e

echo "⚡ Hermes — Setup"

# Make sure Docker is installed and the daemon is running before we do anything
if ! command -v docker &> /dev/null; then
  echo "❌ Docker not found. Install: https://www.docker.com/products/docker-desktop/"
  exit 1
fi

if ! docker info &> /dev/null; then
  echo "❌ Docker daemon not running. Start Docker Desktop."
  exit 1
fi

# Tear down old containers first to avoid port conflicts
docker compose down --remove-orphans 2>/dev/null || true

# Build images and start all services in the background
docker compose up --build -d

# Poll until the app responds — Kafka + MySQL can take a moment to be ready
echo "⏳ Waiting for app..."
ATTEMPTS=0
until curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/notify 2>/dev/null | grep -qE "^[24]"; do
  ATTEMPTS=$((ATTEMPTS + 1))
  if [ "$ATTEMPTS" -ge 30 ]; then
    echo "⚠️  Timed out. Run: docker compose logs app"
    exit 1
  fi
  printf "."
  sleep 3
done

echo ""
echo "✅ Running at http://localhost:8080/notify"
echo "Stop: docker compose down"
