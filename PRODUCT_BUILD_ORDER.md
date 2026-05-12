# Habitat — Product Build Order

> **Scope:** The order we wire real features end-to-end across `habitat-ui` (React frontend) and `habitat-api` (Spring Boot backend).
> **Companion to:** [`development-standards.md`](./development-standards.md). Every phase below follows those standards by default — this doc only spells out the per-feature delta.
> **Reviewed:** 2026-05-12.

The UI scaffold is currently rich and entirely mock-driven; the API scaffold is sparse but production-shaped. Every phase here delivers a **vertical slice** — UI hitting real API hitting real Postgres — rather than building a layer in each repo and integrating later.

---

## How each phase ships

For every phase below:

**API side** (`habitat-api`)
- Entities + repository + service + controller + DTOs.
- **Tests:** service-level Mockito unit tests · `@WebMvcTest` per endpoint · `@DataJpaTest` only when the repo has non-trivial queries (`@Query`, projections, `@Lock`, `@Modifying`) · one end-to-end `@SpringBootTest` per feature against a Testcontainers Postgres.
- **Coverage gate: ≥ 90% line and branch on touched files** (enforced by JaCoCo `<rule>` block in `pom.xml`).
- Postman collection updated under `postman/`.

**UI side** (`habitat-ui`)
- Replace mock data in the relevant screens with calls into `src/lib/api/<domain>.ts` (typed wrappers around `fetch`).
- **Tests:** Vitest + React Testing Library — at least one render test per screen, one happy-path interaction test per flow, MSW handler covering API success + error + loading states.
- Loading + error states wired through the existing `LoadingState` / `ErrorState` primitives.

**Both**
- A short integration walkthrough in the phase commit message (the curl command + the UI path that exercises it).
- Both repos commit + push at phase boundary. **No commit until the reviewer (you) gives the go-ahead.**

---

## Phase 0 — Test + contract foundations

**Goal:** every later phase can write tests on day one.

**API**
- [ ] JaCoCo `<rule>` block in `pom.xml` failing build on < 90% line / branch coverage of touched packages.
- [ ] First `@DataJpaTest` + `@WebMvcTest` example so the patterns are documented.
- [ ] SpringDoc OpenAPI spec served at `/v3/api-docs` (springdoc-openapi already on the classpath).

**UI**
- [ ] Install **Vitest** + **@testing-library/react** + **@testing-library/jest-dom** + **MSW** (Mock Service Worker).
- [ ] `vitest.config.ts`, `src/test/setup.ts`, `src/test/mswServer.ts`.
- [ ] `src/lib/api/client.ts` — fetch wrapper that auto-injects the `Authorization` bearer, handles 401 → token refresh, surfaces `ApiError` shape unchanged. Tested.
- [ ] `npm run test` + `npm run coverage` scripts in `package.json`.
- [ ] One example test per kind (component test, hook test, MSW-driven integration test).

**Both**
- [ ] CI workflow stub (GitHub Actions) running `mvn verify` on `habitat-api` and `npm test && npm run build` on `habitat-ui`. No deploys yet.

---

## Phase 1 — Real auth end-to-end

**Goal:** the demo users in habitat-ui's `DEMO_USERS` come from a real database. Tokens are issued, persisted, refreshed, and revoked.

**API**
- [ ] `POST /auth/refresh` — verifies a refresh token, issues a new access + refresh pair, blacklists the old refresh `jti` in Redis.
- [ ] `POST /auth/logout` — adds the access token `jti` to the Redis blocklist with TTL = remaining lifetime.
- [ ] `GET /api/v1/users/me` — returns the authenticated user's full profile.
- [ ] `PATCH /api/v1/users/me/active-role` — switches the active role within the user's owned roles.
- [ ] `POST /auth/forgot-password` + `POST /auth/reset-password` (Resend email — see Phase 8).
- [ ] `POST /auth/verify-email` token confirm.
- [ ] `POST /auth/oauth2/exchange` — single-use 30-second opaque code → tokens (replaces token-in-URL).
- [ ] Refresh-token device-fingerprint binding (hashed UA + IP subnet).
- [ ] Seed migration for the four demo users so the UI's role-switcher keeps working.

