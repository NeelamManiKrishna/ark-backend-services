# ARK Backend Plan

Standalone execution plan for one Claude Code session. Open the session in `D:\ARK\Development\ark-backend-services` and give it this file. Work phase by phase, tick the boxes, and stop for review after each phase.

**Why:** the review in `D:\ARK\Development\docs\ARK_Review_and_Plan.md` found security holes, data bugs, duplicated screens and schema weaknesses. Parents will later see student metrics in a mobile app, so sign-in follows the shared contract in Part A, which the frontend session uses too. Do not change Part A on one side only.

Assumptions: a future `PARENT` role stored as a normal `User` linked to students; the 4 current roles stay; mobile uses the same `/api/v1` REST API.

---

## PART A: Shared contract (goes into BOTH files)

### A1. Auth rules (web and future mobile use the same ones)
- **Bearer tokens in the JSON body**, not cookies, so the web app and a mobile app work the same way. Web XSS risk is reduced with a CSP header and no `dangerouslySetInnerHTML`.
- `POST /api/v1/auth/login` body `{email, password, deviceId?, deviceName?}` returns `{accessToken, refreshToken, expiresIn, user:{id,name,email,role,organizationId,branchId}}`.
- `POST /auth/refresh` body `{refreshToken}` returns a new pair. **Refresh tokens rotate**: each is single-use. Reusing an old one revokes that whole device session.
- `POST /auth/logout` `{refreshToken}` returns 204 (revokes that device). `POST /auth/logout-all` revokes every device.
- `GET /auth/me` returns the user plus their **scope** (`{role, organizationId, branchIds[], studentIds[]}`). The frontend and mobile app use it as the single source of "who am I and what can I see".
- `POST /auth/change-password` `{currentPassword,newPassword}`; success revokes all other sessions.
- `POST /auth/register` is **removed**. Accounts are created only by admins (`POST /organizations/{o}/users`).
- Access token: short and configurable (default 30 min). Refresh token: configurable (default 7 days web, 30 days when `deviceId` is sent).
- JWT claims: `sub` (userId), `role`, `org`, `branch`, `ver` (user token version, bumps on password change/disable), `typ`.
- Roles are an **extensible set**: `SUPER_ADMIN, ORG_ADMIN, ADMIN, USER` today; `PARENT` later. No code may assume exactly four.
- Access decisions always use one object, **AccessScope** (`role, orgId, branchIds, studentIds`). Parents later get `studentIds` from a parent-student link, with no change to the rule engine.

### A2. Error body (stable, so web and mobile can branch on `error`)
`{error:"CODE", message:"human text", timestamp, path, fieldErrors?:[{field,message}]}`
Codes: `INVALID_CREDENTIALS`(401), `TOKEN_EXPIRED`(401), `TOKEN_REVOKED`(401), `ACCOUNT_LOCKED`(403), `ORG_SUSPENDED`(403), `FORBIDDEN`(403), `NOT_FOUND`(404), `DUPLICATE`(409), `CONFLICT`(409), `VALIDATION_FAILED`(422), `BAD_REQUEST`(400).

### A3. API conventions
- Lists: `GET ...?page=0&size=20&sort=field,asc&q=text`, response `{content, page:{size,number,totalElements,totalPages}}`. `size` max 100. The new `q` param searches by name/email/id fields for that resource.
- Everything stays under `/api/v1`. OpenAPI (`/v3/api-docs`) is the source of truth and is what mobile clients will be generated from.
- Reports return JSON; CSV export via `?format=csv` on the same endpoint.
- New endpoints needed by the frontend: `/auth/me`, `/auth/logout`, `/auth/logout-all`, `/auth/change-password`, `q` on list endpoints, academic-years and subjects lists, `PUT /organizations/{o}` for ORG_ADMIN (own org), `GET /organizations/{o}/audit-logs` for ORG_ADMIN, report endpoints.

