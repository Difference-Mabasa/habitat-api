# Habitat API

REST API for **Habitat** — South Africa's verified rental platform. Mirrors the domain model that lived in `backroom-api`, but built from day one against the [development-standards.md](./development-standards.md) playbook of lessons-learnt.

---

## What's in this scaffold

| Capability | Status |
|---|---|
| Java 21 + Spring Boot 3.3.5 + Maven | ✅ |
| `BaseEntity` with `createdBy` / `updatedBy` / `deletedAt` + Spring Data JPA auditing | ✅ |
| Custom exception hierarchy (`ApiException` + 7 subclasses) + `GlobalExceptionHandler` | ✅ |
| Typed `ApiError` + `PageResponse<T>` contracts | ✅ |
| Stateless JWT auth (`JwtService` + `JwtAuthenticationFilter`) with **jti claim + Redis blocklist** wiring | ✅ |
| `RequestIdFilter` — every response carries `X-Request-Id`, every log line has it via MDC | ✅ |
| `SecurityConfig` with HSTS, CSP, X-Frame-Options DENY, nosniff, strict CORS allowedHeaders | ✅ |
| Page-size cap at 100 (global filter) | ✅ |
| `RestTemplate` with explicit 3s connect / 10s read timeouts | ✅ |
| Flyway with `validate-on-migrate: true` from V1 | ✅ |
| Redis (Spring Data + cache) wired for sessions, blocklists, ShedLock | ✅ |
| **ShedLock** registered (`@EnableSchedulerLock`) — every future `@Scheduled` is auto-locked | ✅ |
| Bucket4j Redis dep present for distributed rate limiting | ✅ |
| Resilience4j (Spring Boot starter) ready for `@CircuitBreaker` / `@Retry` | ✅ |
| Apache Tika (file MIME validation) | ✅ |
| OpenTelemetry + Micrometer + Logstash JSON encoder | ✅ |
| `application-prod.yml` exists from day one — INFO logging, internal management port | ✅ |
| ArchUnit baseline tests (layering / naming / exception / DTO rules) | ✅ |
| Smoke `contextLoads` test against Testcontainers PostgreSQL | ✅ |
| Pre-commit hook blocking bare `RuntimeException`, tokens-in-URL, Flyway edits | ✅ |
| `AuthController` + `AuthService` — `POST /api/v1/auth/{register,login}` | ✅ working end-to-end |
| `HealthController` — `GET /api/v1/health` | ✅ |
| OAuth2 exchange endpoint (`POST /auth/oauth2/exchange`) | ⏳ stubbed in routes, service pending |
| Property / Unit / Application / Lease / Mandate domain | ⏳ next sprints |

---

## Quick start

There are two ways to run the API locally:

