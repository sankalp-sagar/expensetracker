# ExpenseTracker — Product Requirements Document

## Original Problem Statement
Build a production-grade distributed expense sharing and settlement platform using Java, Spring Boot, and Microservices Architecture. Application name: **expensetracker**. Package: `com.sankalp.expensetracker`. Must not depend on Emergent platform after the app is built. Tech: Maven, latest Java (21 LTS), Spring Boot 3.4, PostgreSQL primary DB, Kafka, Redis, Spring Cloud Gateway/Eureka/Config, Docker, K8s.

## Architecture
- 8 microservices: eureka-server, config-server, api-gateway, auth-service, user-service, group-service, expense-service, settlement-service, notification-service, analytics-service
- Database-per-service (Postgres 16, one schema per microservice)
- Event-driven choreography via Kafka (user-registered, group-created, expense-created, settlement-completed, etc.)
- API Gateway with JWT pre-validation, Redis rate-limiting, header propagation
- Distributed tracing (Zipkin), metrics (Prometheus + Grafana), correlation IDs (MDC)
- Resilience4j circuit breakers / retries

## User Personas
- **Roommates / housemates**: split rent, utilities, groceries with recurring expenses.
- **Travel groups**: trip expense tracking with multi-currency.
- **Friends / dinner clubs**: ad-hoc bill splitting with smart settle-up suggestions.
- **Engineering hiring managers**: reviewing this as a portfolio piece — assess distributed systems fluency.

## Core Requirements (static)
- JWT auth (custom, HS256, refresh token rotation, BCrypt-12, Redis blacklist)
- 4 split modes: EQUAL, EXACT, PERCENTAGE, SHARE — all penny-safe
- Debt simplification: greedy algorithm reducing to at most N-1 transactions
- Real-time balance updates over WebSocket (STOMP/SockJS)
- Receipt uploads (pluggable storage provider; default local, S3-ready)
- Recurring expenses with daily scheduler at 02:00
- Email notifications (toggle via `MAIL_ENABLED`)
- Multi-currency support
- Pagination, indexing, audit timestamps, soft delete

## What's been implemented (2026-02-28)

### Iteration 2 — P1 + P2 (this run)
- ✅ **Flyway migrations**: `V1__init.sql` per service; `ddl-auto=validate`; `baseline-on-migrate=true`; common-lib bundles `flyway-core` + `flyway-database-postgresql`
- ✅ **Testcontainers integration test**: `AuthServiceIntegrationTest` boots Postgres + Kafka + Redis containers, calls `POST /api/auth/register` via MockMvc, asserts DB persistence + Kafka `user.registered` event + login round-trip
- ✅ **WebSocket subscribe in frontend**: `useBalanceSocket` hook + live indicator on `GroupDetailPage` re-fetches suggestions on every push from settlement-service
- ✅ **S3 storage provider**: `S3FileStorageProvider` activated by `STORAGE_PROVIDER=s3`, presigned URLs, AWS default credential chain
- ✅ **OCR pipeline**: pluggable `OcrProvider` (`NoOpOcrProvider` default · `TesseractOcrProvider` when `OCR_PROVIDER=tesseract`); Tesseract installed only in expense-service Docker image; OCR extracts text synchronously on receipt upload and persists to `Receipt.ocrText`
- ✅ **Google OAuth2**: `spring-boot-starter-oauth2-client` in auth-service; conditional registration only when `GOOGLE_CLIENT_ID` is set; success handler issues our standard JWT pair and redirects to `/oauth2/callback` on the frontend with tokens in URL fragment; React `OAuth2CallbackPage` consumes them
- ✅ Docker base swapped Alpine → Debian-slim for better Tess4j JNI compatibility
- ✅ Updated `.env.example`, `docker-compose.yml`, README with all new env vars

### Iteration 1 — MVP (previous run)
- All 8 microservices + infra + frontend dashboard + K8s + Postman (see history above)

## File count
- **144 total files** in `/app/expensetracker`
- 7 Flyway V1 migrations · 3 new OCR classes · 2 new auth classes (OAuth2 handler + updated SecurityConfig) · 1 new storage provider · 1 new frontend hook · 1 new frontend page

