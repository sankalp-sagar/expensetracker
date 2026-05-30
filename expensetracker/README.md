# ExpenseTracker — Distributed Expense Sharing & Settlement Platform

Production-grade microservices system for splitting bills, tracking debts, and settling balances across users and groups. Built with **Java 21**, **Spring Boot 3.4**, **Spring Cloud 2024**, **PostgreSQL**, **Apache Kafka**, **Redis**, and a full observability stack.

> Package root: `com.sankalp.expensetracker` · Maven multi-module build · Independent of any managed cloud platform.

---

## What's New (P1 + P2 iteration · 2026-02-28)

| | |
|---|---|
| Flyway migrations  | `V1__init.sql` per service · `ddl-auto=validate` · `baseline-on-migrate=true` |
| Testcontainers IT  | `AuthServiceIntegrationTest` — register → DB → Kafka `user.registered` end-to-end |
| WebSocket live UI  | `useBalanceSocket` hook subscribes to `/topic/balances/{groupId}` on settlement-service |
| S3 storage         | `S3FileStorageProvider` activated by `STORAGE_PROVIDER=s3` (uses AWS default credential chain) |
| OCR pipeline       | Pluggable `OcrProvider` · `NoOpOcrProvider` (default) · `TesseractOcrProvider` (when `OCR_PROVIDER=tesseract`); async-extracts text on receipt upload |
| Google OAuth2      | `spring-boot-starter-oauth2-client` in auth-service; redirects to `/oauth2/callback` on the frontend with tokens in URL fragment. Set `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET` to enable |

### Enabling optional features

```bash
# .env additions (all optional)
STORAGE_PROVIDER=s3
AWS_S3_BUCKET=my-receipts
AWS_REGION=us-east-1
AWS_ACCESS_KEY_ID=...
AWS_SECRET_ACCESS_KEY=...

OCR_PROVIDER=tesseract

GOOGLE_CLIENT_ID=...
GOOGLE_CLIENT_SECRET=...
# Add http://localhost:8081/login/oauth2/code/google to authorized redirect URIs
```

For the React frontend to show the Google button, set in `/app/frontend/.env`:
```
REACT_APP_GOOGLE_OAUTH_ENABLED=true
```

---

## Architecture

```
                              ┌────────────────┐
                              │  React Client  │
                              └────────┬───────┘
                                       │ HTTPS
                              ┌────────▼───────┐         ┌──────────────┐
                              │  API Gateway   │◄────────│ Spring Cloud │
                              │ (Spring Cloud) │  routes │   Gateway    │
                              └────────┬───────┘         └──────────────┘
            JWT/headers forwarded      │                 ┌──────────────┐
                                       │                 │   Eureka     │
        ┌──────────┬─────────┬─────────┼────────┬──────► │   Server     │
        ▼          ▼         ▼         ▼        ▼        └──────────────┘
   ┌─────────┐ ┌────────┐ ┌────────┐ ┌────────┐ ┌─────────────┐
   │  Auth   │ │  User  │ │ Group  │ │ Expense│ │ Settlement  │
   │ Service │ │Service │ │Service │ │Service │ │  Service    │
   └────┬────┘ └───┬────┘ └───┬────┘ └───┬────┘ └──────┬──────┘
        │db        │db        │db        │db           │db
   ┌────▼────┐ ┌───▼────┐ ┌───▼────┐ ┌───▼────┐ ┌──────▼──────┐
   │ PG auth │ │ PG user│ │PG group│ │PG expns│ │ PG settle    │
   └─────────┘ └────────┘ └────────┘ └────────┘ └─────────────┘
        │          │          │          │           │
        └──────────┴──────────┴──────────┴───────────┘
                                │
                          ┌─────▼─────┐
                          │  Kafka    │ ── notification-service
                          │ (events)  │ ── analytics-service
                          └───────────┘

   Cross-cutting: Redis (cache/blacklist/rate-limit) · Zipkin · Prometheus · Grafana
```

### Microservices