**UI**
- [ ] `src/lib/api/auth.ts` — `register`, `login`, `refresh`, `logout`, `me`, `switchRole`.
- [ ] `SessionProvider` replaces `signIn(DEMO_USERS.tenant)` with a real `api.auth.login(...)` call. Tokens persisted to localStorage; access token decoded for role + expiry.
- [ ] Background token-refresh task — kicks off 60 s before access token expiry.
- [ ] `Register.tsx`, `Auth.tsx`, `ForgotPassword.tsx`, `OauthCallback.tsx` all wired to real endpoints.
- [ ] `/dev` role-switcher logs in via the API too (uses seeded demo users).
- [ ] 401 from any API call → `client.ts` triggers refresh; if refresh fails, redirect to `/auth`.

**DoD**
- Curl: register → login → /users/me → refresh → logout (verify 401 after).
- UI: sign in as each of the four demo users via `/auth`, switch roles in the workspaces dropdown, sign out clears local state.
- Tests: ≥ 90% on `AuthService`, `JwtService`, every auth controller endpoint, the new `api.auth` UI wrapper, and `SessionProvider`.

---

## Phase 2 — Areas, amenities, and the public catalog

**Goal:** drop-in replacement for the static SA-area arrays in `HeroSearch`, `AreaMultiSelect`, and the various filter chips.

**API**
- [ ] `Area` entity (id, name, slug, city, lat, lng, hierarchy parent). `@Cacheable("areas")` with 24h TTL.
- [ ] `Amenity` entity (id, name, slug, icon). `@Cacheable("amenities")`.
- [ ] `GET /api/v1/areas?city=&q=` + `GET /api/v1/amenities` — public, page-size capped.
- [ ] Seed migration with the full SA area set (JHB suburbs first, then CPT/DBN/PTA).

**UI**
- [ ] `src/lib/api/catalog.ts`.
- [ ] `HeroSearch` typeahead drives off `api.catalog.areas(q)` (debounced).
- [ ] `AreaMultiSelect` on `/communities` reads from the same source.
- [ ] Browse + Wizard amenity chips read from `api.catalog.amenities()`.

**DoD**
- 4 screens (Landing search, Browse filter, Communities Discover area filter, Wizard amenity step) all hit the API.
- ≥ 90% on `AreaService`, `AmenityService`, `api.catalog` wrapper. MSW-tested loading + error fallbacks visible on each.

---

## Phase 3 — Properties + Units (browse path)

**Goal:** `/browse`, `/property?id=…`, `/unit?id=…` render real data.

**API**
- [ ] `RentalProperty` entity (with `landlord` + `manager` semantics from standards §2).
- [ ] `Unit` entity (denormalised `manager` from property).
- [ ] `PropertyPhoto` + `UnitPhoto` entities; storage in `StorageService` (see Phase 7).
- [ ] `GET /api/v1/properties` (filtered, paginated). `GET /api/v1/properties/{id}`. `GET /api/v1/properties/{id}/units`. `GET /api/v1/units/{id}`.
- [ ] `PropertyAccessResolver` (OWNER → AGENT → MANAGER priority).
- [ ] **N+1 fix on the way in:** the property list endpoint returns a `PropertyListProjection` DTO from a single JPQL query — no per-row `toResponse()`.
- [ ] Seed migration with ~30 properties across JHB suburbs.

**UI**
- [ ] `src/lib/api/properties.ts`.
- [ ] `Browse` listings driven by `api.properties.list({ areas, type, minPrice, maxPrice, minBeds, … })`. Existing filter URL params map 1:1.
- [ ] `PropertyDetail` + `Unit` read real data. Save state + active pin persist.
- [ ] `Saved` screen reads from a future `Wishlist` endpoint — for now keep local until Phase 6.

**DoD**
- Empty database renders the empty state on `/browse` (already exists from Phase 11c).
- Loading + error states preview-able via the `?state=` toggle still work.
- ArchUnit: `PropertyController` doesn't call repositories directly.

---

## Phase 4 — Listing wizard (write path)

**Goal:** `Wizard.tsx` actually creates and edits properties.

**API**
- [ ] `POST /api/v1/properties` (create) — accepts the wizard's nested DTO; transactional.
- [ ] `PUT /api/v1/properties/{id}` (full update).
- [ ] `POST /api/v1/properties/{id}/units` + `PUT /api/v1/units/{id}` + `DELETE /api/v1/units/{id}` (soft-delete).
- [ ] `POST /api/v1/properties/{id}/photos` (multipart — see Phase 7).
- [ ] State machine: DRAFT → UNLISTED → LISTED.

