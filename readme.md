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
* **OpenAPI / Swagger Documentation** for APIs
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
[PaymentService] -----> [JPA Repository] -----> [PostgreSQL/H2]
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
* PostgreSQL / H2 (via Docker)
* JUnit 5 & Mockito
* Testcontainers (for integration tests, if enabled)
* Springdoc OpenAPI 3.x for API documentation
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

## OpenAPI / Swagger Documentation

Springdoc OpenAPI automatically generates interactive API docs for the service. Once the application is running:

* Swagger UI: [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)
* OpenAPI JSON: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

Swagger UI allows you to:

* Explore all endpoints interactively
* See request/response models
* Test APIs with headers such as `Idempotency-Key`
* Add Bearer JWT token if configured in OpenAPI security scheme

No annotations are strictly required on controllers for basic generation; Springdoc infers endpoints and DTOs automatically. Security headers (like OAuth2 JWT) need explicit configuration to appear in Swagger.

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
* End-to-end flow from REST → DB → Kafka

All tests use **JUnit 5**.

---

## Running Locally

### Using Maven

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

### Using Docker Compose

```bash
docker build --no-cache -t oauth_payment-service .

docker build -t auth-server:latest .

docker build -t oauth_auth-server:latest .
docker-compose up -d
```

This starts:

* PostgreSQL or H2 (depending on profile)
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

## Reliability & Error Handling

Kafka consumer uses **Spring Kafka DefaultErrorHandler**.

### Retry with Backoff

* 3 retries with 2s delay (FixedBackOff)
* Handles transient errors before failing

### Dead Letter Queue (DLQ)

* Messages failing after retries go to `payments-dlq`
* Supports inspection, replay, or manual fix

### Non-Retryable Exceptions

* `IllegalArgumentException`
* `PaymentNotFoundException`

Ensures safe, observable, production-ready processing.

---

## UI Integration Design (React + OAuth2)

This service is designed to integrate seamlessly with a modern Single Page Application (SPA) such as **React** secured via **OAuth2 / OpenID Connect**.

### High-Level Flow

1. User accesses the React application
2. React redirects the user to **Auth Server** for login (Authorization Code Flow with PKCE)
3. Auth Server authenticates the user and returns an **access token (JWT)**
4. React stores the token securely (memory or secure storage)
5. React calls Payment Service APIs with:

```
Authorization: Bearer <access_token>
```

6. Payment Service validates the JWT and enforces scopes:

| UI Action      | Required Scope |
| -------------- | -------------- |
| View payments  | payment.read   |
| Create payment | payment.write  |
| Cancel payment | payment.write  |

### Architecture Options

**Direct Integration** (Simple):

```
React UI  ---> Auth Server
React UI  ---> Payment Service
```

**With API Gateway / BFF** (Enterprise):

```
React UI ---> API Gateway / BFF ---> Payment Service
                  |
                  v
             Auth Server
```

The API Gateway can centralize:

* Token validation
* Rate limiting
* Routing
* Logging and monitoring

### Backend Readiness

The current Payment Service already supports:

* OAuth2 Resource Server
* JWT validation
* Scope-based authorization
* OpenAPI documentation for UI consumption

This allows UI integration with **minimal backend changes**, mainly:

* CORS configuration
* OpenAPI security scheme configuration
* Optional refresh token support

---

## Future Improvements

* Resilience4j for circuit breaker & retry
* Schema registry for Kafka
* Distributed tracing (OpenTelemetry)
* Enhanced OpenAPI security configuration
* Caching for read-heavy endpoints

---

## Author

Sorabh G

This project is intended for interview demonstration and learning purposes.
