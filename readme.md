# ⚡ Hermes — Event-Driven Notification Engine

> A production-ready notification microservice built with **Spring Boot 4**, **Apache Kafka**, and **Redis**.
> Designed for reliability, multi-tenancy, and real-world scale.

---

## 🚀 Key Features

- **Event-Driven Architecture** — Decoupled producers and consumers via Apache Kafka
- **Idempotency** — Deduplication layer using MySQL to guarantee exactly-once delivery
- **Multi-Tenant Rate Limiting** — Per-tenant sliding window limits powered by Redis
- **Retry + Dead Letter Queue** — Automatic retries with DLQ persistence for failed events
- **Multi-Channel Dispatch** — Email, Webhook, and In-App notification support
- **Request Validation** — Bean validation on all incoming events
- **Fully Dockerized** — One-command local setup with Docker Compose

---

## 🏗️ Architecture

```
Client
  │
  ▼
POST /notify
  │
  ├── Rate Limit Check (Redis)
  │
  ▼
Kafka Producer ──► Kafka Topic: notification-events
                          │
                          ▼
                   Kafka Consumer
                          │
                   Idempotency Check (MySQL)
                          │
                   NotificationProcessor
                     │           │
               [SUCCESS]      [FAILURE × 3]
                  │                 │
           ProcessedEvent       DLQ (MySQL)
           (status=SUCCESS)  (status=FAILED)
                  │
           NotificationSender
            ├── Email
            ├── Webhook
            └── In-App
```

---

## 🛠️ Tech Stack

| Layer        | Technology                     |
|--------------|--------------------------------|
| Language     | Java 25                        |
| Framework    | Spring Boot 4                  |
| Messaging    | Apache Kafka                   |
| Cache / RL   | Redis (Lettuce)                |
| Database     | MySQL 8                        |
| ORM          | Spring Data JPA / Hibernate    |
| Testing      | JUnit 5, Mockito               |
| Build        | Maven                          |
| Container    | Docker + Docker Compose        |

---

## ⚡ Quick Start (Docker — Recommended)

> **Prerequisites:** [Docker Desktop](https://www.docker.com/products/docker-desktop/) installed and running.

### Option A — Setup Script (One Command)

**Linux / macOS:**
```bash
git clone https://github.com/coder44445/Hermes.git
cd Hermes
chmod +x setup.sh && ./setup.sh
```

**Windows:**
```bat
git clone https://github.com/coder44445/Hermes.git
cd Hermes
setup.bat
```

### Option B — Manual Docker

```bash
git clone https://github.com/coder44445/Hermes.git
cd Hermes
docker-compose up --build
```

The service will be available at `http://localhost:8080`.

---

## 🖥️ Local Development (Without Docker)

> **Prerequisites:** Java 21+, Maven 3.9+, MySQL 8, Redis, Kafka running locally.

```bash
# 1. Clone the repo
git clone https://github.com/coder44445/Hermes.git
cd Hermes

# 2. Create the database
mysql -u root -p -e "CREATE DATABASE notification_db;"

# 3. Run the app
./mvnw spring-boot:run
```

---

## 📡 API Reference

### POST `/notify`

Accepts a notification event and queues it for processing.

**Request Body:**
```json
{
  "tenantId":    "tenant1",
  "eventType":   "PAYMENT_SUCCESS",
  "referenceId": "txn_abc_123",
  "payload": {
    "email":      "user@example.com",
    "webhookUrl": "https://your-service.com/webhook",
    "amount":     500,
    "currency":   "INR"
  }
}
```

**Responses:**

| Status | Meaning                                      |
|--------|----------------------------------------------|
| `202`  | Event accepted and queued in Kafka           |
| `400`  | Validation failed (missing required fields)  |
| `429`  | Rate limit exceeded for this tenant          |

**Example (curl):**
```bash
curl -X POST http://localhost:8080/notify \
  -H "Content-Type: application/json" \
  -d '{
    "tenantId":    "tenant1",
    "eventType":   "ORDER_PLACED",
    "referenceId": "order-001",
    "payload": {
      "email":   "customer@example.com",
      "amount":  1200,
      "currency":"INR"
    }
  }'
```

---

## ⚙️ Configuration

All config lives in `src/main/resources/application.yaml`.  
Override any value via environment variables (Docker handles this automatically).

| Property                        | Default              | Description                     |
|---------------------------------|----------------------|---------------------------------|
| `spring.datasource.url`         | `localhost:3306/...` | MySQL connection URL             |
| `spring.data.redis.host`        | `localhost`          | Redis host                       |
| `spring.data.redis.port`        | `6379`               | Redis port                       |
| `spring.kafka.bootstrap-servers`| `localhost:9092`     | Kafka broker address             |
| `notification.kafka.topic`      | `notification-events`| Kafka topic name                 |
| `ratelimit.per-minute`          | `10`                 | Max requests per tenant/minute   |

---

## 🧪 Running Tests

```bash
./mvnw test
```

Test coverage includes:
- `NotificationProcessorTest` — idempotency, retry, DLQ logic
- `RateLimiterServiceTest` — Redis sliding window
- `NotificationControllerIT` — full HTTP integration test

---

## 📁 Project Structure

```
src/
├── main/java/com/notification/
│   ├── config/         # Kafka, Redis, RestTemplate config
│   ├── controller/     # REST endpoint
│   ├── event/          # NotificationEvent, DeadLetterEvent models
│   ├── processed/      # ProcessedEvent (idempotency) model
│   ├── ratelimit/      # RateLimiterService
│   ├── repository/     # JPA repositories
│   └── service/        # Producer, Consumer, Processor, Sender
└── test/
    └── java/com/notification/
        ├── controller/ # Integration tests
        ├── ratelimit/  # Unit tests
        └── service/    # Unit tests
```

---

## 🔭 Roadmap

- [ ] JavaMailSender integration for real email delivery
- [ ] DLQ reprocessing endpoint (`POST /dlq/retry`)
- [ ] Prometheus + Grafana metrics
- [ ] Notification template management per tenant/event type
- [ ] Spring Security / API key auth per tenant
