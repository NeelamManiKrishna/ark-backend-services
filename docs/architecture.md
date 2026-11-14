# Architecture

How the backend is built and what it exposes. Request and response details for clients are in `api-guide.md`; the history of decisions is in `dev-log.md`. The endpoint reference at the end is generated from `/v3/api-docs` and the `@PreAuthorize` annotations, so it matches the code.

## Stack and layout
Spring Boot 4.0.3, Java 17, MongoDB (Spring Data), Spring Security with JWT (JJWT), springdoc-openapi, Lombok. Base package `com.app.ark_backend_services`.

```
controller/  REST endpoints (@RestController), role rules via @PreAuthorize
dto/         Create/Update requests and Response objects (never entities)
service/     business rules; every method starts with AccessPolicy checks
repository/  MongoRepository interfaces, tenant-scoped finders
model/       @Document entities
security/    JwtUtil, JwtAuthenticationFilter, AccessPolicy/AccessScope, CurrentUser, FieldEncryption
exception/   ApiException, ErrorBody/ErrorCodes, GlobalExceptionHandler
migration/   MigrationRunner (one-off data migrations)
config/      SecurityConfig, CorsConfig, MongoConfig, OpenApiConfig, WebConfig
util/        ArkIdGenerator, AcademicYears, AuditSnapshot
```

Conventions: DTOs separate from entities, `XxxResponse.from(entity)`, partial updates (null = unchanged), `arkId` human-readable ids (`ARK-ORG-`, `ARK-BR-`, `ARK-STU-`, `ARK-FAC-`, `ARK-EXM-`, `ARK-ENR-`, `ARK-ASN-` + 8 characters), `@EnableMongoAuditing` for `createdAt`/`updatedAt`, API under `/api/v1`.

## Configuration
Environment variables (no defaults outside the `dev` profile): `MONGO_URI`, `JWT_SECRET` (32+ characters), `FIELD_ENCRYPTION_KEY` (base64, 32 bytes), `CORS_ALLOWED_ORIGINS`; optional `JWT_ACCESS_EXPIRATION_MS`, `JWT_REFRESH_EXPIRATION_MS`, `JWT_DEVICE_REFRESH_EXPIRATION_MS`, `MAX_FAILED_ATTEMPTS`, `LOCKOUT_MINUTES`. `application-dev.yaml` holds local values. MongoDB is standalone in development, so there are **no multi-document transactions**: cascades delete children before parents and are safe to re-run.

## Multi-tenancy and access control
- Every tenant document stores `organizationId`; tenant resources live under `/organizations/{organizationId}/...`.
- `security/AccessPolicy` is the only place that decides access, using the caller's `AccessScope(role, organizationId, branchIds, studentIds, allBranches)`:
  `assertOrg` (SUPER_ADMIN passes anywhere, others must match their organization), `assertBranch` (ADMIN and USER are limited to their own branch), `restrictBranch` (narrows list queries), `assertCanWrite` (USER is read-only).
- Repository lookups are always `findByIdAndOrganizationId...`; an id from another organization is a 404.
- Roles `SUPER_ADMIN > ORG_ADMIN > ADMIN > USER`; the design allows adding `PARENT` (scope through `studentIds`) without changing the rules. User management: nobody edits themselves or an equal/higher role, the last ORG_ADMIN cannot be removed, ADMIN only creates and sees `USER`/`ADMIN` accounts in their own branch.
- Government ID numbers are AES-GCM encrypted at rest (`@ValueConverter`), masked for `USER`.

## Authentication
- Login issues a 30-minute JWT (claims `sub, role, org, branch, ver, typ`) and an opaque refresh token. Refresh tokens are stored hashed in `refresh_sessions`, are single-use, rotate on every refresh, and belong to a family (one per device login); reuse revokes the family. TTL index on `expiresAt`.
- `JwtAuthenticationFilter` loads the user on each request and rejects the token when the user is inactive or locked, `ver` differs from `User.tokenVersion`, or the user's organization/branch is not ACTIVE. The reason becomes the error code (`TOKEN_EXPIRED`, `TOKEN_REVOKED`, `ACCOUNT_LOCKED`, `ORG_SUSPENDED`).
- `tokenVersion` is bumped on password, role, branch and disable changes and on logout-all.
- Five failed logins lock the account for 15 minutes. Unknown emails and wrong passwords return the same 401 and both are audited.
- Public endpoints: login, refresh, logout, Swagger/OpenAPI, `/actuator/health`. No cookies, no CSRF.

