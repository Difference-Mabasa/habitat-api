# Habitat API — Development Standards

> **Audience:** Anyone writing or reviewing code in `habitat-api`.
> **Status:** Authoritative. PRs that violate these are rejected.
> **Origin:** Distilled from running `backroom-api` for ~18 months. Every "do" and "do not" below maps to a specific incident or piece of tech debt that bit us. Each rule has a one-line *why* — the rationale matters more than the rule.
> **Last reviewed:** 2026-05-12.

`habitat-api` reuses the domain model and most of the Java code from `backroom-api`. This document captures **the things we will do differently from day one**, plus the conventions we already proved out and want to keep.

---

## 0. The Stack

Same shape as backroom — proven, no need to rewrite.

| Layer | Technology | Notes |
|---|---|---|
| Language | Java 21 LTS | up from 17 — virtual threads, pattern matching, sequenced collections |
| Framework | Spring Boot 3.3.x | will follow Spring's LTS line |
| Persistence | PostgreSQL 16 + Hibernate via Spring Data JPA | Flyway for schema |
| Cache / locks | Redis 7 | actually used this time — see §8 and §10 |
| Auth | JWT (JJWT 0.12.x) + Google OAuth2 + email-password | with revocation, see §6 |
| Validation | Spring Validation (Jakarta) | + custom `@ConstraintValidator` per domain |
| Boilerplate | Lombok | `@Getter` `@Builder` `@RequiredArgsConstructor` — no `@Data` on JPA entities |
| Resilience | Resilience4j | wraps every outbound HTTP call (§9) |
| Rate limiting | Bucket4j + Redis | distributed from day one (§10) |
| Scheduling | Spring `@Scheduled` + ShedLock + Redis | distributed lock on every job (§10) |
| Testing | JUnit 5 + Mockito + AssertJ + Testcontainers | + ArchUnit + JaCoCo + SpotBugs + PMD on `mvn verify` |
| Observability | Micrometer + OpenTelemetry + Loki/Grafana | structured JSON logs + correlation IDs (§14) |
| Build | Maven | matches backroom; no point switching mid-stream |

Build & deploy targets are out of scope for this doc.

---

## 1. Package Layout

```
com.habitat.api
├── HabitatApiApplication.java
├── config/        // @Configuration classes only — no business logic
├── constants/     // typed constants per domain (see §5)
├── controller/    // @RestController — thin, one prefix per controller
├── dto/           // request + response records, never entities on the wire
├── entity/        // JPA entities; entity.base for BaseEntity
├── enums/         // domain enums (NEW vs backroom — used to live in entity/)
├── exception/     // custom hierarchy + GlobalExceptionHandler
├── repository/    // JpaRepository<Entity, UUID>; custom @Query as needed
├── security/      // filters, JwtService, SecurityConfig, SecurityUtils
├── service/       // @Service @Transactional; business rules live here
└── util/          // pure functions, no Spring beans
```

**Hard rules:**

- One controller per URL prefix. `POST /applications/{id}/invoice` → `ApplicationController` (because `/applications` is the URL parent). `GET /invoices/{id}` → `InvoiceController`. *Why:* In backroom we had endpoints sprayed across whoever's service was being called — debugging an endpoint meant first finding which controller owned it.
- Controllers **never** call repositories. Always through a service. *Why:* Untestable, leaks persistence into HTTP.
- Services **never** inject `EntityManager`. Use the repository abstraction. *Why:* Keeps query surface area discoverable.
- `config/` holds `@Configuration` and `@ConfigurationProperties` only.

---

## 2. Domain Conventions

Inherited from backroom and load-bearing — keep them.

- **Primary keys are UUID** (`@GeneratedValue(strategy = GenerationType.UUID)`). Never auto-increment Long. *Why:* IDs leak business volume; UUIDs are safe to expose in URLs.
- **Timestamps are `OffsetDateTime` in UTC**. Never `LocalDateTime`, never `Date`, never `java.sql.Timestamp`. *Why:* SA-specific timezone bugs cost us a sprint.
- **All enums:**
  - Live in their own `enums/` package (NOT mixed into `entity/`).
  - Are stored as `@Enumerated(EnumType.STRING)` on every JPA field. **No exceptions.** *Why:* Default `ORDINAL` means inserting a new enum value in the middle silently re-keys every existing row. This bit us once and we caught it before prod — the rule exists so we don't ever ship it.
