# Payment Service – Design Document

## 1. Purpose

This document describes the **current system design** and the **planned future design** of the Payment Service. It is intended to explain architectural decisions, trade-offs, and how the system addresses reliability, scalability, and correctness concerns.

---

## 2. Problem Statement

Build a backend microservice that:

* Accepts payment requests via REST
* Persists payment data reliably
* Publishes payment events for asynchronous processing
* Detects fraudulent payments
* Handles failures safely without data loss or duplicate processing

The system must support:

* Idempotent APIs
* Event-driven processing
* Soft deletes instead of hard deletes
* Production-grade error handling for Kafka consumers

---

## 3. Functional Requirements

* Create a payment (CARD / BANK)
* Retrieve a payment by ID
* Cancel a payment (soft delete)
* Publish payment events to Kafka
* Consume payment events and perform fraud checks
* Update payment status based on fraud result

---

## 4. Non-Functional Requirements

* Reliability: no message loss, safe retry handling
* Consistency: no duplicate payment creation
* Observability: clear logs for operational debugging
* Testability: business logic must be unit-testable
* Extensibility: easy to add resilience and monitoring later

---

## 5. High-Level Architecture (Current)

```
Client
  |
  v
[REST Controller]
  |
  v
[PaymentService]
  |
  +--> [JPA Repository] --> [PostgreSQL]
  |
  +--> [Kafka Producer] --> payments topic
                               |
                               v
                        [Kafka Consumer]
                               |
                               v
                         [FraudService]
                               |
                               v
                     [Update Payment Status]
```

---

## 6. Current Design Details

### 6.1 Layered Architecture

* **Controller Layer**

    * Handles HTTP requests
    * Performs request validation

* **Service Layer**

    * Contains business logic
    * Manages transactions

* **Repository Layer**

    * JPA/Hibernate for persistence

This separation ensures:

* Testability
* Clear ownership of responsibilities
* Low coupling between layers

---

### 6.2 Idempotency Design

* Client provides `Idempotency-Key` header
* Key is stored with the Payment
* Duplicate requests return the original response

Prevents:

* Double charging
* Duplicate rows in database

---

### 6.3 Soft Delete Design

* Payments are cancelled using a **logical delete**:

    * `deleted = true`
    * `status = CANCELLED`

Benefits:

* Auditability
* Ability to trace historical payments
* No accidental data loss

---

### 6.4 Fraud Detection Design (Current)

Fraud logic is deterministic and synchronous in the consumer:

```java
// Payments greater than 15,000 are fraudulent
amount > 15000
```

Reasons:

* Predictable behavior
* Easy to unit test
* No external dependencies

---

### 6.5 Kafka Error Handling & Reliability (Current)

The Kafka consumer uses **Spring Kafka DefaultErrorHandler** with:

#### Retry with Backoff

* `FixedBackOff(2000ms, 3 attempts)`
* Transient failures are retried before failing

#### Dead Letter Queue (DLQ)

* Failed messages are published to `payments-dlq`
* Implemented using `DeadLetterPublishingRecoverer`

#### Non-Retryable Exceptions

Permanent failures are excluded from retry:

* `IllegalArgumentException`
* `PaymentNotFoundException`

These are sent **directly to DLQ**.

This ensures:

* No infinite retry loops
* Poison messages do not block the consumer
* Operators can inspect and replay failed events

---

### 6.6 Testing Strategy (Current)

* Unit Tests

    * `FraudServiceTest`
    * `PaymentValidatorTest`
    * `PaymentServiceTest`

* Integration Tests

    * Controller tests with Spring Boot test context
    * JPA tests with in-memory DB

Focus:

* Business rule correctness
* API behavior
* Persistence correctness

---

## 7. Key Design Decisions & Trade-offs

### 7.1 Synchronous REST + Asynchronous Kafka

Decision:

* Use REST for command handling
* Use Kafka for background processing

Trade-off:

* Adds complexity
* Enables scalability and decoupling

---

### 7.2 Deterministic Fraud Logic

Decision:

* Use static rule instead of random or external call

Trade-off:

* Not realistic
* Excellent for testing, demos, and interviews

---

### 7.3 Fixed Backoff Strategy

Decision:

* Use fixed backoff instead of exponential

Trade-off:

* Simpler
* Can be upgraded later to exponential backoff

---

## 8. Future Design & Planned Improvements

### 8.1 Resilience Enhancements

* Add **Resilience4j** for:

    * Circuit breakers
    * Bulkheads
    * Time limiters

* Apply to:

    * External service calls (future fraud service)
    * Database access under load

---

### 8.2 Advanced Retry Strategy

* Replace `FixedBackOff` with:

    * Exponential backoff
    * Jitter to avoid retry storms

* Classify exceptions into:

    * Retryable (timeouts, network)
    * Non-retryable (business validation)

---

### 8.3 Schema & Contract Management

* Introduce:

    * Kafka Schema Registry
    * Versioned event schemas

Prevents:

* Breaking consumer changes
* Runtime deserialization failures

---

### 8.4 Observability & Monitoring

* Add:

    * Micrometer metrics
    * Prometheus & Grafana dashboards
    * Distributed tracing (OpenTelemetry)

Track:

* Payment latency
* Kafka lag
* Retry and DLQ rates

---

### 8.5 Scalability Improvements

* Increase Kafka partitions
* Add consumer concurrency
* Introduce caching for read-heavy endpoints

---

## 9. Summary

The current design demonstrates:

* Clean layered architecture
* Idempotent REST APIs
* Event-driven processing
* Production-grade Kafka error handling
* Testable and extensible business logic

The future design focuses on:

* Higher resilience
* Better observability
* Safer schema evolution
* Horizontal scalability

This design is intentionally kept simple today, while being structured to evolve into a production-grade system.