**UI**
- [ ] Wizard's "Publish" hits `POST /properties` and routes to `/listing-submitted`.
- [ ] Wizard's `?edit=<id>` pre-fills from `GET /properties/{id}` and submits via `PUT /properties/{id}`.
- [ ] `LandlordProperties` + `Portfolio` tables show real data; Open / Edit buttons unchanged.

**DoD**
- Landlord and agent both create a property + units end-to-end.
- Soft-delete is observable: removing a unit hides it from public `GET /units`, still visible in `?includeDeleted=true` for admins.

---

## Phase 5 — Applications + Viewings

**Goal:** `Apply`, `MyApplications`, `Applicant`, `Viewings`, `BookViewing`, `ViewingAvailability` are real.

**API**
- [ ] `Application` entity + state machine (NEW / VETTING / INTERVIEW / LEASE_READY / DECLINED / WITHDRAWN).
- [ ] `ApplicationDocument` (typed by `DocumentType` enum — STRING-stored).
- [ ] `Booking` entity for viewings + 7 states from the standards crib.
- [ ] `ViewingAvailability` entity (weekly windows + per-property overrides).
- [ ] Application + viewing endpoints under `/applications`, `/bookings`, `/properties/{id}/availability`.
- [ ] Cosigner support on `Application` (per standards §11e — already mocked in the UI).
- [ ] **N+1 fix:** `GET /applications` returns a projection; no per-row scoring queries.

**UI**
- [ ] Apply submits the 5-step wizard; documents upload via `StorageService` (Phase 7).
- [ ] My Applications driven by `GET /applications?tenantId=me`.
- [ ] Applicant detail + pipeline columns wired.
- [ ] Viewings + ViewingAvailability + BookViewing all real.

**DoD**
- A tenant can apply, a landlord/agent can see and progress the application, a viewing can be requested and confirmed — all without touching mock data.

---

## Phase 6 — Lease lifecycle

**Goal:** `Lease`, lease PDF, OTP signing, `LandlordLeases`, lease-end review entry point.

**API**
- [ ] `Lease` entity + state machine + 3 template variants.
- [ ] PDF generation via **Thymeleaf → Flying Saucer** (not raw HTML strings — see standards §18).
- [ ] OTP issuance + verification (Redis-backed, 5-minute TTL).
- [ ] `POST /leases/{id}/sign` + `POST /leases/{id}/countersign`.
- [ ] `MoveOutInspection` + `DepositRefund` entities (the UI already exists at `/move-out` and `/deposit-refund`).
- [ ] PDF download stream uses `StreamingResponseBody` (per standards §JPA-04).

**UI**
- [ ] Lease pre-fill from API, real OTP send + verify, real countersign, real download.
- [ ] LandlordLeases ended-rows show the "Decide refund" CTA wired to a real endpoint.
- [ ] Tenant move-out flow submits via the new endpoint.

**DoD**
- Full lease cycle in one session: tenant signs → landlord countersigns → both download PDFs → lease ends → tenant submits move-out → landlord decides refund.

---

## Phase 7 — File storage + image uploads

**Goal:** photos and documents move off `Photo` placeholders and out of the JVM.

**API**
- [ ] `StorageService` interface + `LocalStorageService` (dev) + `S3StorageService` (staging/prod).
- [ ] Apache Tika magic-byte validation on every upload (per standards §6).
- [ ] Path-traversal guard on download (per standards §6).
- [ ] `GET /files/{path}` — auth + ownership check before streaming.
- [ ] Upload endpoints for `Property`, `Unit`, `Application` documents, user `avatar`.

**UI**
- [ ] Replace `FileUploadZone` mock with real multipart upload + progress.
- [ ] `<Photo>` primitive switches to real image URLs when present, falls back to the existing label placeholder.
- [ ] Avatar upload from `/profile`.

**DoD**
- A property created in Phase 4 can now show real photos in `/browse`, `/property`, the map preview, and the Wizard's edit mode.

---

## Phase 8 — Money: invoices, payments, payouts

**Goal:** the rent flow works end-to-end against Ozow's sandbox.