- **Every JPA entity extends `BaseEntity`** which provides `id` (UUID), `createdAt`, `updatedAt`, `createdBy`, `updatedBy`, `deletedAt`. See §3 — habitat's `BaseEntity` ships richer than backroom's.
- **Paginated endpoints return `PageResponse<T>`** — a typed wrapper, never raw `List` or `org.springframework.data.domain.Page`. Shape: `{ content, page, size, totalPages, totalElements }`.
- **Page size is capped at 100, defaults to 20.** Enforced by a global `HandlerInterceptor`. *Why:* In backroom callers could request `size=100000` and bring the heap down. Don't trust clients to be reasonable.
- **All money is `BigDecimal` with scale 2, currency "ZAR".** Never `double`, never `Long` cents (we mix scales otherwise).
- **`RentalProperty` semantics** (carried from backroom):
  - `landlord` = registered legal owner (always a User).
  - `manager` = platform contact on the listing — could be the landlord or their agent.
  - `Unit.manager` and `Conversation.manager` are *denormalised* from `property.manager` for query speed; updates must keep them in sync (service-level guard).

---

## 3. BaseEntity — Richer Than Backroom

Backroom's `BaseEntity` had three Hibernate-proprietary fields. Habitat's `BaseEntity` is the corrected version we never got around to writing in backroom:

```java
@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@EqualsAndHashCode(of = "id")
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @CreatedDate
    @Column(name = "created_at", updatable = false, nullable = false)
    private OffsetDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private UUID createdBy;

    @LastModifiedBy
    @Column(name = "updated_by")
    private UUID updatedBy;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;
}
```

Rules that fall out:

- **`@EnableJpaAuditing` is on at startup** with an `AuditorAware<UUID>` bean that reads the principal.
- **`@SQLRestriction("deleted_at IS NULL")`** on every entity that supports soft-delete. We hard-delete only on enum/lookup tables.
- **`equals`/`hashCode` use `id`** with a null-safe guard for transient instances. Lombok `@EqualsAndHashCode(of = "id")` is fine. *Why:* The default JPA equality has bitten us with proxy vs plain comparisons after detach.
- **Join-table entities also extend `BaseEntity`.** No exceptions. *Why:* We can audit the bridge later without a migration.

---

## 4. Exception Hierarchy

Reuse backroom's, but enforce it from day one with the pre-commit hook + ArchUnit.

```
ApiException (RuntimeException base)
├── BadRequestException        → 400
├── UnauthorizedException      → 401
├── ForbiddenException         → 403
├── ResourceNotFoundException  → 404
├── ConflictException          → 409
├── ValidationException        → 422 (NEW — distinct from BadRequest for client-side wiring)
└── ServiceUnavailableException → 503 (NEW — for storage / Ozow downtime)
```

**Hard rules:**

- **Never `throw new RuntimeException(...)`** in production code. The pre-commit hook (carried over from backroom) blocks it. *Why:* It bypasses `GlobalExceptionHandler` and turns into an opaque 500 with a Hibernate stack trace.
- **Never throw a raw `IllegalArgumentException` / `IllegalStateException` past a service boundary.** Translate to `BadRequestException` or `ConflictException` at the boundary.
- **`DataIntegrityViolationException` is mapped to 409** in `GlobalExceptionHandler`, not allowed to fall through to 500. *Why:* In backroom this leaked DB constraint names to users.
- Error responses always use the typed `ApiError` shape — never plain strings, never ad-hoc JSON.

```
ApiError {
  status: int
  code: string        // e.g. "RESOURCE_NOT_FOUND"
  message: string     // user-facing
  requestId: string   // matches MDC requestId — see §14
  timestamp: OffsetDateTime
  errors: List<FieldError> | null   // for 422 only
}
```