- **Full-stack Docker** (recommended for manual UI testing) — Postgres + Redis + API + UI all in containers, orchestrated from `~/IdeaProjects/habitat-stack/`. See [`../habitat-stack/README.md`](../habitat-stack/README.md) for the day-to-day command list. Don't run both this path and the IDE path below at the same time — they share container names and ports.
- **IDE + infra-only Docker** (recommended when you're iterating on Java) — Postgres + Redis in containers via this repo's `docker-compose.yml`, with the API running directly from your IDE so hot-reload + the debugger work.

### Path A — IDE + infra-only Docker

```bash
docker compose up -d              # Postgres on :5432, Redis on :6379
mvn spring-boot:run               # or run HabitatApiApplication from IntelliJ
```

Flyway applies every migration under `src/main/resources/db/migration/` at startup.

### Path B — Full-stack Docker

```bash
cd ../habitat-stack
docker compose up -d --build      # builds the api + ui images, brings up everything
```

UI at http://localhost:5173, API at http://localhost:8080 (or via the proxy at http://localhost:5173/api/v1/...).

### Smoke test

```bash
curl -s http://localhost:8080/api/v1/health | jq

curl -s -X POST http://localhost:8080/api/v1/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"email":"sipho@example.co.za","password":"hunter2hunter2","displayName":"Sipho","role":"TENANT"}'
```

You should see an `AuthResponse` with `accessToken`, `refreshToken`, and the user's profile.

### Stop

```bash
docker compose down       # keeps data
docker compose down -v    # wipes volumes
```

---

## Environment variables

All vars default to local-friendly values so the app runs out of the box. **In production, set every one of these explicitly** — the prod profile fails fast on missing values.

| Var | Default | Description |
|---|---|---|
| `DB_HOST` | `localhost` | PostgreSQL host |
| `DB_PORT` | `5432` | PostgreSQL port |
| `DB_NAME` | `habitat_db` | Database name |
| `DB_USERNAME` | `habitat` | DB user |
| `DB_PASSWORD` | `habitat` | DB password |
| `DB_POOL_SIZE` | `20` (prod) | Hikari max pool size |
| `REDIS_HOST` | `localhost` | Redis host |
| `REDIS_PORT` | `6379` | Redis port |
| `REDIS_PASSWORD` | _(empty)_ | Redis password |
| `JWT_SECRET` | dev default | **Must be ≥32 bytes**. Startup validates this. |
| `CORS_ORIGINS` | `http://localhost:5173` | Comma-separated allowed origins |
| `STORAGE_TYPE` | `local` (dev) / `s3` (prod) | File storage adapter |
| `UPLOAD_DIR` | `./uploads` | Local upload path |
| `PORT` | `8080` | API port |

---

## Install the pre-commit hook

The project ships a hook that enforces the bans in [development-standards.md](./development-standards.md):

```bash
ln -s ../../scripts/pre-commit.sh .git/hooks/pre-commit
chmod +x .git/hooks/pre-commit
```

It blocks:

- `throw new RuntimeException(...)` — use the typed hierarchy.
- Tokens in URL query parameters — `Authorization: Bearer` only.
- Editing already-committed Flyway migrations — new file per change.
- `@Enumerated` without `EnumType.STRING`.

Warns on:

- `file.getContentType()` not paired with Tika magic-byte validation.

---

## Build, test, scan

`mvn verify` runs the full pipeline:

- Unit + integration tests (JUnit 5 + Mockito + Testcontainers).
- **ArchUnit** rules (`ArchitectureRulesTest`) — fail on layer / naming / exception / DTO violations.
- **JaCoCo** coverage XML report (read by SonarQube).
- **SpotBugs** static bug analysis (fails on Medium+).
- **PMD** code-smell scan (reports, doesn't block).

For SonarQube uploads (against a local `localhost:9000` Docker instance):

```bash
mvn verify sonar:sonar -Dsonar.token=$SONAR_TOKEN
```

---

## Package layout

```
com.habitat.api
├── HabitatApiApplication.java
├── config/         // @Configuration — Security, JpaAuditing, RestTemplate, WebConfig
├── constants/      // ErrorMessages, JwtConstants, ApiRoutes, PublicEndpoints, StorageConstants
├── controller/     // @RestController — one prefix per controller
├── dto/            // ApiError, FieldError, PageResponse, dto.auth.*
├── entity/         // JPA entities; entity.base for BaseEntity
├── enums/          // domain enums (Role for now)
├── exception/      // ApiException + subclasses + GlobalExceptionHandler
├── repository/     // JpaRepository<Entity, UUID>
├── security/       // JwtService, JwtAuthenticationFilter, RequestIdFilter, SecurityUtils, HabitatPrincipal
├── service/        // @Service @Transactional — business logic
└── util/           // pure helpers
```

Every rule about where things live is enforced by `ArchitectureRulesTest`.

---

## Next sprints

What's *deliberately* not in the scaffold — track these in `build-order.md` as they're picked up.

### Sprint 1 — Identity & auth completion

- [ ] Refresh-token endpoint (`POST /auth/refresh`) + Redis-backed token rotation.
- [ ] Logout endpoint — add `jti` to the Redis blocklist with TTL.
- [ ] OAuth2 exchange code flow (single-use 30-second code → token exchange).
- [ ] Password reset (request → email → reset).
- [ ] Email verification on register (token in email → confirm endpoint).
- [ ] Refresh-token binding to device fingerprint.

### Sprint 2 — Core domain

- [ ] `Property`, `Unit`, `Application`, `Lease`, `Mandate`, `Conversation` entities (port from backroom, fix N+1 patterns up front).
- [ ] DTOs + controllers for each above.
- [ ] `PropertyAccessResolver` (OWNER → AGENT → MANAGER priority).

### Sprint 3 — Money

- [ ] `RentalInvoice`, payment provider integration (Ozow — `@CircuitBreaker` + `@Retry` + 3s/10s timeouts from the start).
- [ ] Webhook events persisted to `webhook_events` before processing.
- [ ] Trust-account payout flow.

### Sprint 4 — Notifications + comms

- [ ] In-app `Notification` entity + service.
- [ ] Email notifications (Resend — TBD per standards §20).
- [ ] WhatsApp / SMS integration.

### Sprint 5 — Storage + observability

- [ ] `StorageService` interface + `LocalStorageService` (dev) + `S3StorageService` (prod).
- [ ] Tika MIME validation on every upload.
- [ ] OpenTelemetry exporter wired against the chosen collector.
- [ ] Custom `HealthIndicator` beans (storage writable, Redis, payment provider).

See [development-standards.md](./development-standards.md) for the rationale on every conventions decision above.
