# habitat-api — Tech Debt

Living log of known issues to address later. Modelled on `backroom-api/TECH_DEBT.md`.

Severity: 🔴 active bug / correctness · 🟠 architectural debt · 🟡 cosmetic / minor · ✅ resolved.

Each item links back to the equivalent `backroom-api/TECH_DEBT.md` entry where one exists — we shipped the same flaw class without learning from it. Don't repeat.

---

## Executive Summary

**All eight items below were closed by the tech-debt sweep (`389b2b5`, 2026-05-14)** and validated by the BUG-01 tests added 2026-05-26 (`OptimisticLockingTest`, `GlobalExceptionHandlerTest`). The detailed problem write-ups are kept below as institutional memory — they document the lessons that shaped `development-standards.md`. New items are added at the bottom.

| ID | Status | Severity | Area | Title |
|---|---|---|---|---|
| **BUG-01** | ✅ | 🔴 | Concurrency | No optimistic locking on `Application`, `Invoice`, `Lease` |
| **ARCH-01** | ✅ | 🔴 | JPA | `Property.units` cascade fights V22 RESTRICT |
| **ARCH-02** | ✅ | 🟠 | State machine | `ApplicationStatus` transitions scattered across 4 services |
| **ARCH-03** | ✅ | 🟠 | Coupling | Service dependency direction inverted; future slices will worsen |
| **BUG-02** | ✅ | 🟠 | Audit | Leases + invoices have no snapshot fields; mutate when upstream changes |
| **BUG-03** | ✅ | 🟠 | Audit | `applications.tenant_id` / `unit_id` still `ON DELETE CASCADE` |
| **ARCH-04** | ✅ | 🟡 | Audit | `applications.decided_by` has no name snapshot |
| **ARCH-05** | ✅ | 🟡 | Convention | No code-level enforcement of "always soft-delete" |

**Open items:** _none_

---

## 🔴 Active correctness bugs

### BUG-01 — No optimistic locking on stateful entities
**Files:** `entity/base/BaseEntity.java`, `Application.java`, `Invoice.java`, `Lease.java`
**Inherits from:** `backroom-api` `PAY-04` (Concurrent Webhook Retry Race Condition)
**Already documented in:** `development-standards.md` line 274 — *"Concurrent webhook / write race? Use `@Lock(LockModeType.PESSIMISTIC_WRITE)` … Don't rely on optimistic `status == X` checks."*

**Problem.** No entity has a `@Version` field. Multiple state-mutating endpoints rely on read-then-status-check guards that are racy under concurrent calls:

- `ApplicationService.review()` — two concurrent landlord-review calls both pass the reviewable check, both write `decided_by` / `decided_at`. Last writer wins; one reviewer's note is silently overwritten.
- `InvoiceService.pay()` — two concurrent pay calls both pass the `PENDING` check; the idempotency guard on the invoice itself saves us, but the parent application's status transition happens twice and the lease-generation side-effect can race.
- `LeaseService.sign()` — a tenant double-click sends two `/sign` requests; both observe `tenantSignedAt = null`, both pass the "already signed" check, both write. No data corruption (last write wins for the timestamp) but no clean 409 either.

**Fix.** Two options, in order of effort:

1. Cheapest: add `@Version Long version` to `BaseEntity`. Hibernate auto-bumps it on every write and throws `OptimisticLockException` on stale writes. The `GlobalExceptionHandler` maps that to HTTP 409.
2. Stronger (for high-contention writes): `@Lock(LockModeType.PESSIMISTIC_WRITE)` on the repository read for `review` / `pay` / `sign`. Pessimistic for *payment* paths is safer per `development-standards.md` §274.

---

### ARCH-01 — `Property.units` JPA cascade conflicts with V22 RESTRICT
**Files:** `entity/Property.java` (line 114), `entity/Unit.java`

**Problem.** `Property.units` is declared:
```java
@OneToMany(mappedBy = "property", cascade = CascadeType.ALL, orphanRemoval = true)
```
After V22, `units` is `RESTRICT`-FK'd from `leases` and `invoices`. The Hibernate cascade and the DB constraint now disagree:

- Hard-deleting a `Property` cascades a `DELETE` to each `Unit` → the first leased unit hits `leases_unit_fk RESTRICT` → exception, full transaction rollback.
- `property.getUnits().clear()` triggers `orphanRemoval` → same DELETE, same crash.

Dormant today (codebase only soft-deletes), but a future purge / GDPR job will blow up.