---

## 5. The Constants Discipline

This is the single biggest quality lever from backroom. **No user-facing string in code, ever.** Every literal lives in a typed constants class. The pre-commit hook scans for `"..."` in service/controller/security files and warns.

| Domain | Class | Used for |
|---|---|---|
| User-facing errors | `ErrorMessages` | every `BadRequestException`, `ResourceNotFoundException`, etc. message |
| In-app notifications | `NotificationMessages` | title + body templates |
| Document templates | `HtmlTemplates` | full HTML strings for PDFs |
| Placeholders | `TemplatePlaceholders` | `{name}` / `{{date}}` token names |
| JWT | `JwtConstants` | claim keys, header names, `ROLE_` prefix |
| Storage | `StorageConstants` | upload paths, MIME types, allowed extensions |
| OAuth2 | `OAuth2Constants` | provider names, claim keys, redirect params |
| Routes | `ApiRoutes` (NEW) | full controller paths — referenced from `SecurityConfig` |

**Use `TemplateUtils.format(template, key, value, key, value, …)` for substitution.** Never `String.format` on user-facing messages — placeholder names are typed, missing keys throw at boot time via integration test.

---

## 6. Security — Production-Grade From Day One

Backroom shipped a working JWT + OAuth2 stack with a long list of "we'll fix it before prod" items. Habitat ships them done.

### Authentication

- **JWT secret length validated at startup** via `@PostConstruct` — at least 32 bytes. Fail-fast on weak dev defaults.
- **Every access token carries a `jti` claim** (UUID). On logout / password change, `jti` is added to a **Redis blocklist** with TTL = remaining token lifetime.
- **OAuth2 token exchange is via short-lived opaque code**, NOT tokens in the URL fragment:
  1. Provider redirects to `?code=<opaque>` (Redis-backed, 30s TTL, single-use).
  2. Frontend `POST /auth/oauth2/exchange` with the code → JSON body with tokens.
- **Refresh tokens are bound to a device fingerprint** (hashed UA + IP subnet). Reuse from a different fingerprint → revoke the refresh token + alert the user.
- **`RegisterRequest` cannot self-assign `ADMIN` or `SUPER_ADMIN`.** `@JsonDeserialize` whitelists allowed roles. Belt-and-braces assertion in `AuthService`. *Why:* In backroom we caught this in audit before prod.

### Authorization

- **Every endpoint has explicit authorization** — either in `SecurityConfig` (`permitAll()`/`hasRole()`) or via `@PreAuthorize` on the controller method. The default is `authenticated()`.
- **Public endpoints are in a single `PUBLIC_ENDPOINTS` constant.** ArchUnit test ensures `SecurityConfig` references it.
- **Service-layer authorization** mirrors HTTP-layer authorization — `requireAdmin(callerId)`, `requireMember(communityId, callerId)`, etc. *Why:* `@PreAuthorize` can be removed accidentally; service guards stay.
- **`SecurityUtils` is the single source of `hasRole()` and `isPrivileged()`** — never duplicate. Property-level access lives in `PropertyAccessResolver` (OWNER → AGENT → MANAGER priority).

### HTTP hardening

- **Security headers on every response** (no excuses):
  ```java
  HSTS max-age=31536000 includeSubDomains
  X-Content-Type-Options: nosniff
  X-Frame-Options: DENY
  Content-Security-Policy: default-src 'self'
  Referrer-Policy: strict-origin-when-cross-origin
  ```
- **CORS allowedHeaders is a finite list**: `Authorization, Content-Type, X-Requested-With, X-Request-Id`. Never `"*"`.
- **`X-Forwarded-For` is only trusted from configured proxy CIDRs** via Spring's `ForwardedHeaderFilter`. *Why:* Otherwise any client spoofs their IP to bypass rate limiting.

### File / payload safety

