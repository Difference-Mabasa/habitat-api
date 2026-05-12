# Habitat API — Build Order

Companion to the scaffold. Tracks what's done, what's in flight, and what's
next. Every check-mark below traces to a section of [README.md](./README.md)
or [development-standards.md](./development-standards.md).

---

## Phase 0 — Scaffold ✅ (2026-05-12)

Everything in the table at the top of README.md "What's in this scaffold".

- [x] Maven project (Java 21, Spring Boot 3.3.5).
- [x] Package layout (config / constants / controller / dto / entity / enums / exception / repository / security / service / util).
- [x] `BaseEntity` with audit + soft-delete columns.
- [x] Custom exception hierarchy + `GlobalExceptionHandler`.
- [x] `ApiError` + `PageResponse<T>` contracts.
- [x] `JwtService` + `JwtAuthenticationFilter` + jti claim + Redis blocklist read.
- [x] `RequestIdFilter` + MDC + `X-Request-Id` response header.
- [x] `SecurityConfig` — HSTS, CSP, X-Frame DENY, nosniff, strict CORS allowedHeaders.
- [x] `WebConfig.PageSizeFilter` — global page-size cap at 100.
- [x] `RestTemplateConfig` — explicit timeouts.
- [x] Flyway V1 — users, user_roles, shedlock, webhook_events.
- [x] application.yml + application-prod.yml + application-test.yml.
- [x] logback-spring.xml — plain text for non-prod, JSON Logstash encoder for prod.
- [x] docker-compose.yml + Dockerfile (multi-stage).
- [x] ArchUnit baseline.
- [x] Smoke test against Testcontainers PostgreSQL.
- [x] Pre-commit hook script.
- [x] Working `POST /api/v1/auth/{register,login}` end-to-end.
- [x] `GET /api/v1/health`.

---

## Phase 1 — Identity & auth completion

- [ ] `POST /auth/refresh` — refresh token rotation, old jti -> Redis blocklist.
- [ ] `POST /auth/logout` — add access token jti to Redis blocklist (TTL = remaining lifetime).
- [ ] `POST /auth/oauth2/exchange` — single-use 30-second opaque code -> tokens.
- [ ] `POST /auth/forgot-password` + `POST /auth/reset-password` — email-driven.
- [ ] Email verification token on register + `POST /auth/verify-email`.
- [ ] Refresh-token device-fingerprint binding (hashed UA + IP subnet).
- [ ] `GET /api/v1/users/me` (returns the principal's profile).
- [ ] `PATCH /api/v1/users/me/role` — switch active role within owned roles.

## Phase 2 — Core domain (port from backroom-api)

Each entity below: JPA mapping + repository + service + controller + DTOs +
ArchUnit rules where applicable. **Apply the N+1 fix on the way in** — use
`JOIN FETCH` / projections, never `toResponse()` per-row loops.

- [ ] `Area`, `Amenity` (slow-changing reference data — cache via `@Cacheable`).
- [ ] `RentalProperty` (with `landlord` vs `manager` semantics).
- [ ] `Unit` (with denormalised `manager` from property).
- [ ] `Application` (+ documents).
- [ ] `Booking` (viewing requests).
- [ ] `Mandate` (the agent-on-behalf flow, three sub-flows).
- [ ] `Agency` (CRUD + public browse).
- [ ] `Lease` (signing + PDF generation via Thymeleaf, not raw HTML strings).
- [ ] `Conversation` + `Message` (unit-scoped messaging).
- [ ] `Community` + `CommunityMember` + `CommunityMessage` (separate from Conversation).
- [ ] `PropertyAccessResolver` (OWNER → AGENT → MANAGER priority — the only place that authorises property-level access).

## Phase 3 — Money

- [ ] `RentalInvoice` + state machine (PENDING / PAID / EXPIRED / FAILED).
- [ ] Ozow integration — wrapped in `@CircuitBreaker` + `@Retry` + 3s/10s timeouts.
- [ ] Webhook handler persists raw payload to `webhook_events` before processing.
- [ ] Trust-account payout flow.
- [ ] `BusinessDayService` — cache the holiday set once in `@PostConstruct` (don't rebuild per call).
- [ ] Externalise SA public holidays (DB-seeded or nager.date integration).

## Phase 4 — Notifications + comms

- [ ] `Notification` entity + service (in-app).
- [ ] Email delivery (Resend, per standards §20).
- [ ] WhatsApp / SMS integration.
- [ ] Notification archival job — soft-deletes READ notifications older than 30 days.

## Phase 5 — Storage + observability

- [ ] `StorageService` interface + `LocalStorageService` + `S3StorageService`.
- [ ] Tika magic-byte validation on every upload.
- [ ] Path-traversal guard on `GET /files/{path}`.
- [ ] Custom `HealthIndicator` beans (storage writable, Redis, Ozow).
- [ ] OpenTelemetry exporter wired against the chosen OTLP collector.

## Phase 6 — Rate limiting + scheduled jobs

- [ ] Bucket4j-Redis filter on `POST /auth/*` and sensitive write endpoints.
- [ ] First scheduled job (e.g. expire stale invoices) — guarded by `@SchedulerLock`.
- [ ] Notification archival job — same.

## Phase 7 — Admin

- [ ] `AdminController` with audit-log endpoints.
- [ ] User CRUD with privileged-role creation (ADMIN-only).
- [ ] Property / lease / agency moderation queues.

---

## Conventions during build-out

> Every PR follows the 7-step Mandatory Change Workflow in
> [development-standards.md §17](./development-standards.md).

- **No new visual tokens / patterns added without thinking through where the parallel constants class lives.**
- **`mvn verify` must be green** before any merge.
- **Postman collection** under `postman/` is updated for every endpoint change.
- **README.md table** of capabilities is updated when status flips.
- **One commit per logical unit.** Phase commits use `phase N: <name>`. Never include `Co-Authored-By: Claude`.