### A4. Role rules (what the UI must mirror, backend enforces)
| Role | Scope | Read | Write |
|---|---|---|---|
| SUPER_ADMIN | all orgs | everything | orgs, all |
| ORG_ADMIN | own org, all branches | everything in org | everything in org except creating/deleting the org |
| ADMIN | own org, **own branch only** | own branch | academic records in own branch; creates USER accounts in own branch only |
| USER | own org, **own branch only** | own branch (govt ID masked) | nothing |
| PARENT (later) | linked students only | their children's metrics | nothing |

### A5. Order of work and sync points
1. Backend Phase 0 and frontend Phase 0 can start together (no dependency).
2. **Sync point 1:** backend Phase 1 publishes the A1/A2 endpoints. Frontend Phase 1 can build against the contract with a mock until then.
3. **Sync point 2:** backend Phase 3 adds `q`, academic-years, subjects. Frontend Phase 4 uses them.
4. Backend finishes Phase 2 and 3 while frontend does Phase 4. Docs (Phase 5) last.

---

## PART B: BACKEND PLAN  (repo: `D:\ARK\Development\ark-backend-services`)

**Read first:** this repo's `CLAUDE.md`, `docs/architecture.md`, `docs/dev-log.md`; the review at `../docs/ARK_Review_and_Plan.md`. Package root: `src/main/java/com/app/ark_backend_services/`. Keep the existing conventions (CreateXxxRequest / UpdateXxxRequest / XxxResponse.from, `AuditLogService.log` on every write, org-scoped finders). Add a `docs/dev-log.md` entry per phase. Do not commit unless asked.

### Phase 0: Safety net
- [x] `.gitignore` (add `target/`, `.env`, IDE files). Fill `README.md` (run, Mongo, seed, Swagger).
- [x] Move Mongo credentials and the JWT secret out of `application.yaml` to env vars (`MONGO_URI`, `JWT_SECRET`, required in non-dev profile). Add `application-dev.yaml`.
- [x] Add tests with `spring-security-test` that pin current behaviour (login, org check) so later changes are checked. Use an embedded or Testcontainers Mongo so `./mvnw test` no longer needs a manual Docker DB.
- [x] Decide `@Transactional`: confirm whether a `MongoTransactionManager` exists (none found). If not, either enable it with a replica set for dev/prod or remove the annotation and make deletes/promotion ordered and idempotent. Record the decision in `dev-log.md`.
- [x] Add GitHub Actions workflow: `./mvnw test`.

