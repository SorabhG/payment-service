# Payments Service

## Overview
A Spring Boot microservice to handle card and bank payments with async fraud detection using Kafka, Redis caching, and JWT OAuth2 security.


## Features
- Create payments (card / bank)
- CRUD endpoints (GET / PUT / DELETE)
- JWT-based OAuth2 security
- Async fraud processing via Kafka
- Redis cache for validated bank info / BIN
- Ready for AWS Lambda & API Gateway deployment

## Tech Stack
- Java 17 / Spring Boot 3
- PostgreSQL
- Kafka
- Redis
- Spring Security OAuth2 (JWT)
- Maven

## Setup

1. **Database**
```bash
createdb paymentsdb