**Fix.** Narrow the cascade to `{PERSIST, MERGE}` only on `Property.units`. Drop `orphanRemoval`. Same for `Property.images` and `Unit.images` for consistency (those don't have RESTRICT downstream, but the looser cascade is good hygiene).

---

## 🟠 Architectural debt

### ARCH-02 — `ApplicationStatus` state machine is scattered across 4 services
**Files:** `ApplicationService`, `InvoiceService`, `LeaseService`

**Problem.** No single place enumerates legal `ApplicationStatus` transitions. Today:

| Transition | Owner |
|---|---|
| `AWAITING_DOCUMENTS → DOCUMENTS_SUBMITTED` | `ApplicationService.uploadDocument` |
| `* → APPROVED → INVOICE_SENT` | `ApplicationService.review` |
| `INVOICE_SENT → DEPOSIT_PAID` | `InvoiceService.pay` |
| `DEPOSIT_PAID → LEASE_PENDING_SIGNATURES` | `LeaseService.issueForPaidApplication` |
| `LEASE_PENDING_SIGNATURES → COMPLETED` *(Slice 4)* | TBD |

Adding a new transition means trusting yourself to remember every other site. The compiler can't help.

**Fix.** `ApplicationStatus.canTransitionTo(next)` helper, or a tiny FSM (hand-rolled or Spring StateMachine). All transition writes go through one helper that throws `ConflictException` on illegal moves. Backed by unit tests that enumerate the legal transition matrix.

---

### ARCH-03 — Service dependency direction points backward
**Files:** `ApplicationService → InvoiceService → LeaseService`

**Problem.** Each new slice's "next-step" side effect lives in the *upstream* service:

```
ApplicationService → InvoiceService → LeaseService → ???
     (slice 1)         (slice 2)        (slice 3)     (slice 4)
```

So `ApplicationService` now knows about invoices, `InvoiceService` knows about leases. Slice 4's move-in completion will force `LeaseService` to know about `unit.status = OCCUPIED` (a `UnitService` concern) and the application → COMPLETED transition (an `ApplicationService` concern). The coupling chain grows with every slice.

**Fix.** Domain events. Spring's `@TransactionalEventListener` fires after commit, which keeps this clean:

```java
// LeaseService.sign() — after status flip
events.publishEvent(new LeaseSignedEvent(lease.getId()));

// new MoveInOrchestrator listens
@TransactionalEventListener(phase = AFTER_COMMIT)
void onLeaseSigned(LeaseSignedEvent e) { ... }
```

Best done **before Slice 4** so the move-in flow gets events for free instead of cementing the direct-call style.

---

### BUG-02 — No snapshot fields on durable records
**Files:** `entity/Lease.java`, `entity/Invoice.java`
**Inherits from:** general accounting / legal-record hygiene; not explicit in backroom but `RentalInvoice` has the same flaw.

**Problem.** Leases and invoices re-read the *current* values of upstream entities on every render:

- Tenant changes their surname → every old lease shows the new surname.
- Unit rent is updated → past invoices keep showing the right amount (it's frozen on the invoice), but the lease "monthly rent" comes from the lease's own `monthly_rent` column ✅ — that part is fine.
- Property address changes (renumbering, suburb rezoning) → every old lease shows the new address.

A 2024 lease should display 2024-era data, not whatever the row currently holds.

**Fix.** Add snapshot columns on issuance, never updated:
- `lease.tenant_name_snapshot`, `lease.landlord_name_snapshot`, `lease.property_address_snapshot`, `lease.unit_title_snapshot`
- `invoice.tenant_name_snapshot`, `invoice.property_address_snapshot`

DTO reads prefer the snapshot, fall back to the live record. New migration `V25__lease_invoice_snapshots.sql` plus service write-time additions.

---

### BUG-03 — `applications.tenant_id` / `unit_id` still `ON DELETE CASCADE`
**Files:** `db/migration/V16__create_applications.sql`
**Inherits from:** `backroom-api` `ARCH-02` (No soft deletes — hard deletes break audit trails)

**Problem.** V22 fixed leases + invoices but explicitly deferred the application table. So:

- Hard-deleting a user wipes their entire application history. Fair-housing audits care about rejected applications.
- Hard-deleting a unit wipes every historical application against it.

Same shape of fix as V22 (RESTRICT + nullable trace pointer), smaller surface area since applications don't fan out as much downstream state.

**Fix.** `V25` (or whatever's next) — same template as V22:
```sql
ALTER TABLE applications DROP CONSTRAINT applications_tenant_id_fkey;
ALTER TABLE applications ADD CONSTRAINT applications_tenant_id_fkey
    FOREIGN KEY (tenant_id) REFERENCES users(id) ON DELETE RESTRICT;
-- repeat for unit_id
```

---

## 🟡 Minor / cosmetic

### ARCH-04 — `applications.decided_by` has no name snapshot
**Files:** `entity/Application.java`, `db/migration/V19__application_review_fields.sql`

If a landlord reviews an application then leaves the platform and is hard-deleted, the `decided_by` UUID dangles. Audit trail loses the name.

**Fix.** Add `decided_by_name VARCHAR(200)` snapshot column. Populate in `ApplicationService.review()` from the User entity at decision time.

---

### ARCH-05 — Nothing enforces "always soft-delete, never `entityManager.remove()`"
**Files:** convention only; no code-level guard

**Problem.** All entities use `deleted_at` + `@SQLRestriction`. A future contributor calling `repository.delete(entity)` bypasses the convention and (after V22) will hit `RESTRICT` constraints in surprising places.

**Fix.** Add a line to `development-standards.md` §"Database Rules": *"Never call `repository.delete()` or `entityManager.remove()` on a `BaseEntity`. Always set `deletedAt`."* Consider an ArchUnit rule that forbids the call sites in service packages.

---

## How this list will be worked off

Suggested order, smallest blast radius first:

1. **ARCH-01** — pure Java change to one entity, no migration. Low risk.
2. **BUG-01** — `@Version` on `BaseEntity` + one Hibernate column on each table via a single migration. Cheap, addresses two of the three race classes immediately.
3. **ARCH-03** — domain events refactor. **Do this before Slice 4** or pay the coupling tax forever.
4. **BUG-02** — snapshot columns + write-time population.
5. **BUG-03** — applications FK flip, same shape as V22.
6. **ARCH-02** — FSM helper. Touches every service; do it once the above are in.
7. **ARCH-04**, **ARCH-05** — cleanup pass.