## Collections
| Collection | Purpose | Notable indexes |
|---|---|---|
| `organizations`, `branches` | tenants and locations | org name unique; `{org,name}` unique per branch |
| `users` | accounts (4 roles) | `email` unique (created at startup) |
| `refresh_sessions` | device sessions | `tokenHash` unique, `family`, TTL on `expiresAt` |
| `students`, `faculty` | permanent person records | `arkId` unique, `{org,branch}`, `{org,status}` / `{org,department}` |
| `academic_classes` | class per branch, name, section, year | `{org,branch,name,section,year}` unique |
| `student_enrollments` | student to class per year (history = promotion trail) | `{student,year}` unique, `{org,class,status}`, `{student,status}`, `{org,branch,year}` |
| `faculty_assignments` | faculty to class/subject per year | `arkId` unique |
| `class_progressions` | ordered class names per branch, one terminal | one per branch |
| `examinations`, `exam_subjects`, `exam_results` | exams, subjects per class, marks | `{exam,subject,class}` unique, `{subject,student}` unique, `{student}`, `{exam,class}` |
| `academic_years`, `subjects` | per-organization reference catalogs | `{org,label}` / `{org,code}` unique |
| `audit_logs` | audit trail | `{org,timestamp}`, `{org,action}`, `{performedBy}`, `{entityType,entityId}` |
| `migrations` | applied one-off migrations | `_id` = migration id |

`Student.classId` no longer exists; enrollments are the source of truth. `ExamResult` and `FacultyAssignment` deliberately keep a copied `branchId` (one indexed query for branch scoping and cascades); it always equals the class's branch (tested, repaired by migration 006). Records carry both the academic year / subject text and the catalog id (`academicYearId`, `subjectId`), resolved by `CatalogResolver`. Gender is `MALE`/`FEMALE`/`OTHER` (`util/Genders`).

## Cascades (done in services, children first)
- Organization: academic years, subjects, users, enrollments, assignments, progressions, exam results, exam subjects, exams, faculty, students, classes, branches.
- Branch: users, exam results, exam subjects, enrollments, assignments, progression, exams, faculty, students, classes.
- Student: exam results, enrollments. Faculty: assignments. Exam: results and subjects. Exam subject: results.
- A class cannot be deleted while it has enrollments (any status), faculty assignments or exam subjects.

## Audit trail
Every create, update, delete, login, failed login and logout is written to `audit_logs` after the change succeeded: action, entity, organization, who (id, email, role), details, timestamp. Updates of students, faculty, users, organizations, branches and classes add `before`, `after` and `changedFields` (sensitive fields excluded). Cascade deletes record per-collection counts.

## Errors
One body everywhere (including filter-level 401/403): `{error, message, timestamp, path, fieldErrors?}` with codes `INVALID_CREDENTIALS, UNAUTHORIZED, TOKEN_EXPIRED, TOKEN_REVOKED, ACCOUNT_LOCKED, ORG_SUSPENDED, FORBIDDEN, NOT_FOUND, DUPLICATE, CONFLICT, VALIDATION_FAILED, BAD_REQUEST, INTERNAL_ERROR` (see `api-guide.md`).