### Phase 1: Security and login (follow Part A exactly)
Files: `service/AuthService.java`, `controller/AuthController.java`, `security/JwtUtil.java`, `security/JwtAuthenticationFilter.java`, `security/CurrentUser.java`, `config/SecurityConfig.java`, `service/UserService.java`, `model/User.java`, new `model/RefreshSession.java` + repository.
- [x] Delete `/auth/register`. Seed/dev accounts come from `test-data.js`.
- [x] Login: unknown email/wrong password → 401 `INVALID_CREDENTIALS` (never reveal which); audit `LOGIN_FAILED` for unknown emails too; failed-attempt counter on `User` with timed lockout.
- [x] Block login and requests when the user is not ACTIVE, the org is SUSPENDED/INACTIVE, or the branch is INACTIVE (check in the JWT filter once, cache per request).
- [x] `RefreshSession` collection: `{userId, deviceId, tokenHash, family, expiresAt, revokedAt}`. Rotate on refresh; reuse of a rotated token revokes the family. Add logout, logout-all, `/auth/me`, change-password. `User.tokenVersion` bumps on password change/disable and is checked from the `ver` claim.
- [x] `AccessScope` + `AccessPolicy` in `security/` (replace scattered `validateOrgAccess`): `assertOrg(orgId)`, `assertBranch(branchId)`, `assertCanWrite(scope)`. Roles stay an enum with a `scopeFor(User)` method so `PARENT` can be added later.
- [x] Apply `AccessPolicy` in every service. ADMIN/USER: branch must match own branch on all reads and writes (students, faculty, classes, enrollments, assignments, exams, results, promotion, bulk import, dashboards). USER: all writes denied; mask `govtIdNumber` in responses.
- [x] Fix org-unscoped lookups: `StudentEnrollmentService.getActiveEnrollment`, `FacultyAssignmentService` (`findByFacultyId*`, class `findById`), `FacultyPerformanceService` class lookups. Use `findByIdAndOrganizationId` and verify branch.
- [x] Fix path-id bugs: assignment get/update/delete honour `branchId`; `updateExamResult` checks `examId` matches; `getResultsBySubject` filters by `subjectId`.
- [x] User management rules in `UserService`: cannot edit/delete yourself, a peer, or the last ORG_ADMIN; ADMIN creates USER only in own branch and cannot list ORG_ADMINs; validate new `branchId` on update.
- [x] ORG_ADMIN may `PUT /organizations/{own id}` (name, address, contacts, logo; not status) and read own org audit logs.
- [x] Bulk-import samples restricted to write roles; ADMIN import limited to own branch.
- [x] Encrypt `govtIdNumber` (AES-GCM, key from `FIELD_ENCRYPTION_KEY`) via a Spring Data converter; one-off migration in Phase 3.
- [x] `GlobalExceptionHandler`: implement the Part A2 body and codes; handle `HttpMessageNotReadableException`, type mismatch, missing params, `IllegalStateException`→409.
- [x] Add CORS config from env (allowed origins list) so mobile/web hosts can be added without code changes.
**Acceptance:** `./mvnw test` includes tests: anonymous register → 404/401; ADMIN branch A → 403 on branch B read and write; USER write → 403; cross-org id → 404; wrong password → 401; suspended org cannot log in; refresh reuse revokes session; logout makes the refresh token fail.

**Status (2026-11-14): Phase 0 and Phase 1 done, awaiting review.** Notes: tests are acceptance tests written after the change (`SecurityAcceptanceTest`, 17 cases) rather than tests pinning the old behaviour; the repo has no Maven wrapper, so CI and docs use `mvn`; `@Transactional` was removed (no replica set), cascades already delete children first and are re-runnable; the org dashboard is now SUPER_ADMIN/ORG_ADMIN only (ADMIN uses the branch dashboard); refresh tokens are opaque (hashed in `refresh_sessions`), only access tokens are JWTs; `govtIdNumber` is encrypted on write and legacy plaintext still reads (migration is Phase 3).

### Phase 2: Data bugs
Files: `service/BulkImportService.java`, `PromotionService.java`, `ExaminationService.java`, `FacultyPerformanceService.java`, `StudentEnrollmentService.java`, `AcademicClassService.java`, `AuditLogService.java`, `AuditLogController.java`.
- [x] Bulk student import: validate the class (exists, same branch, valid year) **before** adding to the batch; keep rows and enrollments in one list of pairs; per-row errors with row numbers; catch duplicate-key per batch; no partial counts.
- [x] Promotion: validate overrides (enum, `@Valid` nested); reject PROMOTE in terminal class; reject if source class already COMPLETED or target year is not later/`YYYY-YYYY`; skip students already enrolled in target year; held-back students get a new enrollment in the same class for the next year and the old one closes with `HELD_BACK`; audit auto-created classes and per-student changes; batch the result queries to remove N+1.
- [x] Marks entry: validate against the enrollment for the **exam's academic year**, not the current ACTIVE one.
- [x] Recompute grade/status of existing results when `maxMarks`/`passingMarks` change; guard `calculateGrade` for `maxMarks == 0`.
- [x] Faculty performance: null-safe marks, exclude ABSENT from averages, count distinct students, include class-teacher assignments with no subject, aggregate in fewer queries.
- [x] Class delete: block if any enrollment (active or history) or assignment exists, or cascade-clean exam subjects; stop leaving orphans.
- [x] Branch change on Student/Faculty: reject if active enrollment/assignment exists; verify the new branch exists.
- [x] Closing an enrollment with GRADUATED/DROPPED/TRANSFERRED updates `Student.status`.
- [x] Add missing deletes/actions: exam result delete, class-progression delete, enrollment reopen (only if latest and no newer enrollment).
- [x] Audit: log cascade deletes (count per child type), log after the write succeeds, use `CREATE` on first progression save, add `before`/`after`/`changedFields`, fix the audit filter chain so `organizationId + action + entityType + userId` combine.
- [x] Paginate the unpaged loads (`getActiveByFaculty`, enrollment history, student results); fix `getAllByClass` to paginate in the database and honour `sort`.
**Acceptance:** one test per bug above; promotion run twice does not duplicate; a bad CSV row never changes other rows.