**API**
- [ ] `RentalInvoice` entity + states (PENDING / PAID / EXPIRED / FAILED).
- [ ] Ozow integration via the new `RestClient` (Phase 0's HTTP standard) wrapped in `@CircuitBreaker + @Retry + @TimeLimiter` (per standards §9).
- [ ] Webhook payloads persisted to `webhook_events` (table already exists in V1) before any state transition.
- [ ] `Statement` + payout job (`@SchedulerLock`-protected per standards §10).
- [ ] `BusinessDayService` — cache the holiday set in `@PostConstruct`; externalise SA public holidays from the YAML (DB-backed).

**UI**
- [ ] `Payment`, `Invoice`, `PaymentResult`, `Failed`, `Statements` all wired.
- [ ] Landlord trust-account view + payout history.

**DoD**
- A tenant pays a rent invoice in Ozow sandbox, the webhook hits the API, the invoice flips to PAID, the landlord's statement updates, the simulated payout fires on T+3.

---

## Phase 9 — Notifications + email delivery

**Goal:** the bell drawer + `/notifications` page show real events; critical events also email out.

**API**
- [ ] `Notification` entity + service.
- [ ] Domain events fire notifications: `APPLICATION_RECEIVED`, `LEASE_READY`, `PAYMENT_RECEIVED`, `PAYMENT_FAILED`, `MANDATE_*`, `VIEWING_*`, `POST_REPLY`, `NEW_FOLLOWER`, etc.
- [ ] Email delivery via **Resend** (per standards §20).
- [ ] Notification archival job (90-day expiry — per standards §7).
- [ ] `POST /notifications/mark-read` + `GET /notifications`.

**UI**
- [ ] `NotificationDrawer` polls or subscribes to a small SSE stream for new events.
- [ ] `/notifications` page reads from the real endpoint, supports filters + bulk read.
- [ ] Outbound email preferences exposed on `/settings`.

**DoD**
- Trigger an application from the tenant side → landlord sees a bell badge within 10 seconds → tapping the row navigates to `/applicant` with the right ID.

---

## Phase 10 — Inbox + messaging

**Goal:** the WhatsApp-Web-style `/inbox` carries real messages.

**API**
- [ ] `Conversation` (unit-scoped, plus DM-style for community admins) + `Message` entities.
- [ ] Polling endpoint first (`GET /conversations/{id}/messages?after=<ts>`), then SSE upgrade.
- [ ] `POST /conversations/{id}/messages` (text + optional attachment).

**UI**
- [ ] `Inbox` thread list + thread pane wired.
- [ ] Right-rail action buttons (Book viewing, Send lease, Open applicant) keep working through the new IDs.
- [ ] Unread counters in nav badge driven from the API.

**DoD**
- Two browsers, two roles, one conversation — messages appear within a couple of seconds.

---

## Phase 11 — Communities + Feed (social)

**Goal:** the most differentiated surface in the product becomes real.

**API**
- [ ] `Community`, `CommunityMember`, `CommunityRole` (ADMIN / MOD / MEMBER) entities.
- [ ] `Post`, `Comment`, `PostBookmark`, `Follow`, `PostLike`, `PostReport` entities.
- [ ] Post tag enum (the 7 we picked: Tip · Question · Looking · Sublet · Heads-up · Classifieds · Review).
- [ ] Feed endpoints: `GET /posts?scope=for_you|following|local`, `POST /posts`, `GET /posts/{id}/comments`.
- [ ] Public-profile endpoint: `GET /users/{id}` returns the social profile shape (no email).
- [ ] Trust & safety: `POST /posts/{id}/report`, `POST /users/{id}/mute`, `POST /users/{id}/block`.
- [ ] **N+1 fix on toResponse:** the Feed projection includes follow + bookmark + like state in one query.

**UI**
- [ ] `Communities` (Feed + Discover + People + Articles) all wired.
- [ ] `PostCard` interactions (like, bookmark, report, mute, block) fire real mutations.
- [ ] `/u/:userId` + `/post/:postId` read real data.

**DoD**
- A landlord posts a Tip → 5 verified tenants see it in their "For you" feed → one replies → the landlord gets a bell notification (Phase 9 hook).

---

## Phase 12 — Agent placement loop + mandates

**Goal:** `JobBoard`, `AgentRequests`, `MyMandates`, `MandateApprovals`, `MyAgency`, `Portfolio` are real.

**API**
- [ ] `RoomRequest` (tenant brief) + `AgentRequest` (proposal).
- [ ] `Mandate` entity + lifecycle (full 3-flow management vs tenant-find vs letting-and-inspections).
- [ ] `Agency` entity + agent ↔ agency association.
- [ ] Agent-on-behalf creation gating on `Wizard?ctx=agent` writes (Phase 4 hook).
- [ ] Mandate approval emails via Phase 9.

**UI**
- [ ] JobBoard reads briefs; agent proposes → tenant accepts → mandate created.
- [ ] Mandate approval flow for landlords.

**DoD**
- End-to-end placement: brief → propose → accept → lease → first invoice → payout. The flow already exists as a UI shell — this phase makes it real.

---

## Phase 13 — Search + cmdk + saved searches with alerts

**Goal:** global search and `Saved` actually work.

**API**
- [ ] `GET /search?q=…&type=…` — multi-entity (properties, communities, users).
- [ ] Postgres full-text search; trigram on `name`/`title` columns.
- [ ] `SavedSearch` entity + `AlertChannel` enum.
- [ ] Daily / Weekly alert scheduler (`@SchedulerLock`-protected) — fires Phase 9 emails for new matches.

**UI**
- [ ] `/cmdk` palette wired to `GET /search`.
- [ ] `Saved` screen reads + writes real saved searches and channel preferences.

---

## Phase 14 — Admin + audit + moderation queue

**Goal:** the `/admin` page is real.

**API**
- [ ] `AuditEvent` entity — append-only log of state-changing operations.
- [ ] `ModerationReport` queue (consumed from Phase 11's post / user reports).
- [ ] `AdminController` with role-gated endpoints (only ADMIN / SUPER_ADMIN).

**UI**
- [ ] Admin queue actions (Approve / Reject / Open) fire real mutations.
- [ ] Audit log viewer.

---

## Phase 15 — Reviews + reputation

**Goal:** end-of-lease review loop closes.

**API**
- [ ] `Review` entity (subject = landlord OR tenant OR area OR agent).
- [ ] Aggregate score endpoints.
- [ ] Gate write to "post lease-end" only.

**UI**
- [ ] `/reviews` (already exists) wired to real submit + listing.
- [ ] Subject's profile (`/u/:id`) shows aggregate score.

---

## Phase 16 — Real map + property geocoding

**Goal:** the MapLibre map (already wired in Phase 11f frontend) shows real, geocoded properties.

**API**
- [ ] Geocoding on property create (via OpenCage or similar — `@CircuitBreaker`-wrapped).
- [ ] `GET /properties?bbox=…&zoom=…` returns simplified pins + clusters server-side.

**UI**
- [ ] `MapPanel` switches from the 6 mock pins to API-driven.

---

## Phase 17 — Production gates

**Goal:** the green / red light for first live customer.

**API**
- [ ] Bucket4j-Redis rate limiting on `/auth/*` and write endpoints.
- [ ] `application-prod.yml` reviewed end-to-end; every env var documented in README.
- [ ] OTel exporter wired against the chosen collector.
- [ ] Custom `HealthIndicator` beans (Postgres, Redis, S3, Ozow, Resend).
- [ ] Load test (k6 or Gatling) at 5× peak expected RPS — fail-fast on regressions.
- [ ] Pen-test pass (or at least an automated OWASP Zap baseline scan).

**UI**
- [ ] Real loading states across every screen — `?state=` previews retired.
- [ ] Error retry buttons everywhere actually retry instead of refreshing.
- [ ] Sentry (or equivalent) wired for runtime errors.
- [ ] Lighthouse + axe accessibility audit ≥ 95.

**DoD**
- All four roles can complete their key journeys against staging without any UI fallback to mock data.
- `mvn verify` + `npm run test && npm run build` green on the latest commit.

---

## Conventions across all phases

- **Vertical slice rule.** A phase is not "done" until both repos can show the slice working in a browser pointing at a local API.
- **Test-then-merge.** UI lands its MSW-driven tests in the same PR as the screen wiring; API lands its Mockito + WebMvc + integration tests in the same PR as the controllers. No "tests will follow" PRs.
- **Coverage gate at 90%** on touched files in both repos.
- **One commit per logical unit.** Phase-level boundary commits use `phase N: <name>` in both repos. Per-feature commits inside a phase use Conventional Commits (`feat(api): …`, `fix(ui): …`, etc.).
- **No `Co-Authored-By: Claude` lines.**
- **Review-gated pushes** (per the new working agreement): I write code + tests, run the build, summarise, and stop. You review the diff, then say "push".

---

## Where each repo's own build-order lives

This file is the **product-level** view across both repos. Each repo keeps its own tactical build-order for things that don't span the boundary:

- `habitat-api/build-order.md` (this repo) — API-only phases (currently Phase 0 done, Phases 1-7 mapped — those will be folded into the phases above as we work through them).
- `habitat-ui/build-order.md` — UI-only phases (currently up to Phase 11, all done).

When a phase here ships, both repo build-orders get their tick.