## Search, reports, migrations
- `q` on list endpoints goes through `SearchService` (anchored, escaped, case-insensitive prefix, combined with the caller's tenant and branch criteria).
- `ReportService` computes the enrollment and exam-performance reports with Mongo aggregation; JSON or CSV.
- `migration/MigrationRunner`: start with `--migrate.dry-run` or `--migrate`; migrations never run implicitly and each runs once (001 encrypt government IDs, 002 verify unique emails, 003 remove legacy `students.classId`, 004 backfill catalog ids, 005 normalize gender, 006 repair copied `branchId`).

## Tests
`AbstractMongoIntegrationTest` starts MongoDB with Testcontainers. `SecurityAcceptanceTest` (auth contract, isolation, role rules), `DataBugsAcceptanceTest` (import, promotion, exams, enrollments, audit), `Phase3AcceptanceTest` (search, catalogs, reports, indexes, migrations), `OpenItemsAcceptanceTest` (catalog ids, progression rules, gender, copied branchId), `ApiDocsConsistencyTest` (docs match `/v3/api-docs`).

## Endpoint reference
Generated from the running application. "Access" is the `@PreAuthorize` role list; "any authenticated user" still obeys organization and branch scoping, and `USER` can only read. 95 operations.

### Academic Classes

| Method | Path | Access | Summary |
|---|---|---|---|
| GET | `/api/v1/organizations/{organizationId}/branches/{branchId}/classes` | any authenticated user | List classes by branch |
| POST | `/api/v1/organizations/{organizationId}/branches/{branchId}/classes` | SUPER_ADMIN, ORG_ADMIN, ADMIN | Create class |
| GET | `/api/v1/organizations/{organizationId}/branches/{branchId}/classes/{classId}` | any authenticated user | Get class by ID |
| PUT | `/api/v1/organizations/{organizationId}/branches/{branchId}/classes/{classId}` | SUPER_ADMIN, ORG_ADMIN, ADMIN | Update class |
| DELETE | `/api/v1/organizations/{organizationId}/branches/{branchId}/classes/{classId}` | SUPER_ADMIN, ORG_ADMIN, ADMIN | Delete class |

### Audit Logs

| Method | Path | Access | Summary |
|---|---|---|---|
| GET | `/api/v1/audit-logs` | SUPER_ADMIN | List audit logs |
| GET | `/api/v1/organizations/{organizationId}/audit-logs` | SUPER_ADMIN, ORG_ADMIN | List an organization's audit logs |

### Authentication

| Method | Path | Access | Summary |
|---|---|---|---|
| POST | `/api/v1/auth/change-password` | any authenticated user | Change password |
| POST | `/api/v1/auth/login` | public | Login |
| POST | `/api/v1/auth/logout` | public | Logout this device |
| POST | `/api/v1/auth/logout-all` | any authenticated user | Logout all devices |
| GET | `/api/v1/auth/me` | any authenticated user | Current user and access scope |
| POST | `/api/v1/auth/refresh` | public | Refresh tokens |

### Branches

| Method | Path | Access | Summary |
|---|---|---|---|
| GET | `/api/v1/organizations/{organizationId}/branches` | any authenticated user | List branches by organization |
| POST | `/api/v1/organizations/{organizationId}/branches` | SUPER_ADMIN, ORG_ADMIN | Create branch |
| GET | `/api/v1/organizations/{organizationId}/branches/{branchId}` | any authenticated user | Get branch by ID |
| PUT | `/api/v1/organizations/{organizationId}/branches/{branchId}` | SUPER_ADMIN, ORG_ADMIN | Update branch |
| DELETE | `/api/v1/organizations/{organizationId}/branches/{branchId}` | SUPER_ADMIN, ORG_ADMIN | Delete branch |

### Bulk Import

| Method | Path | Access | Summary |
|---|---|---|---|
| POST | `/api/v1/organizations/{organizationId}/bulk-import/academic-classes` | SUPER_ADMIN, ORG_ADMIN, ADMIN | Bulk import academic classes |
| POST | `/api/v1/organizations/{organizationId}/bulk-import/branches` | SUPER_ADMIN, ORG_ADMIN | Bulk import branches |
| POST | `/api/v1/organizations/{organizationId}/bulk-import/faculty` | SUPER_ADMIN, ORG_ADMIN, ADMIN | Bulk import faculty |
| GET | `/api/v1/organizations/{organizationId}/bulk-import/samples/{entityType}` | SUPER_ADMIN, ORG_ADMIN, ADMIN | Download sample CSV |
| POST | `/api/v1/organizations/{organizationId}/bulk-import/students` | SUPER_ADMIN, ORG_ADMIN, ADMIN | Bulk import students |

### Catalogs

| Method | Path | Access | Summary |
|---|---|---|---|
| GET | `/api/v1/organizations/{organizationId}/academic-years` | any authenticated user | List academic years |
| POST | `/api/v1/organizations/{organizationId}/academic-years` | SUPER_ADMIN, ORG_ADMIN | Create academic year |
| GET | `/api/v1/organizations/{organizationId}/academic-years/{id}` | any authenticated user | Get academic year |
| PUT | `/api/v1/organizations/{organizationId}/academic-years/{id}` | SUPER_ADMIN, ORG_ADMIN | Update academic year |
| DELETE | `/api/v1/organizations/{organizationId}/academic-years/{id}` | SUPER_ADMIN, ORG_ADMIN | Delete academic year |
| GET | `/api/v1/organizations/{organizationId}/subjects` | any authenticated user | List subjects |
| POST | `/api/v1/organizations/{organizationId}/subjects` | SUPER_ADMIN, ORG_ADMIN | Create subject |
| PUT | `/api/v1/organizations/{organizationId}/subjects/{id}` | SUPER_ADMIN, ORG_ADMIN | Update subject |
| DELETE | `/api/v1/organizations/{organizationId}/subjects/{id}` | SUPER_ADMIN, ORG_ADMIN | Delete subject |

### Class Progression

| Method | Path | Access | Summary |
|---|---|---|---|
| GET | `/api/v1/organizations/{organizationId}/branches/{branchId}/class-progression` | SUPER_ADMIN, ORG_ADMIN, ADMIN | Get class progression |
| PUT | `/api/v1/organizations/{organizationId}/branches/{branchId}/class-progression` | SUPER_ADMIN, ORG_ADMIN | Set class progression |
| DELETE | `/api/v1/organizations/{organizationId}/branches/{branchId}/class-progression` | SUPER_ADMIN, ORG_ADMIN | Delete class progression |

### Dashboard

| Method | Path | Access | Summary |
|---|---|---|---|
| GET | `/api/v1/dashboard/organization/{organizationId}` | SUPER_ADMIN, ORG_ADMIN | Organization dashboard |
| GET | `/api/v1/dashboard/organization/{organizationId}/branch/{branchId}` | SUPER_ADMIN, ORG_ADMIN, ADMIN | Branch dashboard |
| GET | `/api/v1/dashboard/platform` | SUPER_ADMIN | Platform dashboard |

### Examinations

| Method | Path | Access | Summary |
|---|---|---|---|
| GET | `/api/v1/organizations/{organizationId}/branches/{branchId}/examinations` | any authenticated user | List examinations by branch |
| POST | `/api/v1/organizations/{organizationId}/branches/{branchId}/examinations` | SUPER_ADMIN, ORG_ADMIN, ADMIN | Create examination |
| GET | `/api/v1/organizations/{organizationId}/examinations/{examId}` | any authenticated user | Get examination by ID |
| PUT | `/api/v1/organizations/{organizationId}/examinations/{examId}` | SUPER_ADMIN, ORG_ADMIN, ADMIN | Update examination |
| DELETE | `/api/v1/organizations/{organizationId}/examinations/{examId}` | SUPER_ADMIN, ORG_ADMIN, ADMIN | Delete examination |
| GET | `/api/v1/organizations/{organizationId}/examinations/{examId}/classes/{classId}/results` | any authenticated user | Get class results |
| PUT | `/api/v1/organizations/{organizationId}/examinations/{examId}/results/{resultId}` | SUPER_ADMIN, ORG_ADMIN, ADMIN | Update exam result |
| DELETE | `/api/v1/organizations/{organizationId}/examinations/{examId}/results/{resultId}` | SUPER_ADMIN, ORG_ADMIN, ADMIN | Delete exam result |
| GET | `/api/v1/organizations/{organizationId}/examinations/{examId}/students/{studentId}/results` | any authenticated user | Get student's exam results |
| GET | `/api/v1/organizations/{organizationId}/examinations/{examId}/subjects` | any authenticated user | List exam subjects |
| POST | `/api/v1/organizations/{organizationId}/examinations/{examId}/subjects` | SUPER_ADMIN, ORG_ADMIN, ADMIN | Create exam subject |
| PUT | `/api/v1/organizations/{organizationId}/examinations/{examId}/subjects/{subjectId}` | SUPER_ADMIN, ORG_ADMIN, ADMIN | Update exam subject |
| DELETE | `/api/v1/organizations/{organizationId}/examinations/{examId}/subjects/{subjectId}` | SUPER_ADMIN, ORG_ADMIN, ADMIN | Delete exam subject |
| GET | `/api/v1/organizations/{organizationId}/examinations/{examId}/subjects/{subjectId}/results` | any authenticated user | List results by subject |
| POST | `/api/v1/organizations/{organizationId}/examinations/{examId}/subjects/{subjectId}/results` | SUPER_ADMIN, ORG_ADMIN, ADMIN | Create exam result |

### Faculty

| Method | Path | Access | Summary |
|---|---|---|---|
| GET | `/api/v1/organizations/{organizationId}/faculty` | any authenticated user | List faculty |
| POST | `/api/v1/organizations/{organizationId}/faculty` | SUPER_ADMIN, ORG_ADMIN, ADMIN | Create faculty |
| GET | `/api/v1/organizations/{organizationId}/faculty/{facultyId}` | any authenticated user | Get faculty by ID |
| PUT | `/api/v1/organizations/{organizationId}/faculty/{facultyId}` | SUPER_ADMIN, ORG_ADMIN, ADMIN | Update faculty |
| DELETE | `/api/v1/organizations/{organizationId}/faculty/{facultyId}` | SUPER_ADMIN, ORG_ADMIN, ADMIN | Delete faculty |

### Faculty Assignments

| Method | Path | Access | Summary |
|---|---|---|---|
| GET | `/api/v1/organizations/{organizationId}/branches/{branchId}/faculty-assignments` | any authenticated user | Get assignments by branch and year |
| POST | `/api/v1/organizations/{organizationId}/branches/{branchId}/faculty-assignments` | SUPER_ADMIN, ORG_ADMIN, ADMIN | Create faculty assignment |
| GET | `/api/v1/organizations/{organizationId}/branches/{branchId}/faculty-assignments/class/{classId}` | any authenticated user | Get assignments by class |
| GET | `/api/v1/organizations/{organizationId}/branches/{branchId}/faculty-assignments/faculty/{facultyId}` | any authenticated user | Get all assignments for faculty |
| GET | `/api/v1/organizations/{organizationId}/branches/{branchId}/faculty-assignments/faculty/{facultyId}/active` | any authenticated user | Get active assignments for faculty |
| GET | `/api/v1/organizations/{organizationId}/branches/{branchId}/faculty-assignments/{assignmentId}` | any authenticated user | Get assignment by ID |
| PUT | `/api/v1/organizations/{organizationId}/branches/{branchId}/faculty-assignments/{assignmentId}` | SUPER_ADMIN, ORG_ADMIN, ADMIN | Update assignment |
| DELETE | `/api/v1/organizations/{organizationId}/branches/{branchId}/faculty-assignments/{assignmentId}` | SUPER_ADMIN, ORG_ADMIN, ADMIN | Delete assignment |

### Faculty Performance

| Method | Path | Access | Summary |
|---|---|---|---|
| GET | `/api/v1/organizations/{organizationId}/faculty/{facultyId}/performance` | SUPER_ADMIN, ORG_ADMIN, ADMIN | Overall faculty performance |
| GET | `/api/v1/organizations/{organizationId}/faculty/{facultyId}/performance/class/{classId}` | SUPER_ADMIN, ORG_ADMIN, ADMIN | Faculty performance in a class |
| GET | `/api/v1/organizations/{organizationId}/faculty/{facultyId}/performance/subject/{subjectName}` | SUPER_ADMIN, ORG_ADMIN, ADMIN | Faculty performance across classes for a subject |

### Organizations

| Method | Path | Access | Summary |
|---|---|---|---|
| GET | `/api/v1/organizations` | SUPER_ADMIN | List all organizations |
| POST | `/api/v1/organizations` | SUPER_ADMIN | Create organization |
| GET | `/api/v1/organizations/{id}` | any authenticated user | Get organization by ID |
| PUT | `/api/v1/organizations/{id}` | SUPER_ADMIN, ORG_ADMIN | Update organization |
| DELETE | `/api/v1/organizations/{id}` | SUPER_ADMIN | Delete organization |

### Promotions

| Method | Path | Access | Summary |
|---|---|---|---|
| POST | `/api/v1/organizations/{organizationId}/branches/{branchId}/promotions/execute` | SUPER_ADMIN, ORG_ADMIN, ADMIN | Execute promotion |
| GET | `/api/v1/organizations/{organizationId}/branches/{branchId}/promotions/preview` | SUPER_ADMIN, ORG_ADMIN, ADMIN | Preview promotion |

### Reports

| Method | Path | Access | Summary |
|---|---|---|---|
| GET | `/api/v1/organizations/{organizationId}/reports/enrollment` | SUPER_ADMIN, ORG_ADMIN, ADMIN | Enrollment summary |
| GET | `/api/v1/organizations/{organizationId}/reports/performance` | SUPER_ADMIN, ORG_ADMIN, ADMIN | Exam performance |

### Student Enrollments

| Method | Path | Access | Summary |
|---|---|---|---|
| GET | `/api/v1/organizations/{organizationId}/branches/{branchId}/enrollments` | any authenticated user | Get enrollments by branch and year |
| POST | `/api/v1/organizations/{organizationId}/branches/{branchId}/enrollments` | SUPER_ADMIN, ORG_ADMIN, ADMIN | Enroll student in a class |
| GET | `/api/v1/organizations/{organizationId}/branches/{branchId}/enrollments/class/{classId}` | any authenticated user | Get enrollments by class |
| GET | `/api/v1/organizations/{organizationId}/branches/{branchId}/enrollments/student/{studentId}/active` | any authenticated user | Get active enrollment |
| GET | `/api/v1/organizations/{organizationId}/branches/{branchId}/enrollments/student/{studentId}/history` | any authenticated user | Get enrollment history |
| PUT | `/api/v1/organizations/{organizationId}/branches/{branchId}/enrollments/{enrollmentId}/close` | SUPER_ADMIN, ORG_ADMIN, ADMIN | Close enrollment |
| PUT | `/api/v1/organizations/{organizationId}/branches/{branchId}/enrollments/{enrollmentId}/reopen` | SUPER_ADMIN, ORG_ADMIN, ADMIN | Reopen enrollment |

### Students

| Method | Path | Access | Summary |
|---|---|---|---|
| GET | `/api/v1/organizations/{organizationId}/students` | any authenticated user | List students |
| POST | `/api/v1/organizations/{organizationId}/students` | SUPER_ADMIN, ORG_ADMIN, ADMIN | Create student |
| GET | `/api/v1/organizations/{organizationId}/students/{studentId}` | any authenticated user | Get student by ID |
| PUT | `/api/v1/organizations/{organizationId}/students/{studentId}` | SUPER_ADMIN, ORG_ADMIN, ADMIN | Update student |
| DELETE | `/api/v1/organizations/{organizationId}/students/{studentId}` | SUPER_ADMIN, ORG_ADMIN, ADMIN | Delete student |

### Users

| Method | Path | Access | Summary |
|---|---|---|---|
| GET | `/api/v1/organizations/{organizationId}/users` | SUPER_ADMIN, ORG_ADMIN, ADMIN | List users |
| POST | `/api/v1/organizations/{organizationId}/users` | SUPER_ADMIN, ORG_ADMIN, ADMIN | Create user |
| GET | `/api/v1/organizations/{organizationId}/users/{userId}` | SUPER_ADMIN, ORG_ADMIN, ADMIN | Get user by ID |
| PUT | `/api/v1/organizations/{organizationId}/users/{userId}` | SUPER_ADMIN, ORG_ADMIN | Update user |
| DELETE | `/api/v1/organizations/{organizationId}/users/{userId}` | SUPER_ADMIN, ORG_ADMIN | Delete user |

