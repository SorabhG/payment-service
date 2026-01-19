# Payment Service – Spring Boot Microservice

## Overview

This project is a production-style **Payment Processing Microservice** built with **Spring Boot**, designed to demonstrate clean architecture, domain-driven design, Kafka-based eventing, idempotency handling, soft deletes, validation, and comprehensive testing.

The service exposes REST APIs to create, retrieve, and cancel payments, persists them using JPA/Hibernate, and publishes payment events to Kafka for downstream processing (e.g., fraud checks).

This repository is intended to showcase **backend engineering best practices** suitable for technical interviews.

---

## Key Features

* **RESTful API** for payment lifecycle
* **Soft Delete** (logical cancel instead of physical delete)
* **Kafka Integration** for event-driven processing
* **Idempotency Support** using Idempotency-Key header
* **Fraud Detection Service** with deterministic business rule
* **Layered Architecture** (Controller → Service → Repository)
* **Validation Layer** for request DTOs
* **Unit Tests & Integration Tests** with JUnit 5
* **Docker & Docker Compose** for local environment

---

## Architecture

High-level flow:

1. Client sends payment request via REST API
2. Controller validates and forwards to Service layer
3. Service persists Payment and related details (Card/Bank)
4. Payment ID is published to Kafka topic
5. Kafka Consumer loads Payment and runs FraudService
6. Payment status is updated based on fraud result

```
Client
  |
  v
[RestController]
  |
  v
[PaymentService] -----> [JPA Repository] -----> [PostgreSQL]
  |
  v
[Kafka Producer] -----> Kafka Topic -----> [Kafka Consumer] -----> [FraudService]
```

---

## Technology Stack

* Java 17+
* Spring Boot 3.x
* Spring Data JPA (Hibernate)
* Apache Kafka
* PostgreSQL (via Docker)
* JUnit 5 & Mockito
* Testcontainers (for integration tests, if enabled)
* Docker & Docker Compose

---

## API Endpoints

### Create Payment

```
POST /api/payments
Headers:
  Idempotency-Key: <uuid>

Request Body:
{
  "type": "CARD",
  "amount": 1200.50,
  "currency": "AUD",
  "cardNumber": "4111111111111111",
  "expiry": "12/27"
}
```

### Get Payment by ID

```
GET /api/payments/{id}
```

### Cancel Payment (Soft Delete)

```
PATCH /api/payments/{id}/cancel
```

Returns the cancelled payment with updated status.

---

## Domain Model

Core entities:

* `Payment`

    * id (UUID)
    * amount (BigDecimal)
    * currency
    * status (CREATED, COMPLETED, CANCELLED, FRAUD)
    * type (CARD, BANK)
    * deleted (boolean – soft delete flag)

* `CardPaymentDetails`

* `BankPaymentDetails`

---

## Fraud Logic

Fraud detection is implemented as a deterministic business rule:

```java
// Payments greater than 15,000 are considered fraudulent
boolean fraud = payment.getAmount().compareTo(BigDecimal.valueOf(15000)) > 0;
```

This keeps behavior predictable and easy to test.

---

## Idempotency

* Client must provide `Idempotency-Key` header for create payment requests
* Duplicate requests with the same key return the **original response**
* Prevents double charging and duplicate persistence

---

## Testing Strategy

The project includes both **unit tests** and **integration tests**.

### Unit Tests

* `FraudServiceTest`
* `PaymentValidatorTest`
* `PaymentServiceTest`

Focus:

* Business rules
* Validation logic
* Service-layer behavior

### Integration Tests

* Controller tests with `@WebMvcTest` / `@SpringBootTest`
* JPA integration with H2/Testcontainers
* End-to-end flow from REST → DB → Kafka (where enabled)

All tests use **JUnit 5**.

---

## Running Locally

### Using Docker Compose

```bash
docker-compose up -d
```

This starts:

* PostgreSQL
* Kafka
* Zookeeper
* Payment Service

### Running Tests

```bash
./mvnw clean test
```

---

## Project Structure

```
src/main/java
 └── com.example.paymentservice
      ├── controller
      ├── service
      ├── repository
      ├── entity
      ├── dto
      ├── validator
      └── kafka

src/test/java
 └── com.example.paymentservice
      ├── service
      ├── controller
      └── integration
```

---

## Design Principles Demonstrated

* Separation of Concerns
* Single Responsibility Principle
* Idempotent APIs
* Soft Delete Pattern
* Event-Driven Architecture
* Testable Business Logic
* Clean Package Structure

---

## Reliability & Error Handling

The Kafka consumer is configured with a production-grade error handling strategy using **Spring Kafka DefaultErrorHandler**.

### Retry with Backoff

* Retries are enabled using `FixedBackOff`:

    * **Backoff interval:** 2000 ms (2 seconds)
    * **Max attempts:** 3 retries
* This allows the consumer to recover from **transient failures** (e.g., temporary DB or network issues) before giving up.

### Dead Letter Queue (DLQ)

* Failed messages are published to a dedicated **`payments-dlq`** topic using `DeadLetterPublishingRecoverer`.
* Poison messages can be:

    * Inspected
    * Replayed
    * Manually fixed and reprocessed

### Non-Retryable Exceptions

Permanent business failures are excluded from retry and sent **directly to DLQ**:

* `IllegalArgumentException`
* `PaymentNotFoundException`

This design ensures:

* No infinite retry loops
* Clear separation of transient vs permanent failures
* Operational observability and safe recovery

---

## Future Improvements

* Retry & circuit breaker using Resilience4j
* Schema registry for Kafka
* Distributed tracing (OpenTelemetry)
* API documentation with OpenAPI / Swagger
* Caching for read-heavy endpoints

---

## Author

Sorabh G

This project is intended for interview demonstration and learning purposes.