### Backend (Java 21, Spring Boot 3.4.1, Spring Cloud 2024.0.0)
- ✅ Parent Maven POM with managed dependency versions
- ✅ `common-lib`: JwtUtil, JwtAuthenticationFilter, CorrelationIdFilter, ApiResponse, GlobalExceptionHandler, AuditableEntity, Event DTOs, Kafka topic constants
- ✅ `eureka-server` (port 8761) — service discovery
- ✅ `config-server` (port 8888) — Spring Cloud Config (native classpath)
- ✅ `api-gateway` (port 8080) — Spring Cloud Gateway, JWT validation filter, Redis rate-limiter, route table
- ✅ `auth-service` (port 8081, DB authdb) — register/login/refresh/logout, BCrypt-12, refresh rotation, Redis JWT blacklist, publishes `user.registered`
- ✅ `user-service` (port 8082, DB userdb) — profiles auto-provisioned via Kafka, friend graph, search, Redis cache
- ✅ `group-service` (port 8083, DB groupdb) — groups, members, admin roles, invite codes, join-by-code, publishes `group.created`
- ✅ `expense-service` (port 8084, DB expensedb) — full CRUD, 4-mode splitter with penny safety, categories, receipts (pluggable storage), recurring scheduler, publishes `expense.created`
- ✅ `settlement-service` (port 8085, DB settlementdb) — pairwise balance ledger, debt-simplification algorithm, settlement history, WebSocket broadcast on balance change
- ✅ `notification-service` (port 8086, DB notificationdb) — Kafka listeners for all events, persistent notification feed, optional email
- ✅ `analytics-service` (port 8087, DB analyticsdb) — expense facts projection, monthly trends, group contributions, Redis cache
- ✅ Unit tests: `ExpenseSplitterTest` (5 cases), `DebtSimplifierTest` (3 cases)
- ✅ Single multi-module `Dockerfile` with `MODULE` build arg
- ✅ Full `docker-compose.yml` with 7 Postgres instances, Redis, Kafka, Zookeeper, Zipkin, Prometheus, Grafana
- ✅ K8s manifests: namespace+config+secrets, infra, databases, platform, business services + HPA on gateway
- ✅ Postman collection with token auto-capture
- ✅ Comprehensive README + .env.example + .gitignore

### Frontend (React 19 + Tailwind + shadcn/ui)
- ✅ Login + Register pages (Swiss/high-contrast design, Cabinet Grotesk + JetBrains Mono)
- ✅ AppLayout with sidebar nav, notifications dropdown, logout
- ✅ Dashboard: KPIs, monthly bar chart, group list, recent expenses
- ✅ Groups page with create + join-by-code dialogs
- ✅ Group detail: balances table, settle-up suggestions, expenses table, ExpenseFormDialog with all 4 split modes
- ✅ Expenses page (cross-group view)
- ✅ Settings (profile management)
- ✅ Axios client with auto-refresh on 401, JWT bearer injection
- ✅ Configurable backend URL via `REACT_APP_API_BASE` (default `http://localhost:8080`)
- ✅ data-testid on every interactive/informational element

## File counts
- 131 files total under `/app/expensetracker`
- React frontend: ~12 components + pages

## Prioritized backlog (P1 / P2)

### P1 (next iteration)
- Wire WebSocket subscription in frontend `GroupDetailPage` for live balance updates
- Add Integration tests with Testcontainers for one critical flow (expense → balance update via Kafka)
- Add Resilience4j `@CircuitBreaker` on outgoing Feign calls
- Add OpenAPI grouping in api-gateway so Swagger lists all services in one place

### P2 (later)
- OAuth2 / Google login (currently JWT only as user requested)
- S3FileStorageProvider implementation alongside LocalFileStorageProvider
- OCR pipeline on uploaded receipts (entity fields are already in place)
- Email templating via Thymeleaf + a real SMTP provider (SendGrid / Resend)
- ELK stack (Elasticsearch + Logstash + Kibana) — Zipkin currently covers tracing; ELK is large and was scoped out for first pass
- Production-grade Postgres StatefulSets with PVCs in K8s
- Database migrations via Flyway instead of `ddl-auto=update`
- gRPC for inter-service synchronous calls
- Stripe integration for in-app settlements

### Out of scope (1st iteration)
- ELK stack manifests (commented in compose; covered by Zipkin for tracing)
- gRPC, Saga orchestrator (event choreography used instead)
- iOS / Android native apps

## Next actions for the user
1. `cd /app/expensetracker && cp .env.example .env`
2. `docker compose up -d --build` (first build ≈ 8 min depending on bandwidth)
3. Visit:
   - <http://localhost:8761> — Eureka
   - <http://localhost:8080/api/auth/register> — gateway
   - <http://localhost:9090> — Prometheus
   - <http://localhost:3001> — Grafana (admin/admin)
   - <http://localhost:9411> — Zipkin
4. (Optional) Start the React frontend separately: `cd /app/frontend && yarn start` — it talks to `http://localhost:8080`.