- **MIME types validated via Apache Tika magic bytes**, not `file.getContentType()`. *Why:* That header is client-supplied and trivially spoofed.
- **File downloads sanitise resolved paths:**
  ```java
  Path resolved = Paths.get(uploadDir).resolve(filename).normalize();
  if (!resolved.startsWith(Paths.get(uploadDir).normalize()))
      throw new ForbiddenException(ErrorMessages.INVALID_FILE_PATH);
  ```
  *Why:* Defence-in-depth even if DB is trusted today.
- **`/uploads/**` is NOT publicly served.** Sensitive subdirectories (`leases/`, `documents/`) go through `GET /files/{path}` which checks ownership.
- **Payment-related endpoints require:** ownership check + status check + idempotency key.
- **The simulate-payment endpoint exists** — but `@Profile("!prod")` + `@PreAuthorize("hasRole('ADMIN')")`. Both.

### PII in logs

- **Never log full email, full ID number, full phone, or token fragments.** Use the entity UUID as the log key. Hash or last-4 if you must show something. Pre-commit hook scans for the obvious patterns.

---

## 7. Persistence & Queries

The hardest backroom lessons live here.

### Schema

- **Flyway, always.** `ddl-auto: none` (NOT `update`, NEVER `create`). New migration file per change, format `V{next}__short_description.sql`.
- **`flyway.validate-on-migrate: true` from day one.** Backroom turned this off and let migrations drift between envs.
- **`IF [NOT] EXISTS` in DDL** for idempotency.
- **Indexes on every FK column** and every column used in `WHERE` clauses on tables expected to grow past 10k rows.
- **New PostgreSQL enum values use `ALTER TYPE ... ADD VALUE`** — never recreate the type.

### Reads

- **`@Transactional(readOnly = true)` on every read-only service method.** Lets Hibernate skip dirty checks.
- **No N+1 from mapping loops.** The pattern that bit us in backroom (~20 times):
  ```java
  // ❌ Don't:
  return communities.stream().map(c -> toResponse(c)).toList();
  // …where toResponse() runs 3 queries per community.

  // ✅ Do — one JPQL projection:
  @Query("""
      SELECT new com.habitat.api.dto.CommunityRow(
          c.id, c.name, COUNT(m), MAX(CASE WHEN m.userId = :userId THEN m.role END))
      FROM Community c LEFT JOIN c.members m
      GROUP BY c.id, c.name
  """)
  ```
- **For `@OneToMany` fetches, use `JOIN FETCH` or `@EntityGraph`.** Default to LAZY everywhere; opt into eager via the query.
- **`@Modifying(clearAutomatically = true)`** on every bulk UPDATE/DELETE. *Why:* L1 cache holds stale data otherwise.

### Writes

- **Never set entity IDs via reflection.** No `Field.setAccessible(true)`. Use builder or constructor.
- **Never construct detached entities with `new User().setId(...)` to satisfy a `@ManyToOne`.** Use `repository.getReferenceById(id)` if you only need the FK, or `findById(id).orElseThrow()` if you need the full entity.
- **`@Builder.Default OffsetDateTime createdAt = OffsetDateTime.now()` is BANNED.** This sets the timestamp at build time, not persist time, and gave us hours of "why is this invoice expiring early" debugging. Use `@CreatedDate` from `BaseEntity` instead.
- **Concurrent webhook / write race?** Use `SELECT ... FOR UPDATE` via Spring Data `@Lock(LockModeType.PESSIMISTIC_WRITE)` or a Redis distributed lock keyed on the natural identifier (e.g. `transactionReference`). Don't rely on optimistic `status == X` checks.

### Audit & deletion

- **Soft-delete on Application, Booking, Lease, Message, Conversation, Notification.** `deletedAt = now()`, never `repository.delete()`. *Why:* Dispute resolution and GDPR right-to-erasure both need a trail.
- **Notifications expire after 90 days** via a paginated scheduled job (see §10).

### Slow query detection

- Production PostgreSQL: `log_min_duration_statement = 200`.
- Dev: `spring.jpa.properties.hibernate.generate_statistics = true` — log a warning whenever a request issues > 30 queries.

