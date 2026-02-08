# Notification Engine (Event-Driven)

A backend notification engine built with **Spring Boot 4** designed for **event-driven notification processing** with idempotency.

---

## 🚀 Features Completed

### ✔️ Core Features
- Spring Boot 4 application
- MySQL integration
- Idempotent event processing
- JSON-based event model
- Clean layered architecture (Controller → Service → Repository)

### ✔️ Idempotency Implementation
To prevent duplicate notifications, the system stores processed events in a database table:

- `processed_events`
- Unique `idempotencyKey` for each event
- `processedAt` timestamp
- `status` field

### ✔️ Event Model
The system accepts notification events as JSON:

```json
{
  "tenantId": "tenant1",
  "eventType": "PAYMENT_SUCCESS",
  "referenceId": "txn_123",
  "payload": {
    "amount": 500,
    "currency": "INR"
  }
}
