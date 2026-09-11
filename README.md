# OrderFlow — Event-Driven Order & Payment Processing Platform

> A production-oriented e-commerce backend built with Spring Boot microservices, Kafka, Redis, MySQL, JWT security, Saga-based distributed transactions, Transactional Outbox, Resilience4j, Prometheus, and Grafana.

OrderFlow is a backend platform that demonstrates how a real-world e-commerce system can reliably process orders, reserve inventory, process payments, and handle failures across multiple independent microservices.

The project focuses on:

- Microservices architecture
- Event-driven communication
- Distributed transactions
- Reliability and fault tolerance
- Authentication and authorization
- Concurrency handling
- Caching
- Observability

---

## 🏗️ Architecture

```text
                         ┌──────────────────┐
                         │   React / Vite    │
                         │    Frontend      │
                         └────────┬─────────┘
                                  │
                                  ▼
                         ┌──────────────────┐
                         │   API Gateway    │
                         │ Spring Gateway   │
                         │ JWT + RBAC       │
                         └────────┬─────────┘
                                  │
              ┌───────────────────┼───────────────────┐
              │                   │                   │
              ▼                   ▼                   ▼
       ┌─────────────┐     ┌─────────────┐     ┌─────────────┐
       │    Auth     │     │   Product   │     │    Order    │
       │   Service   │     │   Service   │     │   Service   │
       └──────┬──────┘     └──────┬──────┘     └──────┬──────┘
              │                   │                   │
              ▼                   ▼                   ▼
          ┌───────┐           ┌───────┐           ┌───────┐
          │ MySQL │           │ MySQL │           │ MySQL │
          └───────┘           └───┬───┘           └───────┘
                                  │
                                Redis

                         ┌──────────────────┐
                         │      Kafka       │
                         │    Event Bus      │
                         └────────┬─────────┘
                                  │
                  ┌───────────────┼────────────────┐
                  │               │                │
                  ▼               ▼                ▼
          ┌─────────────┐ ┌─────────────┐ ┌─────────────┐
          │  Inventory  │ │   Payment   │ │Notification │
          │   Service   │ │   Service   │ │   Service   │
          └──────┬──────┘ └──────┬──────┘ └──────┬──────┘
                 │               │               │
                 ▼               ▼               ▼
             ┌───────┐       ┌───────┐       ┌───────┐
             │ MySQL │       │ MySQL │       │ MySQL │
             └───────┘       └───────┘       └───────┘


                         ┌──────────────────┐
                         │ Eureka Discovery │
                         │     Server       │
                         └──────────────────┘

                         ┌──────────────────┐
                         │ Prometheus +     │
                         │     Grafana      │
                         └──────────────────┘


| Service              | Responsibility                         |
| -------------------- | -------------------------------------- |
| API Gateway          | Routing, authentication, authorization |
| Auth Service         | Registration, login, JWT and RBAC      |
| Product Service      | Product CRUD, search and Redis caching |
| Order Service        | Order creation and order lifecycle     |
| Inventory Service    | Stock reservation and release          |
| Payment Service      | Payment processing and idempotency     |
| Notification Service | Payment notifications                  |
| Eureka Server        | Service discovery                      |


Auth          → auth_db
Product       → product_db
Order         → order_db
Inventory     → inventory_db
Payment       → payment_db
Notification  → notification_db


1) Order Processing

When a customer creates an order:
Customer
   │
   ▼
Order Service
   │
   │ Save order as CREATED
   │
   ▼
Kafka
   │
   ▼
Inventory Service
   │
   │ Reserve stock
   │
   ▼
Kafka
   │
   ▼
Payment Service
   │
   │ Process payment
   │
   ▼
Kafka
   │
   ▼
Order Service
   │
   ▼
CONFIRMED


Successful Flow : 

CREATED
   ↓
INVENTORY_RESERVED
   ↓
PAYMENT_PENDING
   ↓
CONFIRMED

if payment fails (saga + compensation)

Order Created
     ↓
Inventory Reserved
     ↓
Payment Failed
     ↓
Release Inventory
     ↓
Order Cancelled

🔐 Authentication & Authorization
OrderFlow uses JWT-based authentication.

Login
  ↓
Auth Service
  ↓
Validate credentials
  ↓
Generate JWT
  ↓
Client

Supporting Roles : ADMIN CUSOMER

Example :

 GET /api/products
        ↓
Public

POST /api/products
        ↓
ADMIN only

POST /api/orders
        ↓
Authenticated user

Passwords are stored using BCrypt hashing.

🔁 Payment Idempotency
Payment processing must prevent duplicate payments when the same request is processed more than once.

OrderFlow uses an Idempotency-Key with a database uniqueness constraint.
Payment Event
     ↓
Check Idempotency Key
     ↓
Already processed?
   /       \
 YES       NO
  │         │
Return    Process
           │
           ▼
      Save Payment

📦 Inventory Concurrency
Inventory can be accessed by multiple customers simultaneously.

OrderFlow uses optimistic locking with a version field to detect conflicting updates
Inventory
├── id
├── product_id
├── available_quantity
├── reserved_quantity
├── version
└── updated_at

📤 Transactional Outbox

A common distributed-system problem is:
1. Update database
2. Publish Kafka event

If the database update succeeds but Kafka publishing fails, the system can become inconsistent.

OrderFlow uses the Transactional Outbox Pattern.
┌───────────────────────────┐
│       DB Transaction      │
│                           │
│  Update Business Data     │
│           +               │
│  Save Outbox Event        │
│                           │
└─────────────┬─────────────┘
              │
              ▼
       Outbox Publisher
              │
              ▼
            Kafka

This pattern is implemented for the Order, Inventory and Payment flows.

📨 Kafka Event-Driven Communication

Important Kafka events include:
order.created
inventory.reserved
inventory.failed
inventory.release.requested
inventory.released
payment.requested
payment.succeeded
payment.failed

example :
Order Service
     │
     │ order.created
     ▼
   Kafka
     │
     ▼
Inventory Service


♻️ Resilience4j

Synchronous communication between services is protected using Resilience4j.

OrderFlow uses:

Retry
Circuit Breaker
Timeout
Fallback

Example :
Order Service
      │
      ▼
Product Service
      │
      X
   Failure
      │
      ▼
    Retry
      │
      X
   Failure
      │
      ▼
Circuit Breaker
      │
      ▼
Fallback

⚡ Redis Caching

Product Service uses Redis to reduce repeated database queries.

GET Product
     │
     ▼
   Redis
     │
 ┌───┴────┐
 │        │
Hit      Miss
 │        │
 ▼        ▼
Return   MySQL
          │
          ▼
        Redis

🚨 Dead Letter Topics

Kafka messages that repeatedly fail processing can be moved to Dead Letter Topics.

Example:

payment.requested
        │
        ▼
Payment Consumer
        │
        X
     Failure
        │
        ▼
payment.requested.DLT

📊 Observability

OrderFlow uses Micrometer, Prometheus and Grafana for monitoring.

The monitoring dashboard provides visibility into:

Service availability
HTTP request rate
HTTP 5xx errors
JVM heap memory
CPU usage
Circuit breaker state
Database connections
Kafka activity

Spring Boot Services
        │
        ▼
     Micrometer
        │
        ▼
    Prometheus
        │
        ▼
      Grafana

🛠️ Technology Stack

Backend
Java 21
Spring Boot
Spring Web
Spring Data JPA
Spring Security
Spring Cloud Gateway
Spring Cloud Netflix Eureka
Spring Cloud OpenFeign
Spring Kafka
Resilience4j

Database & Cache
MySQL
Redis

Messaging
Apache Kafka


Security
JWT
BCrypt
Role-Based Access Control


Observability
Micrometer
Prometheus
Grafana


DevOps
Docker
Docker Compose
Maven
Git / GitHub

📁 Project Structure
orderflow/
│
├── api-gateway/
├── auth-service/
├── product-service/
├── order-service/
├── inventory-service/
├── payment-service/
├── notification-service/
│
├── eureka-service/
├── infrastructure/
├── frontend/
│
├── docs/
│   ├── architecture/
│   ├── database/
│   └── sequence-diagrams/
│
├── docker-compose.yml
├── .gitignore
└── README.md

🚀 Running Locally
Prerequisites
Java 21+
Maven
Docker Desktop
MySQL
Redis


Clone
git clone https://github.com/UpendraDudekonda/orderflow.git
cd orderflow

START infrastructure :
docker compose up -d

Start Eureka and the microservices from your IDE or Maven.

Eureka:

http://localhost:8761

API Gateway:

http://localhost:8080

🌐 Main APIs
Authentication
POST /api/auth/register
POST /api/auth/login
POST /api/auth/refresh
POST /api/auth/logout
GET  /api/auth/me

Products
GET    /api/products
GET    /api/products/{id}
GET    /api/products/search?keyword=phone

POST   /api/products
PUT    /api/products/{id}
DELETE /api/products/{id}

Orders
POST /api/orders
GET  /api/orders/{id}
GET  /api/orders/my-orders
POST /api/orders/{id}/cancel

Payments
GET /api/payments/{orderId}

🧪 Failure Testing

Payment failure can be simulated using:

FAIL_TEST

This demonstrates the Saga compensation flow:

Order Created
      ↓
Inventory Reserved
      ↓
Payment FAILED
      ↓
Inventory Released
      ↓
Order CANCELLED


🧠 Key Engineering Concepts

OrderFlow demonstrates practical backend engineering concepts:

Microservices Architecture
Event-Driven Architecture
Saga Pattern
Transactional Outbox
Kafka
Idempotency
Optimistic Locking
Redis Caching
Circuit Breaker
Retry and Timeout
JWT Authentication
Role-Based Authorization
Dead Letter Topics
Service Discovery
Application Metrics
Dockerized Infrastructure

🎯 End-to-End Example

A customer purchases a product:

1. Customer logs in
        ↓
2. JWT is issued
        ↓
3. Customer creates order
        ↓
4. Order saved as CREATED
        ↓
5. order.created published
        ↓
6. Inventory reserves stock
        ↓
7. inventory.reserved published
        ↓
8. Payment processes transaction
        ↓
9. payment.succeeded published
        ↓
10. Order becomes CONFIRMED

If payment fails:

Payment Failed
      ↓
Release Inventory
      ↓
Order Cancelled


⭐ Project Highlights

7 independent Spring Boot business services
Kafka-based event-driven communication
Saga-based distributed transaction handling
Transactional Outbox
Payment idempotency
Inventory optimistic locking
JWT authentication and RBAC
Redis caching
Resilience4j fault tolerance
Kafka Dead Letter Topics
Eureka service discovery
Prometheus + Grafana observability
Dockerized infrastructure
Database-per-service architecture

👨‍💻 Author
Upendra D

Java Backend / Full Stack Developer

GitHub:
https://github.com/UpendraDudekonda

Portfolio:
https://portfolio-frontend.2124upendra.workers.dev/

LinkedIn:
https://www.linkedin.com/in/upendra-dudekonda-360339286/