| Service                | Port | Database              | Notes                                                       |
|------------------------|------|-----------------------|-------------------------------------------------------------|
| `eureka-server`        | 8761 | —                     | Service registry                                            |
| `config-server`        | 8888 | —                     | Native classpath config (sample for git/vault swap)         |
| `api-gateway`          | 8080 | —                     | Spring Cloud Gateway · JWT validation · Redis rate-limit    |
| `auth-service`         | 8081 | `authdb`              | Register/login, JWT + refresh, BCrypt, blacklist via Redis  |
| `user-service`         | 8082 | `userdb`              | Profiles, friend graph, search, auto-provision on register  |
| `group-service`        | 8083 | `groupdb`             | Groups, members, admin roles, invite codes                  |
| `expense-service`      | 8084 | `expensedb`           | Expense CRUD, 4 split modes, receipts, recurring scheduler  |
| `settlement-service`   | 8085 | `settlementdb`        | Pairwise balances, debt simplification, WebSocket updates   |
| `notification-service` | 8086 | `notificationdb`      | Kafka listeners → DB + optional email                       |
| `analytics-service`    | 8087 | `analyticsdb`         | Aggregations, monthly trends, group contributions (cached)  |

### Kafka Topics (event-driven choreography)

| Topic                  | Producer                | Consumers                                |
|------------------------|-------------------------|------------------------------------------|
| `user.registered`      | auth-service            | user-service                             |
| `user.invited`         | group-service           | notification-service                     |
| `group.created`        | group-service           | notification-service                     |
| `expense.created`      | expense-service         | settlement-service, notification, analytics |
| `expense.updated`      | expense-service         | settlement-service                       |
| `settlement.completed` | settlement-service      | notification-service                     |

---

## Tech Stack

- **Java 21**, Spring Boot 3.4.1, Spring Cloud 2024.0.0, Maven
- **PostgreSQL 16** (database-per-service)
- **Redis 7** (caching, JWT blacklist, rate-limiting)
- **Apache Kafka** (confluentinc/cp-kafka:7.6.1)
- **Spring Cloud Gateway** + **Eureka** + **Config Server**
- **Spring Security** (JWT HS256, BCrypt cost 12)
- **Resilience4j** (circuit breakers, retries)
- **Micrometer + Zipkin** (distributed tracing with W3C `traceparent`)
- **Prometheus + Grafana** (metrics & dashboards)
- **Springdoc OpenAPI** (Swagger UI per service)
- **JUnit 5 + Mockito + Testcontainers**
- **Docker + Docker Compose** + Kubernetes manifests

---

## Quick Start

### Prerequisites
- Docker 24+ and Docker Compose v2
- (Local builds only) JDK 21 and Maven 3.9+

### Run everything with Docker Compose

```bash
cd expensetracker
cp .env.example .env       # tweak JWT_SECRET in production
docker compose up -d --build
```

To wipe local Postgres data and recreate every service database from migrations:

```bash
scripts/reset-dbs.sh --yes
```

Wait ~60 seconds for everything to register, then:

- API Gateway: <http://localhost:8080>
- Eureka dashboard: <http://localhost:8761>
- Swagger per service: <http://localhost:8081/swagger-ui.html>, 8082, …, 8087
- Zipkin: <http://localhost:9411>
- Prometheus: <http://localhost:9090>
- Grafana: <http://localhost:3001> (admin/admin)

### Smoke test

```bash
# 1. Register
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"alice@example.com","password":"Password123!","fullName":"Alice"}'

# 2. Login
ACCESS=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"alice@example.com","password":"Password123!"}' | jq -r .data.accessToken)

# 3. Get profile (auto-provisioned via Kafka)
curl http://localhost:8080/api/users/me -H "Authorization: Bearer $ACCESS"
```

### Build locally (without Docker)

```bash
./mvnw clean install -DskipTests   # or: mvn clean install
java -jar eureka-server/target/eureka-server-1.0.0.jar &
java -jar api-gateway/target/api-gateway-1.0.0.jar &
java -jar auth-service/target/auth-service-1.0.0.jar &
# ... etc
```

### Run tests

```bash
mvn test                            # all modules
mvn -pl expense-service test        # one module
```

Key tested logic:
- `ExpenseSplitterTest` — 4 split modes & penny-rounding correctness
- `DebtSimplifierTest` — greedy debt minimization

### Deploy to Kubernetes

```bash
kubectl apply -f k8s/00-namespace-config.yaml
kubectl apply -f k8s/01-infrastructure.yaml
kubectl apply -f k8s/02-databases.yaml
kubectl apply -f k8s/03-platform-services.yaml
kubectl apply -f k8s/04-business-services.yaml
```