---

## 8. Caching with Redis (Actually Used This Time)

Backroom declared Redis as a dependency and never put a `@Cacheable` on anything. Habitat uses Redis from day one for:

| Use | TTL | Notes |
|---|---|---|
| `@Cacheable("areas")` / `@Cacheable("amenities")` | 24h | Slow-changing reference data |
| Rate-limit buckets | sliding | Bucket4j-Redis (see §10) |
| OAuth2 exchange codes | 30s | Single-use, single-instance not allowed |
| JWT `jti` blocklist | per-token | TTL = remaining token lifetime |
| Distributed locks | per-job | ShedLock (see §10) |
| Slow-changing aggregates (e.g. community member counts) | 60s | Eviction on write — `@CacheEvict` |

**`spring.cache.type = redis`** in `application.yml`. **`RedisCacheConfiguration` per cache** with explicit TTL. No infinite caches.

---

## 9. External HTTP — Every Call Is Wrapped

Backroom had `RestTemplate` with no timeout and no circuit breaker on payment calls — a hung Ozow connection blocked a servlet thread indefinitely. Habitat:

- **Every outbound HTTP client has explicit timeouts.** Connect 3s, read 10s.
- **Every outbound call goes through Resilience4j:**
  ```java
  @CircuitBreaker(name = "ozow", fallbackMethod = "ozowFallback")
  @Retry(name = "ozow")        // maxAttempts=2, exponential backoff
  @TimeLimiter(name = "ozow")  // hard 12s ceiling
  ```
- **Webhook payloads are persisted before processing** to a `webhook_events` table (raw JSON, source IP, received_at). Lets us replay disputed transactions and debug provider-side bugs.
- **Webhook idempotency is enforced at the service layer** via a unique `external_id` on `webhook_events`. Re-deliveries are no-ops.

---

## 10. Background Jobs & Rate Limiting

Multi-replica from day one.

- **Every `@Scheduled` method is `@SchedulerLock(...)`-protected** by ShedLock-Redis. `lockAtMostFor` is just longer than the expected runtime. *Why:* Otherwise the same job runs on every replica → duplicate notifications, racing status transitions.
- **Scheduled jobs process in pages of 200.** Never `findAllByStatus(...)` and iterate. *Why:* A long maintenance window could produce thousands of expired invoices and exhaust the heap.
- **Rate limiting uses Bucket4j + Redis** (`bucket4j-redis` artifact), not in-memory. *Why:* Otherwise users bypass limits by hitting a different replica.
- **Rate-limit bucket cache is bounded.** Caffeine `expireAfterAccess(1h)` even if we lose the Redis layer briefly.

---

## 11. File Storage

- **`StorageService` interface** with `LocalStorageService` (dev) and `S3StorageService` (prod/staging) implementations. Pick via `app.storage.type`.
- **MIME validation via Apache Tika** on every upload (see §6).
- **Filename is regenerated** server-side as `{uuid}.{ext}`. Original client-supplied filename is stored separately for display only.
- **Sensitive directories require auth** (see §6). Public assets (property hero photos) can be CDN-fronted.

---

## 12. DTOs & API Contracts

- **Never expose entities on the wire.** Always a DTO. Compile-time guarantee via ArchUnit: nothing in `controller/` returns a type from `entity/`.
- **Request DTOs and Response DTOs are separate types.** Even if they have the same fields right now. *Why:* They will diverge.
- **All requests use Jakarta validation annotations** (`@NotBlank`, `@Email`, `@Size`, custom validators). The `GlobalExceptionHandler` translates `MethodArgumentNotValidException` to `ApiError` with `errors: [{field, message}]` and HTTP 422.
- **API versioning is path-based** (`/api/v1/...`). Breaking changes require `/v2` + a deprecation header on `v1` for ≥3 months before removal.
- **`Content-Type: application/json` only.** No XML, no form-encoded except OAuth2 callback. Reject everything else at the filter chain.

---

## 13. Logging