**Status (2026-11-14): Phase 2 done, awaiting review.** Covered by `DataBugsAcceptanceTest` (9 cases). Notes: faculty performance now excludes ABSENT/WITHHELD and null marks, counts distinct students, counts class-teacher-only assignments, and fetches results with one `IN` query per class (class and exam lookups per class are still separate queries, not one aggregate); the "unpaged" list endpoints (`active assignments`, `enrollment history`, `student results`) keep their list response shape but are capped at 200 rows so the frontend does not break; `before`/`after`/`changedFields` are recorded for updates of students, faculty, users, organizations, branches and classes (government ID and password are never copied); cascade deletes log per-collection counts in one DELETE entry; promotion `studentOverrides[].action` is now an enum (`PROMOTE`, `HOLD_BACK`, `GRADUATE`) and the summary has a new `skipped` count.

### Phase 3: Database cleanup (+ search + reports)
Files: models under `model/`, `config/MongoConfig.java`, new `migration/` package, `test-data.js`.
- [x] Remove `Student.classId` and its repository methods/DTO fields.
- [x] `ClassProgression.sequence[]` stores `classId` (+ display order); validate unique order and exactly one terminal; migration maps names to ids per branch.
- [x] New collections `academic_years` (label `YYYY-YYYY`, start, end, current flag, org) and `subjects` (org, code, name); services accept ids, validate `YYYY-YYYY`; keep the old text fields readable during migration.
- [x] Email: add unique index on `email`, change lookups consistently; migration detects duplicates first.
- [x] Drop copied `branchId` from `ExamResult` and `FacultyAssignment` (derive from class) after code no longer reads it. Single shared `GovtIdType` and gender enums.
- [x] Indexes: students `{org,branch}`,`{org,status}`; faculty `{org,branch}`,`{org,department}`; exam_results `{studentId}`,`{exam,class}`; exam_subjects `{org}`; audit `{org,action}`,`{performedBy}`; remove redundant exam index; set `auto-index-creation` explicitly.
- [x] Migrations run through a `MigrationRunner` (`--dry-run` prints a report, real run is recorded in a `migrations` collection so it runs once). Includes govt-ID encryption.
- [x] `q` search param on students, faculty, users, classes, branches (indexed prefix/regex on name, email, arkId, roll/employee id).
- [x] Reports: `GET /organizations/{o}/reports/{enrollment|performance}` (only these two for now: **enrollment summary** and **exam performance**); add `?format=csv`.
- [x] Rewrite `test-data.js` as the only seed (fix `ARK-ASN-`/`ARK-EXM-` prefixes, real counts, FINAL-exam data so promotion preview works, a few `PARENT`-ready fields untouched). Delete `docs/test-data.js`.
**Acceptance:** dry-run report reviewed; run on a copy of the DB; app starts; tests pass; a student list query with `q` uses an index.