Each service has an `HorizontalPodAutoscaler` example. Replace `image: expensetracker/<svc>:1.0.0` with your registry path.

---

## API Highlights

All routes go through the gateway at `:8080`. Auth-free endpoints: `/api/auth/{register,login,refresh,verify-email}`.

### Auth
- `POST /api/auth/register` · `POST /api/auth/login` · `POST /api/auth/refresh` · `POST /api/auth/logout`

### Users
- `GET /api/users/me` · `PUT /api/users/me`
- `GET /api/users/search?q=`
- `POST /api/users/friends/{addresseeId}` · friend lifecycle

### Groups
- `POST /api/groups` · `GET /api/groups/me`
- `POST /api/groups/{id}/members/{memberId}`
- `POST /api/groups/join/{inviteCode}`

### Expenses
- `POST /api/expenses` — body specifies `splitType` ∈ `EQUAL|EXACT|PERCENTAGE|SHARE` and per-user values
- `GET /api/expenses/me` · `GET /api/expenses/group/{id}`
- `POST /api/categories` · `POST /api/receipts/{expenseId}` (multipart)

### Settlements & Balances
- `POST /api/settlements` (full or partial)
- `GET /api/balances/group/{id}` — pairwise balances
- `GET /api/balances/group/{id}/suggestions` — minimum-transaction settlement plan

### Analytics
- `GET /api/analytics/spent?from=&to=`
- `GET /api/analytics/monthly`
- `GET /api/analytics/group/{id}/contributions`

### Real-time
- WebSocket endpoint: `ws://localhost:8085/ws` (SockJS), topic `/topic/balances/{groupId}`

---

## Project Layout

```
expensetracker/
├── pom.xml                       # parent
├── docker-compose.yml
├── Dockerfile                    # single multi-module Dockerfile (uses MODULE arg)
├── .env.example
├── common-lib/                   # JWT util, AuditableEntity, ApiResponse, events, exceptions
├── eureka-server/
├── config-server/
├── api-gateway/                  # JWT filter, rate limiter, route table
├── auth-service/                 # /api/auth/**
├── user-service/                 # /api/users/**
├── group-service/                # /api/groups/**
├── expense-service/              # /api/expenses, /api/categories, /api/receipts
├── settlement-service/           # /api/settlements, /api/balances, /ws
├── notification-service/         # /api/notifications + Kafka consumers
├── analytics-service/            # /api/analytics
├── infra/prometheus/             # scrape config
├── k8s/                          # full deployment manifests
└── postman/                      # Postman collection
```

Each service follows the same layered convention:
```
controller/   service/   repository/   entity/   dto/   config/   storage/ (if files)
```

---

## Design Decisions

- **Database-per-service**: each domain owns its schema; cross-service queries happen via Kafka events or OpenFeign.
- **Symmetric JWT (HS256)** for simplicity. Same `JWT_SECRET` shared via env. For production switch to RSA/JWKS with `auth-service` as the issuer.
- **Gateway pre-validates** JWT, then injects `X-User-Id` / `X-User-Email` / `X-User-Roles` headers. Downstream services revalidate (defense in depth).
- **Token rotation**: every refresh revokes the old refresh token (`revoked=true`). Blacklist is in Redis with TTL.
- **Debt simplification**: greedy O(n log n) using two priority queues. Suggests at most N−1 payments for N participants.
- **Balance canonicalization**: each `(groupId, userPair, currency)` has a single row; sign of `amount` encodes direction.
- **Penny safety in splits**: rounding remainder is always absorbed by the last split → invariants hold even with 33.33% × 3.
- **Receipts**: pluggable `FileStorageProvider`; default = local disk. Swap to S3 via `app.storage.provider=s3`.
- **Recurring expenses**: stored as templates with `nextOccurrence`; daily scheduler clones them at 02:00.
- **Observability**: every request gets a `X-Correlation-Id` (MDC), Brave traces are forwarded to Zipkin, Prometheus scrapes `/actuator/prometheus` on every service.

---

## Postman

Import `postman/ExpenseTracker.postman_collection.json` — it pre-configures the `{{baseUrl}} = http://localhost:8080` and an auth helper that auto-stores the access token after login.

---

## License

MIT (sample / educational).