- **JSON-structured logs in every profile except local-dev.** `logstash-logback-encoder`. Mandatory MDC fields: `requestId`, `userId` (when authenticated), `traceId`, `spanId`.
- **`INFO` for state-changing operations** with the entity UUID. `DEBUG` for read paths. `WARN` for recoverable client errors (validation, 4xx). `ERROR` only for genuine server failures.
- **`logging.level.com.habitat = INFO`** in prod. Never DEBUG.
- **No PII in log messages** (see §6). Reviewer sanity check: would you put this log line in a screenshot you'd share publicly?

---

## 14. Observability

- **`RequestIdFilter`** runs first in the filter chain. `MDC.put("requestId", UUID.randomUUID())`. Echoed as `X-Request-Id` response header and included in every `ApiError`. *Why:* Support tickets like "I got an error at 14:32" become unsolvable without this.
- **Micrometer + OpenTelemetry** wired at boot. W3C TraceContext headers propagated through every outbound call.
- **Custom `HealthIndicator` beans** for: storage writable, Redis reachable, Ozow API reachable. Not just DB.
- **Actuator endpoints on a separate management port** (`management.server.port=8081`) — internal-only in prod. Public on dev for convenience.
- **Coverage XML, SpotBugs, PMD findings push to SonarQube** on every CI run. PR blocks on new Critical/High issues.

---

## 15. Configuration

- **`application-prod.yml` exists from day one.** Never let prod inherit dev defaults. Fail-fast on missing env vars (`@PostConstruct` validation).
- **No secrets in `application.yml`.** Env vars via `${VAR}` with no inline default. Bootstrap parameter store / Doppler / 1Password Secrets Automation in prod.
- **Spring profiles:** `local`, `dev`, `staging`, `prod`. Test code uses `test` (Testcontainers).
- **Hikari `max-pool-size` is environment-tuned** — formula `(core_count * 2) + effective_spindle_count`. Documented in `application-prod.yml`.

---

## 16. Testing

Backroom's testing baseline (850 tests, ArchUnit, JaCoCo, SpotBugs, PMD on every `mvn verify`) is the minimum. Add:

- **Every new service method needs a unit test.** Mockito for dependencies.
- **Every new controller endpoint needs an MVC test** (`@WebMvcTest`).
- **Cross-layer flows go in `integration/`** using Testcontainers — real PostgreSQL, real Redis. Run on every PR.
- **`mvn verify` runs the full ArchUnit suite.** Failing layer/naming/exception/import rules fail the build.
- **No `@DirtiesContext`.** If a test pollutes the context, fix the test. *Why:* It silently halves CI throughput.
- **Test names are sentences:** `void rejects_oauth_login_when_redirect_uri_not_allowlisted()`. *Why:* Tests double as documentation.
- **PR cannot regress test count.** Tracking gate in CI.

---

## 17. The Mandatory Change Workflow

Same shape as backroom — proven to work. Every change follows this:

1. **Plan.** Read the build-order item, identify files to touch, confirm no existing service/utility covers this. Run the pre-flight checklist below.
2. **Implement** within agreed scope only.
3. **Build.** `mvn compile`. Fix every error.
4. **Test & scan.** `mvn verify`. ArchUnit + SpotBugs + PMD must be clean. JaCoCo coverage ≥ 80% on touched files.
5. **Postman.** Update `habitat-api/postman/` for added/changed endpoints.
6. **Docs.** Update `build-order.md`, `README.md` if external surface changed, this file if a *new* standard emerges.
7. **Sanity.** `mvn verify sonar:sonar -Dsonar.token=$SONAR_TOKEN`. Restart the server. Smoke-test the touched flow.

### Pre-flight checklist

Answer every question **before** writing code:

| # | Question | Required action |
|---|---|---|
| 1 | New `toResponse()` loop? | Batch-load or `JOIN FETCH`. No per-item queries. |
| 2 | New `@Scheduled` method? | Add `@SchedulerLock`. |
| 3 | New file upload? | Use `StorageService` + Tika magic-byte validation. |
| 4 | Duplicate `hasRole()` logic? | Reuse `SecurityUtils`. |
| 5 | New entity? | Extends `BaseEntity`. All enums `@Enumerated(STRING)`. Soft-delete column. |
| 6 | Throwing an exception? | Custom hierarchy only. No bare `RuntimeException`. |
| 7 | In-memory state / cache? | Bounded (Caffeine) or Redis-backed. |
| 8 | New endpoint? | Explicit `@PreAuthorize` or `permitAll()`. |
| 9 | New Flyway migration? | New file. Never edit applied versions. |
| 10 | New string literal? | Lives in the right constants class. |
| 11 | New endpoint? | In the controller that owns the URL prefix. |
| 12 | New outbound HTTP call? | `@CircuitBreaker` + `@Retry` + timeout. |
| 13 | New PII anywhere near a log statement? | Use UUID instead. |

### Hard prohibitions (pre-commit hook enforces)

- ❌ `throw new RuntimeException(...)` in production code.
- ❌ Tokens in URL query parameters.
- ❌ Editing an applied Flyway migration.
- ❌ Hardcoded user-facing strings in controllers/services/security/filters.
- ❌ `@Enumerated` without `EnumType.STRING`.
- ❌ `file.getContentType()` as the only file validation.
- ❌ Manual `Authorization: Bearer` header construction (use `authInterceptor`).

---

## 18. What We Will NOT Carry Over From Backroom

These are intentional differences. If you find yourself replicating one of them, stop.

- **No `@Scheduled` without ShedLock.** Backroom shipped with bare `@Scheduled`; we paid for it in duplicate-notification bugs.
- **No in-memory rate limiting.** Backroom's `ConcurrentHashMap<String, Bucket>` was per-replica and unbounded. Skip the intermediate step.
- **No "hard delete now, soft delete later".** Soft delete fields ship in `BaseEntity` from commit 1.
- **No DEBUG logging in any non-local profile.** Backroom shipped `logging.level.com.backroom=DEBUG` to prod once.
- **No raw `RestTemplate` without timeouts.** Backroom's payment integration hung on a single bad Ozow call.
- **No "we'll add the security headers later".** They're in `SecurityConfig` on day one — see §6.
- **No `@Builder.Default` on timestamp fields.** Always `@CreatedDate` / `@LastModifiedDate`.
- **No Flyway `validate-on-migrate: false`.** Drift between envs is not negotiable.
- **No reflection-based ID setting.** Use builders or constructors.
- **No primitive `NotificationMessages.format(...)` string templating.** Use Thymeleaf with typed model objects.
- **No `application.yml` with secrets.** Env vars only, fail-fast at startup.

---

## 19. Quality Gates

These run on every PR. Red gate blocks the merge.

- ✅ `mvn verify` is green (ArchUnit, JaCoCo, SpotBugs, PMD, unit + integration tests).
- ✅ Coverage on touched files ≥ 80%.
- ✅ SonarQube: zero new Critical or High issues.
- ✅ No new pre-commit hook violations.
- ✅ Postman collection updated if API surface changed.
- ✅ `build-order.md` and any other relevant doc updated.

---

## 20. Open Questions / Decisions Pending

These are flagged for design before the first sprint:

- **Email delivery provider** — Resend vs SendGrid vs SES. Lean Resend for SA developer ergonomics.
- **PDF generation** — Backroom used HTML strings; habitat should adopt a proper template engine (likely Thymeleaf → Flying Saucer or jsreports).
- **Event-driven extensions** — Outbox pattern + Postgres LISTEN/NOTIFY, or pull in Kafka now? Lean outbox for v1, Kafka when we have a second consumer.
- **Multi-tenancy** — habitat is single-tenant SaaS for now; if we go multi-tenant, schema-per-tenant vs row-level. Decide before any tenant-specific feature lands.

---

*Every decision above traces to a specific incident or piece of backroom-api tech debt. If you disagree with one, raise the discussion — but the burden of proof is on the change.*