**Status (2026-11-14): Phase 3 mostly done, three items deliberately open, awaiting review.** Covered by `Phase3AcceptanceTest` (6 cases).
- Done: `Student.classId` removed (create requests still accept `classId` for the first enrollment; a migration unsets the legacy field); `users.email` globally unique (index created at startup without blocking boot, duplicates reported by migration 002); indexes added (students, faculty, exam results, exam subjects, audit logs) and `auto-index-creation` set explicitly; one shared `GovtIdType`; `MigrationRunner` (`--migrate.dry-run`, `--migrate`, recorded in `migrations`, each runs once; includes government ID encryption); `q` search on students, faculty, users, classes, branches (case-insensitive prefix, literal not regex, always inside the caller's branch scope); the two reports with `?format=csv`; seed script rewritten and `docs/test-data.js` deleted.
- **Open: `ClassProgression.sequence[]` storing `classId`.** Levels are class names that repeat every year and section; a class id would pin a level to one year's instance and break the following year. Needs your decision (suggestion: keep names, add a validation that every name exists as a class in the branch).
- **Open (partial): `academic_years` and `subjects`.** The collections and CRUD/list endpoints exist, labels are validated everywhere `YYYY-YYYY` is accepted, but records still store the label/subject name as text; switching them to ids would change every existing API payload, so it should be done together with the frontend.
- **Open: dropping the copied `branchId` from `ExamResult`/`FacultyAssignment`, and gender enum.** The cascade deletes and branch-scoped queries still read `branchId` on both; removing it needs those rewritten first. Gender is free text in existing data. The "redundant exam index" was not identified unambiguously, so no index was removed.

### Phase 5 (backend part): Docs and quality
- [x] Rewrite `docs/architecture.md` from the code (all controllers, roles per endpoint, cascades). Merge `promotion-api.md` and `enrollment-assignment-api.md` into one verified doc; remove endpoints that do not exist. Update `CLAUDE.md` (auth, AccessPolicy, migrations).
- [x] Every documented endpoint must exist in `/v3/api-docs`.

### Mobile-readiness checklist (no mobile work now)
- [x] Tokens, refresh, device sessions and error codes follow Part A. [ ] `AccessScope` supports `studentIds`. [ ] All list endpoints paginate and search. [ ] OpenAPI is accurate and includes auth. [ ] Nothing relies on cookies or CSRF.
**Later plan (not now):** `PARENT` role, `Guardian`/parent-student link, `GET /me/children`, parent-safe student summary and report-card endpoints, push-token registration.

**Status (2026-11-14): Phase 5 done.** `docs/architecture.md` is rewritten from the code (its endpoint reference is generated from the live `/v3/api-docs` plus `@PreAuthorize`); `docs/api-guide.md` replaces `promotion-api.md` and `enrollment-assignment-api.md` (removed endpoints and fields that never existed: `/students/{id}/enrollments`, promotion history, `currentEnrollment`, class-teacher endpoint, `PromotionRecord`); `CLAUDE.md` updated; `ApiDocsConsistencyTest` enforces that docs and `/v3/api-docs` agree. Mobile-readiness: lists paginate and search except three capped arrays (student active assignments, enrollment history, student exam results), and the Open items of Phase 3 remain.

### Backend verification
`./mvnw test`; start with seed; use Swagger as each seeded role (password `Password@123`); run migration dry-run then real on a DB copy; compare `/v3/api-docs` with `docs/architecture.md`.

### Decisions on the three open Phase 3 items (2026-11-14)
- **Class progression:** levels stay class **names** (an id would pin a level to one year's class). Saving now requires display orders 1..n without gaps, unique names and exactly one terminal level that is the last one; the response lists `classesNotCreatedYet` (informational, because promotion creates the next class on demand, so requiring the class to exist would block the normal setup flow).
- **Catalog ids (additive):** create requests for classes, enrollments, exams, exam subjects, faculty assignments, the student's initial enrollment and promotion accept `academicYearId` / `subjectId` instead of (or together with) the text; both are stored and returned; text-only requests still work and pick up the id when the catalog has the entry; disagreeing id and text is a 400. Migration 004 backfills existing records.
- **Copied `branchId` and gender:** `branchId` stays (one indexed query for scoping and cascades); a test asserts it equals the class's branch and migration 006 repairs any mismatch. Gender is validated to `MALE`/`FEMALE`/`OTHER` (case-insensitive input, stored upper-case); migration 005 normalizes existing values and reports unmappable ones.

