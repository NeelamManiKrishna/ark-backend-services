# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**Project ARK (Academic Record Keeper)** — backend for a multi-tenant academic records platform. Spring Boot 4.0.3, Java 17, MongoDB (Spring Data), Spring Security + JWT (JJWT 0.12.6), Lombok, springdoc-openapi, commons-csv. The companion frontend lives in `../ark-web-frontend` (Vite/React dev servers on :5173/:3000 are allowed by CORS).

Requirements come from a BRD with IDs like BR-ORG-xxx, BR-ROLE-xxx, BR-AUTH-xxx, BR-REC-xxx (historically at `../ark_brd_doc.txt`; not currently checked out next to this repo).

## Commands

There is no Maven wrapper in the repo; use a global `mvn`. Tests need Docker running (Testcontainers starts MongoDB).

```bash
mvn clean package -DskipTests                       # build
mvn spring-boot:run -Dspring-boot.run.profiles=dev  # run (needs MongoDB, see below)
mvn test                                            # all tests
mvn test -Dtest=ClassName#method                    # single test
```

Integration tests extend `AbstractMongoIntegrationTest` (Testcontainers Mongo, `test` profile). `SecurityAcceptanceTest` covers the auth contract and tenant/branch isolation.

### Local environment
- MongoDB runs in a Docker container named `mongodb` with root user `admin` / `secret`; the app uses database `ark_web` (`spring.mongodb.uri` in `application.yaml`).
- Config comes from env vars (`MONGO_URI`, `JWT_SECRET`, `FIELD_ENCRYPTION_KEY`, `CORS_ALLOWED_ORIGINS`); there are no defaults outside the `dev` profile (`application-dev.yaml`).
- Seed data: `src/main/resources/test-data.js` (drops and repopulates collections; all seeded users have password `Password@123`):
  ```bash
  docker cp src/main/resources/test-data.js mongodb:/tmp/test-data.js
  docker exec mongodb mongosh -u admin -p secret --authenticationDatabase admin ark_web /tmp/test-data.js
  ```
- Swagger UI: `/swagger-ui/index.html`.

## Spring Boot 4 gotchas
- MongoDB config is under `spring.mongodb.*`, **not** `spring.data.mongodb.*` — the old prefix is silently ignored.
- Use `spring-boot-starter-data-mongodb` (the plain `-mongodb` starter is driver-only, no repositories).
- Pages are serialized via `@EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO)` on the main class.

## Architecture

Classic layering under `com.app.ark_backend_services`: `controller → service → repository → model`, with `dto/`, `config/`, `security/`, `exception/`, `util/`. See `docs/architecture.md` for the design and endpoint catalogue, `docs/api-guide.md` for the client contract, and `docs/dev-log.md` for the history of design decisions.

### Multi-tenancy (enforced in the service layer)
- Almost every resource is nested under `/api/v1/organizations/{organizationId}/...` and every tenant document stores `organizationId`.
- Each service method starts with `AccessPolicy.assertOrg(organizationId)` (SUPER_ADMIN passes for any org; others must match their own org) and throws `AccessDeniedException`.
- Repository lookups are always scoped, e.g. `findByIdAndOrganizationId(id, orgId)` — never fetch by bare id for tenant data.
- Uniqueness is scoped to the parent via `@CompoundIndex` (e.g. branch name unique per org).

### Security / RBAC
- Auth contract (see `docs/ARK_Backend_Plan.md` Part A): login returns a 30-min JWT access token plus an opaque, single-use refresh token (stored hashed in `refresh_sessions`, rotated on every refresh, reuse revokes the device session). No self-registration; accounts are created by admins.
- `JwtAuthenticationFilter` loads the `User` (principal) on every request and rejects it if inactive, locked, token `ver` differs from `User.tokenVersion`, or its org/branch is not ACTIVE. Bump `tokenVersion` whenever role, branch, status or password change.
- Roles: `SUPER_ADMIN -> ORG_ADMIN -> ADMIN -> USER` (`User.Role`; designed to gain `PARENT` later, never assume four).
- All access decisions go through `security/AccessPolicy` (backed by `AccessScope`): `assertOrg(orgId)` first, then `assertBranch(branchId)` for entity/path branches, `restrictBranch(requested)` for list endpoints (ADMIN/USER are narrowed to their own branch). ADMIN/USER are branch-scoped; USER is read-only and sees `govtIdNumber` masked.
- Coarse rules live in `SecurityConfig` (only login/refresh/logout, swagger and health are public); role rules are `@PreAuthorize` on controller methods.
- `govtIdNumber` is AES-GCM encrypted at rest via `@ValueConverter(EncryptedStringConverter)` (legacy plaintext still reads).
- Errors: `{error: CODE, message, timestamp, path, fieldErrors?}` built by `GlobalExceptionHandler`/`ErrorBody`; throw `ApiException` for coded 401/403 cases.

### Conventions
- DTOs: `CreateXxxRequest`, `UpdateXxxRequest`, `XxxResponse` with a static `XxxResponse.from(entity)` factory.
- Updates are partial: null fields in the update request are skipped.
- Entities have a Mongo `id` plus a human-readable `arkId` from `ArkIdGenerator` (`ARK-ORG-`, `ARK-BR-`, `ARK-STU-`, `ARK-FAC-`, `ARK-EXM-`, `ARK-ENR-`, `ARK-ASN-` + 8-char nanoid).
- Every CREATE/UPDATE/DELETE (and LOGIN/REGISTER) must call `AuditLogService.log(action, entityType, entityId, entityName, organizationId, details)`.
- Errors: throw `ResourceNotFoundException` (404) / `DuplicateResourceException` (409); see Security above for the body shape.
- Deletes cascade manually in services (e.g. org → branches/classes/students/faculty; exam → subjects → results).

### Search, reports, migrations
- List endpoints for students, faculty, users, classes and branches accept `q` (prefix match) through `SearchService`; always pass the tenant/branch criteria as `scope` so search cannot widen access.
- `ReportService` builds the enrollment and exam performance reports with Mongo aggregation; the controller renders JSON or CSV.
- One-off data migrations live in `migration/MigrationRunner` and only run with `--migrate` / `--migrate.dry-run`. Add new ones as `Migration` implementations; each runs once and is recorded in the `migrations` collection.
- Indexes belong to the application (annotations plus `MongoConfig`); do not create conflicting ones in the seed script.

### Domain model notes
- Identity is separated from temporal assignment: `Student` / `Faculty` are permanent; their link to a class is tracked per academic year in `StudentEnrollment` (unique per `studentId + academicYear`) and `FacultyAssignment`. Don't reintroduce a mutable `Student.classId` as the source of truth.
- Promotion flow: `ClassProgression` (per-branch ordered class sequence, with terminal class) → `PromotionService` preview → execute, which closes and creates enrollments. Faculty performance is derived from `FacultyAssignment` + `ExamResult`.
- Examinations: `Examination → ExamSubject → ExamResult`.
- `BulkImportService` handles CSV imports (5MB multipart limit).
- Client-facing contract (auth, errors, enrollments, assignments, performance, promotion, reports): `docs/api-guide.md`.

### Keeping docs current
`docs/architecture.md` (design plus a generated endpoint table with roles), `docs/api-guide.md` and `docs/dev-log.md` (session log) are maintained alongside code changes. `ApiDocsConsistencyTest` fails when an endpoint in `/v3/api-docs` is missing from the architecture table (`| METHOD | \`path\` |` rows) or a documented `/api/v1` path no longer exists, so add a row (and the guide text) whenever you add, move or remove an endpoint.
