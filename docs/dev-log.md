







# Development Log

## Session 1 (2026-03-07)

### 1. Project Init (CLAUDE.md)
- Created CLAUDE.md for the `ark-backend-services` repo with build commands, tech stack, and architecture requirements

### 2. Organization REST API
- Created full layered structure: model, repository, service, controller, DTOs, exceptions
- Model: `Organization` with fields: name (unique), address, contactEmail, contactPhone, website, logoUrl, status (ACTIVE/INACTIVE/SUSPENDED), createdAt, updatedAt
- Endpoints: POST, GET by ID, GET all (paginated), PUT (partial update)
- Exception handling: ResourceNotFoundException (404), DuplicateResourceException (409), GlobalExceptionHandler
- Duplicate name validation on both create and update

### 3. POM Fix: MongoDB Starter
- `spring-boot-starter-mongodb` only includes the driver, not Spring Data
- Changed to `spring-boot-starter-data-mongodb` (and test equivalent) to get MongoRepository, annotations, pagination

### 4. Springdoc OpenAPI
- Added `springdoc-openapi-starter-webmvc-ui` v2.8.6
- Swagger UI at: `/swagger-ui/index.html`

### 5. Validation Starter
- Added `spring-boot-starter-validation` to fix `NoProviderFoundException` for Jakarta Bean Validation

### 6. CORS Configuration
- Created `CorsConfig.java` with `WebMvcConfigurer` allowing localhost:3000 and localhost:5173
- Frontend CORS issue was resolved on frontend side

### 7. MongoDB Auth Fix
- Docker container uses `MONGO_INITDB_ROOT_USERNAME=admin`, `MONGO_INITDB_ROOT_PASSWORD=secret`
- **Key discovery:** Spring Boot 4.x moved MongoDB config from `spring.data.mongodb.*` to `spring.mongodb.*`
- The old `spring.data.mongodb.uri` was silently ignored → app connected without auth → Unauthorized errors
- Fixed by using `spring.mongodb.uri`

### 8. Page Serialization Fix
- Added `@EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO)` to main application class
- Fixes unstable `PageImpl` JSON serialization warning

### 9. MongoDB Auditing
- Created `MongoConfig.java` with `@EnableMongoAuditing` for automatic `@CreatedDate`/`@LastModifiedDate`

### 10. Branch API (Organization Branches/Locations)
- Created full CRUD for branches nested under organizations
- Model: `Branch` with fields: organizationId, name, address, city, state, zipCode, contactEmail, contactPhone, status (ACTIVE/INACTIVE), createdAt, updatedAt
- Compound unique index on `(organizationId, name)` — branch names unique per org
- Endpoints nested: `/api/v1/organizations/{organizationId}/branches`
- All queries scoped by organizationId for data isolation
- Organization existence validated on create/list

### 11. Code Review
- Identified critical issues: no input validation, hardcoded credentials, race condition in duplicate checks, no auth/RBAC, no logging
- Full prioritized list of improvements documented

### 12. Delete APIs
- Added DELETE endpoints for both Organization and Branch
- Organization delete cascade-deletes all its branches via `branchRepository.deleteByOrganizationId(id)`
- Branch delete scoped to organizationId (validates branch belongs to org before deleting)
- Both return 204 No Content

### 13. AcademicClass API
- Model: `AcademicClass` — name, section, academicYear, capacity, description, status (ACTIVE/INACTIVE/COMPLETED)
- Compound unique index on `(organizationId, branchId, name, section)` — class name+section unique per branch
- Nested under branch: `/api/v1/organizations/{orgId}/branches/{branchId}/classes`
- Full CRUD with cascade delete from branch and org

### 14. Student API
- Model: `Student` — rollNumber (unique per org), firstName, lastName, email, phone, dateOfBirth, gender, address, guardian info, enrollmentDate, status (ACTIVE/INACTIVE/GRADUATED/TRANSFERRED/DROPPED)
- Compound unique index on `(organizationId, rollNumber)`
- Scoped under org: `/api/v1/organizations/{orgId}/students`
- GET supports optional query params: `?branchId=` and `?classId=` for filtering
- branchId and classId stored as references on the student (not deeply nested URL)
- Cascade delete from org delete

### 15. Updated Cascade Deletes
- Org delete now: faculty → students → classes → branches → org
- Branch delete now: classes → branch

### 16. Faculty API
- Model: `Faculty` — employeeId (unique per org), firstName, lastName, email, phone, dateOfBirth, gender, address, department, designation, qualifications (List), specializations (List), joiningDate, status (ACTIVE/INACTIVE/ON_LEAVE/RESIGNED/TERMINATED)
- Compound unique index on `(organizationId, employeeId)`
- Scoped under org: `/api/v1/organizations/{orgId}/faculty`
- GET supports optional query params: `?branchId=` and `?department=` for filtering
- Cascade delete from org delete

### 17. Test Data Bulk Insert
- Created `src/main/resources/test-data.js` mongosh script
- Run: `docker cp src/main/resources/test-data.js mongodb:/tmp/test-data.js && docker exec mongodb mongosh -u admin -p secret --authenticationDatabase admin ark_web /tmp/test-data.js`
- Data: 3 orgs, 5 branches, 7 classes, 10 students, 8 faculty
- Orgs: Springfield Academy (IL), Riverside College (TX), Oakwood International (OR)
- Script drops and recreates all collections + indexes
- **Fix:** AcademicClass unique index changed from `(orgId, branchId, name)` to `(orgId, branchId, name, section)` — allows same class name with different sections in same branch
- Updated repository method and service accordingly

## Session 2 (2026-03-08)

### 18. JWT Authentication & RBAC
- Added Spring Security + JJWT 0.12.6 dependencies
- Created `User` model with roles: SUPER_ADMIN, ORG_ADMIN, ADMIN, USER
  - Compound unique index on `(organizationId, email)`, global unique index on `email`
  - Fields: fullName, email, password (BCrypt), role, organizationId, branchId, department, status (ACTIVE/INACTIVE/LOCKED)
- JWT token implementation:
  - Access token (1 hour) and refresh token (7 days)
  - Claims include: userId (subject), email, role, organizationId, token type
  - Secret key configured in `application.yaml` under `app.jwt.*`
- Security components:
  - `JwtAuthenticationFilter` — extracts Bearer token, validates, sets SecurityContext with User principal
  - `AuthEntryPoint` — returns 401 JSON for unauthenticated requests
  - `AccessDeniedHandler` — returns 403 JSON for unauthorized requests
  - `SecurityConfig` — stateless session, CSRF disabled, endpoint-level authorization rules
  - `CurrentUser` utility — static helpers to get current user, check org membership, check Super Admin
- Auth endpoints (`/api/v1/auth`):
  - POST `/register` — creates user with BCrypt password, validates org existence for non-Super Admin roles
  - POST `/login` — validates credentials, returns access + refresh tokens
  - POST `/refresh` — validates refresh token type, returns new token pair
- Authorization rules:
  - `/api/v1/auth/**` — public (no auth required)
  - POST/DELETE `/api/v1/organizations` — Super Admin only
  - All other `/api/**` — authenticated users
  - Swagger UI and actuator health — public
- Updated CorsConfig from `WebMvcConfigurer` to `CorsConfigurationSource` bean (integrates with Security filter chain)
- Updated GlobalExceptionHandler with handlers for: IllegalArgumentException (400), MethodArgumentNotValidException (400), AccessDeniedException (403)
- Added `spring-security-test` dependency for testing

### 19. User Management API
- Created full CRUD for users scoped under organizations: `/api/v1/organizations/{orgId}/users`
- DTOs: CreateUserRequest, UpdateUserRequest, UserResponse
- Repository: added `findByOrganizationIdAndBranchId`, `findByIdAndOrganizationId`
- GET supports optional `?branchId=` filter
- Role creation hierarchy enforced:
  - SUPER_ADMIN → can create any role
  - ORG_ADMIN → can create ADMIN, USER
  - ADMIN → can create USER only
  - USER → cannot create users
- Org access validation via `CurrentUser.belongsToOrg()` — non-Super Admins can only manage users in their own org
- `@PreAuthorize` on controller: create/get requires SUPER_ADMIN/ORG_ADMIN/ADMIN, update/delete requires SUPER_ADMIN/ORG_ADMIN
- Branch validation: if branchId provided, verifies branch exists in the org
- Password BCrypt-encoded on create and update
- Cascade delete: org delete now also deletes all users → `userRepository.deleteByOrganizationId(id)`

### 20. User Seed Data for Auth Testing
- Added 7 test users to `test-data.js` (password for all: `Password@123`)
- BCrypt hash pre-computed and embedded in script
- Users cover all 4 roles across 2 orgs:
  - `superadmin@ark-platform.com` — SUPER_ADMIN (no org)
  - `orgadmin@springfieldacademy.edu` — ORG_ADMIN (Springfield)
  - `admin@springfieldacademy.edu` — ADMIN (Springfield, Main Campus, Science)
  - `user@springfieldacademy.edu` — USER (Springfield, Main Campus, Science)
  - `orgadmin@riversidecollege.edu` — ORG_ADMIN (Riverside)
  - `admin@riversidecollege.edu` — ADMIN (Riverside, Downtown, CS)
  - `user@riversidecollege.edu` — USER (Riverside, Downtown, Math)

### 21. Audit Log System
- Created `AuditLog` model with fields: action, entityType, entityId, entityName, organizationId, performedBy, performedByEmail, performedByRole, details, timestamp
- Actions tracked: CREATE, UPDATE, DELETE, LOGIN, LOGOUT, REGISTER
- Compound indexes: `(organizationId, timestamp desc)` and `(entityType, entityId)`
- `AuditLogService` with two logging methods:
  - `log()` — for CRUD actions, auto-captures current user from SecurityContext
  - `logAuth()` — for auth actions (login/register), takes user info as params since SecurityContext isn't set yet
- `AuditLogController` at `GET /api/v1/audit-logs` — SUPER_ADMIN only
  - Filterable by: `?organizationId=`, `?action=`, `?entityType=`, `?userId=`
  - Paginated, sorted by timestamp descending
- Integrated into all 7 services:
  - AuthService — LOGIN, REGISTER
  - OrganizationService — CREATE, UPDATE, DELETE
  - BranchService — CREATE, UPDATE, DELETE
  - AcademicClassService — CREATE, UPDATE, DELETE
  - StudentService — CREATE, UPDATE, DELETE
  - FacultyService — CREATE, UPDATE, DELETE
  - UserService — CREATE, UPDATE, DELETE

### 22. Dashboard Analytics APIs
- Platform Dashboard (`GET /api/v1/dashboard/platform`) — SUPER_ADMIN only
  - Summary cards: total orgs, active orgs, total branches/students/faculty/users
  - Pie chart: organizations by status
  - Bar charts: students/faculty/branches per organization
  - Pie chart: users by role (platform-wide)
  - Audit action counts + 10 most recent activities
- Org Dashboard (`GET /api/v1/dashboard/organization/{orgId}`) — SUPER_ADMIN, ORG_ADMIN, ADMIN (org-scoped)
  - Summary cards: branches, classes, students, faculty, users for this org
  - Pie charts: students by status, students by gender, faculty by status
  - Bar charts: students/faculty/classes per branch
  - Pie chart: users by role (within org)
  - Audit action counts + 10 most recent activities for this org
- Added count methods to all repositories: StudentRepository, FacultyRepository, BranchRepository, AcademicClassRepository, UserRepository, OrganizationRepository, AuditLogRepository
- Org dashboard enforces org access via `CurrentUser.belongsToOrg()`
- Branch Dashboard (`GET /api/v1/dashboard/organization/{orgId}/branch/{branchId}`) — SUPER_ADMIN, ORG_ADMIN, ADMIN (org-scoped)
  - Summary cards: classes, students, faculty for this branch
  - Pie charts: students by status, students by gender, faculty by status, faculty by department
  - Bar chart: students per class
  - Recent activity (org-level, since audit logs don't have branchId)
- Added branch-scoped count methods to StudentRepository, FacultyRepository
- Added non-paginated list queries for classes and faculty by branch

### 23. Mandatory Branch Mapping & Input Validation
- Made `branchId` mandatory (`@NotBlank`) on CreateStudentRequest and CreateFacultyRequest
- Added branch existence validation in StudentService and FacultyService create methods
- Added `@NotBlank` validation to CreateStudentRequest (rollNumber, firstName, lastName), CreateFacultyRequest (employeeId, firstName, lastName), CreateAcademicClassRequest (name, academicYear)
- Added `@Valid` annotation to all controller create/update endpoints:
  - OrganizationController, BranchController, AcademicClassController, StudentController, FacultyController
  - (UserController and AuthController already had it)
- Branch existence validated against the org: `branchRepository.findByIdAndOrganizationId()`

### 24. User Role-to-Branch Mapping Enforcement
- Enforced branchId as mandatory for ADMIN and USER roles in both user creation paths:
  - `UserService.create()` — throws IllegalArgumentException if branchId missing for ADMIN/USER
  - `AuthService.register()` — same enforcement added; also added `BranchRepository` dependency
- ORG_ADMIN branchId remains optional but is validated if provided
- Branch validated against the organization: `branchRepository.findByIdAndOrganizationId()`
- Summary of role-to-mapping rules:
  - SUPER_ADMIN: no orgId, no branchId
  - ORG_ADMIN: orgId required, branchId optional
  - ADMIN: orgId required, branchId required
  - USER: orgId required, branchId required

### 25. Organization Data Isolation & Role-Based Access Control
- **Service-level org access validation** — Added `CurrentUser.belongsToOrg(organizationId)` check to all public methods in:
  - `StudentService` — create, getById, getAllByOrganization, getAllByBranch, getAllByClass, update, delete
  - `FacultyService` — create, getById, getAllByOrganization, getAllByBranch, getAllByDepartment, update, delete
  - `AcademicClassService` — create, getById, getAllByBranch, update, delete
  - `BranchService` — create, getById, getAllByOrganization, update, delete
  - (UserService already had this)
- Non-Super Admin users can now only access data within their own organization (Super Admin bypasses via `CurrentUser.belongsToOrg()`)

- **Controller-level @PreAuthorize annotations** added:
  - `OrganizationController` — all CRUD: `SUPER_ADMIN` only
  - `BranchController` — create/update/delete: `SUPER_ADMIN, ORG_ADMIN`; read: any authenticated
  - `StudentController` — create/update/delete: `SUPER_ADMIN, ORG_ADMIN, ADMIN`; read: any authenticated
  - `FacultyController` — create/update/delete: `SUPER_ADMIN, ORG_ADMIN, ADMIN`; read: any authenticated
  - `AcademicClassController` — create/update/delete: `SUPER_ADMIN, ORG_ADMIN, ADMIN`; read: any authenticated
  - (UserController and DashboardController already had annotations)

- **SecurityConfig** — added `PUT /api/v1/organizations/**` to SUPER_ADMIN-only rules

- **UserService.update()** — added branchId validation after all fields are set: if final role is ADMIN/USER, branchId must be present and valid in the org

### Endpoint Security Summary

| Resource | GET | POST | PUT | DELETE |
|----------|-----|------|-----|--------|
| Organizations | Authenticated | SUPER_ADMIN | SUPER_ADMIN | SUPER_ADMIN |
| Branches | Authenticated | SUPER_ADMIN, ORG_ADMIN | SUPER_ADMIN, ORG_ADMIN | SUPER_ADMIN, ORG_ADMIN |
| Students | Authenticated | SA, OA, ADMIN | SA, OA, ADMIN | SA, OA, ADMIN |
| Faculty | Authenticated | SA, OA, ADMIN | SA, OA, ADMIN | SA, OA, ADMIN |
| Classes | Authenticated | SA, OA, ADMIN | SA, OA, ADMIN | SA, OA, ADMIN |
| Users | SA, OA, ADMIN | SA, OA, ADMIN | SA, OA | SA, OA |
| Audit Logs | SUPER_ADMIN | — | — | — |
| Dashboard (Platform) | SUPER_ADMIN | — | — | — |
| Dashboard (Org/Branch) | SA, OA, ADMIN | — | — | — |

All org-scoped endpoints also enforce `CurrentUser.belongsToOrg()` at the service layer.

### 26. Test Data v2 — Expanded Seed Script
- Rewrote `test-data.js` with significantly more data for all 3 organizations:
  - **Organizations:** 3 (2 ACTIVE, 1 INACTIVE)
  - **Branches:** 6 (5 ACTIVE, 1 INACTIVE) — added West Branch to Oakwood
  - **Classes:** 10 (was 7) — added Grade 9B, Physics 101, Year 8 Beta
  - **Students:** 20 (was 10) — mixed statuses: 15 ACTIVE, 2 INACTIVE, 1 GRADUATED, 1 TRANSFERRED, 1 DROPPED
  - **Faculty:** 12 (was 8) — mixed statuses: 10 ACTIVE, 1 ON_LEAVE, 1 RESIGNED
  - **Users:** 11 (was 7) — added Oakwood users (ORG_ADMIN, ADMIN, USER) + Springfield North Campus ADMIN
  - **Audit Logs:** 22 seed entries — CREATE/UPDATE/DELETE/LOGIN/REGISTER across all orgs for dashboard graphs
- All users follow role-to-mapping rules: ADMIN/USER always have branchId, ORG_ADMIN has only orgId
- Audit log entries include `_class` field for Spring Data MongoDB polymorphism
- Added audit_logs indexes: `(organizationId, timestamp desc)` and `(entityType, entityId)`
- Script prints login credentials summary at the end

Login accounts (password: `Password@123`):
| Role | Email |
|------|-------|
| SUPER_ADMIN | superadmin@ark-platform.com |
| ORG_ADMIN | orgadmin@springfieldacademy.edu, orgadmin@riversidecollege.edu, orgadmin@oakwoodintl.edu |
| ADMIN | admin@springfieldacademy.edu, admin.north@springfieldacademy.edu, admin@riversidecollege.edu, admin@oakwoodintl.edu |
| USER | user@springfieldacademy.edu, user@riversidecollege.edu, user@oakwoodintl.edu |

### 27. ARK Platform ID System
- Added `arkId` field to Organization, Branch, Student, and Faculty models
- Format: `ARK-{PREFIX}-{8-CHAR-UPPERCASE-ALPHANUMERIC}` using SecureRandom
  - `ARK-ORG-XXXXXXXX` — Organizations
  - `ARK-BR-XXXXXXXX` — Branches
  - `ARK-STU-XXXXXXXX` — Students
  - `ARK-FAC-XXXXXXXX` — Faculty
- Character set: `A-Z, 0-9` (36 chars, 8 length = ~2.8 trillion combinations)
- Created `ArkIdGenerator` utility class (`com.app.ark_backend_services.util.ArkIdGenerator`)
- arkId is auto-generated at creation time in service layer (not user-provided)
- arkId is immutable — never changes after creation
- Unique index on arkId for all 4 collections
- Added arkId to all response DTOs (OrganizationResponse, BranchResponse, StudentResponse, FacultyResponse)
- Updated test-data.js seed script with arkId generation
- Existing fields (`rollNumber`, `employeeId`) remain as org-managed display labels

### 28. Examinations Feature
- Implemented full examination management: Examination → ExamSubject → ExamResult hierarchy
- **Models:**
  - `Examination` — scoped to org + branch + academicYear; types: MIDTERM, FINAL, QUARTERLY, HALF_YEARLY, UNIT_TEST, SUPPLEMENTARY; statuses: SCHEDULED, IN_PROGRESS, COMPLETED, CANCELLED
  - `ExamSubject` — links exam to a subject/class with maxMarks, passingMarks, examDate; statuses: SCHEDULED, COMPLETED, CANCELLED
  - `ExamResult` — individual student score per exam subject; auto-calculated grade (A+/A/B+/B/C/D/F) and pass/fail status; statuses: PASS, FAIL, ABSENT, WITHHELD
- **Repositories:** ExaminationRepository, ExamSubjectRepository, ExamResultRepository with compound indexes for uniqueness and query performance
- **Service:** `ExaminationService` with full CRUD for all 3 entities, validation chains, auto-grade calculation, cascade deletes
- **Controller:** `ExaminationController` at `/api/v1/organizations/{organizationId}` with 14 endpoints
- **Cascade deletes:** deleteExamination removes results → subjects → exam; deleteExamSubject removes results → subject
- **Integration:** OrganizationService and BranchService updated for cascade delete of examination data
- **Seed data:** 4 examinations, 9 exam subjects, 17 exam results across Springfield, Riverside, and Oakwood
- ArkId prefix: `ARK-EXM-XXXXXXXX` for examinations

## Session 3 (2026-03-10)

### 29. Enrollment & Assignment Architecture Redesign
- Implemented architectural redesign documented in `docs/enrollment-assignment-api.md` and `docs/promotion-api.md`
- Separates student identity from class assignment; enrollment is now the source of truth for "which class is a student in"

#### New Models
- **StudentEnrollment** — links student to class per academic year; statuses: ACTIVE, COMPLETED; exitReasons: PROMOTED, GRADUATED, HELD_BACK, TRANSFERRED, DROPPED
  - Unique index: `{studentId, academicYear}`; one ACTIVE enrollment per student at a time
  - ArkId prefix: `ARK-ENR-XXXXXXXX`
- **FacultyAssignment** — links faculty to class/subject per academic year; types: SUBJECT_TEACHER, CLASS_TEACHER, BOTH; statuses: ACTIVE, COMPLETED
  - Unique index: `{facultyId, classId, subjectName, academicYear}`
  - ArkId prefix: `ARK-ASN-XXXXXXXX`
- **ClassProgression** — org/branch-level config defining class ordering for promotion (e.g., Grade 9 → 10 → 11 → 12)
  - Unique index: `{organizationId, branchId}`

#### New Services
- **StudentEnrollmentService** — enroll, getActiveEnrollment, getEnrollmentHistory, getEnrollmentsByClass, getEnrollmentsByBranchAndYear, closeEnrollment
- **FacultyAssignmentService** — full CRUD for assignments with validation
- **ClassProgressionService** — upsert (create or update) and get
- **PromotionService** — preview (exam-based recommendations using FINAL exams) and execute (close old enrollments, create new ones, auto-create target class, dual-write Student.classId)
- **FacultyPerformanceService** — derives metrics via chain: FacultyAssignment → ExamSubject (matched by classId + subjectName) → ExamResult aggregation
  - 3 views: overall performance, per-class performance, per-subject across classes

#### New Controllers (all with full Swagger annotations)
- **StudentEnrollmentController** — 6 endpoints at `/api/v1/organizations/{orgId}/branches/{branchId}/enrollments`
- **FacultyAssignmentController** — 8 endpoints at `/api/v1/organizations/{orgId}/branches/{branchId}/faculty-assignments`
- **ClassProgressionController** — 2 endpoints (PUT/GET) at `/api/v1/organizations/{orgId}/branches/{branchId}/class-progression`
- **PromotionController** — 2 endpoints (GET preview, POST execute) at `/api/v1/organizations/{orgId}/branches/{branchId}/promotions`
- **FacultyPerformanceController** — 3 endpoints at `/api/v1/organizations/{orgId}/faculty/{facultyId}/performance`

#### Modified Existing Code
- **AcademicClass** — compound unique index now includes academicYear: `{organizationId, branchId, name, section, academicYear}`
- **AcademicClassService** — duplicate checks updated to use academicYear-aware methods
- **CreateStudentRequest** — added `academicYear` field
- **StudentService** — auto-creates initial enrollment when student is created with classId + academicYear
- **OrganizationService & BranchService** — cascade deletes now include enrollments, assignments, progressions
- **DashboardService** — org and branch dashboards now include totalEnrollments and totalFacultyAssignments counts
- **OrgDashboardResponse & BranchDashboardResponse** — added totalEnrollments and totalFacultyAssignments fields
- **ExaminationService** — `createExamResult()` now validates student's active enrollment classId matches the exam subject's classId; resolves classId from enrollment when available (backward-compatible fallback to ExamSubject.classId)
- **ExaminationRepository** — added `List<Examination> findByOrganizationIdAndBranchIdAndAcademicYear()` overload (non-paginated)

#### Seed Data Updates
- Added drops for new collections: student_enrollments, faculty_assignments, class_progressions
- Added 18 student enrollments (17 ACTIVE, 1 COMPLETED for graduated student)
- Added 11 faculty assignments mapping faculty to classes/subjects
- Added 3 class progressions (Springfield Main, Springfield North, Oakwood Central)
- Updated academic_classes index to include academicYear
- Added indexes for all 3 new collections

#### Design Decisions
- Student.classId kept for backward compatibility (dual-write) but deprecated — enrollment is source of truth (Step 16: remove Student.classId deferred until frontend migrates)
- Promotion creates enrollment records rather than separate PromotionRecord model — enrollment history with exitReason IS the promotion trail
- HELD_BACK students keep their enrollment ACTIVE (no close/re-open)
- FacultyPerformance is derived (not stored) — computed from FacultyAssignment → ExamSubject → ExamResult chain

## Session 4 (2026-03-11)

### 30. Codebase-Wide Security & Quality Fixes

#### CRITICAL Security Fixes
- **AuthService.register() role escalation** — Only SUPER_ADMINs can register SUPER_ADMIN or ORG_ADMIN accounts; unauthenticated users can only self-register as ADMIN or USER
- **SecurityConfig fail-closed default** — Changed `anyRequest().permitAll()` to `anyRequest().denyAll()` so unmapped endpoints are blocked
- **CurrentUser.belongsToOrg() null check** — Added null guard on organizationId parameter to prevent NPE bypass

#### Index Fixes
- **Student rollNumber partial unique index** — Replaced `@CompoundIndex` annotation with programmatic index in `MongoConfig` using `partialFilterExpression: { rollNumber: { $type: "string" } }` — allows multiple students without rollNumber in the same org
- **User email global unique index removed** — Removed `@Indexed(unique = true)` on `User.email`; kept only compound `{organizationId, email}` to allow same email across orgs
- **Seed data indexes updated** — `test-data.js` updated to match new partial index and removed global email index

#### Cascade Delete Completeness
- **StudentService.delete()** — Now cascade deletes: exam results → enrollments → student
- **FacultyService.delete()** — Now cascade deletes: faculty assignments → faculty
- **BranchService.delete()** — Now cascade deletes: users → exam results → exam subjects → enrollments → assignments → progressions → exams → faculty → students → classes → branch (was missing results, subjects, students, faculty, users)
- Added repository methods: `deleteByStudentId` (ExamResult, StudentEnrollment), `deleteByFacultyId` (FacultyAssignment), `deleteByOrganizationIdAndBranchId` (ExamResult, ExamSubject, Student, Faculty, User)

#### Promotion Logic Fix
- **Held-back student enrollment** — Fixed state machine violation: held-back students now keep their enrollment ACTIVE (previously it was closed then re-opened, causing inconsistent state)
- **@Transactional on PromotionService.execute()** — Multi-step promotion is now atomic
- Enrollment close only happens for PROMOTE and GRADUATE actions, with exit reason set per-action

#### Validation Improvements
- **CreateOrganizationRequest** — Added `@NotBlank` on name, `@Email` on contactEmail
- **CreateBranchRequest** — Added `@NotBlank` on name, `@Email` on contactEmail
- **CreateStudentRequest** — Added `@Email` on email
- **CreateFacultyRequest** — Added `@Email` on email
- **CreateExaminationRequest** — Added message strings to `@NotBlank`/`@NotNull` annotations
- **ExaminationService.createExamination()** — Added startDate < endDate validation
- **ExaminationService.updateExamSubject()** — Added passingMarks <= maxMarks validation after updates applied
- **ExaminationService.createExamResult()** — Simplified enrollment lookup (removed unnecessary `.stream().findFirst()` on Optional)

#### Error Handling
- **GlobalExceptionHandler** — Added handlers for `IllegalStateException` (409 Conflict) and generic `Exception` (500 Internal Server Error)

#### Code Quality
- **Student.classId** — Added `@Deprecated` annotation with comment pointing to StudentEnrollment as source of truth

#### Console Logging
- **GlobalExceptionHandler** — Added `@Slf4j` logging to all exception handlers: `warn` level for client errors (400/403/404/409), `error` level with full stack trace for unexpected 500s

### 31. Removed Unique Indexes on rollNumber and employeeId
- **arkId is the sole unique identifier** for both Student and Faculty; `rollNumber` and `employeeId` are now optional display labels
- **Student model** — Removed `@CompoundIndex` on `{organizationId, rollNumber}` (was already removed in #30 and replaced with partial index; now removed entirely)
- **Faculty model** — Removed `@CompoundIndex` on `{organizationId, employeeId}`
- **StudentService** — Removed duplicate rollNumber checks from `create()` and `update()`
- **FacultyService** — Removed duplicate employeeId checks from `create()` and `update()`
- **CreateFacultyRequest** — Removed `@NotBlank` on employeeId (now optional)
- **MongoConfig** — Drops legacy indexes (`org_student_rollno`, `org_faculty_empid`, and their auto-generated variants) on startup
- **Seed data** — Removed both `{organizationId, rollNumber}` and `{organizationId, employeeId}` unique indexes from `test-data.js`

### 32. Government ID for Students & Faculty
- Added `govtIdType` (enum) and `govtIdNumber` (String) fields to both Student and Faculty
- **GovtIdType enum values:** AADHAAR, PAN, PASSPORT, DRIVING_LICENSE, VOTER_ID, OTHER
- Enum defined separately in each model (`Student.GovtIdType`, `Faculty.GovtIdType`) for independent extensibility
- **Both fields are mandatory on create:** `@NotNull` on govtIdType, `@NotBlank` on govtIdNumber in CreateStudentRequest and CreateFacultyRequest
- Optional on update DTOs (can update other fields without resending govt ID)
- Updated all 6 DTOs: CreateStudentRequest, UpdateStudentRequest, StudentResponse, CreateFacultyRequest, UpdateFacultyRequest, FacultyResponse
- Updated both services: StudentService and FacultyService (create + update methods)

### 33. Stale academic_classes Index Fix
- **Problem:** MongoDB had old index `organizationId_1_branchId_1_name_1_section_1` (without `academicYear`) from before Session 3. The model expects `{orgId, branchId, name, section, academicYear}`. Promotion auto-create of target class hit `DuplicateKeyException` because the old index treated same class name+section across different academic years as duplicates.
- **MongoConfig** — Added `dropIndexSilently("academic_classes", "organizationId_1_branchId_1_name_1_section_1")` to drop the stale index on startup
- **GlobalExceptionHandler** — Added `DuplicateKeyException` handler (409 Conflict) so MongoDB duplicate key errors return a meaningful message instead of falling through to the generic 500 handler

## Session 5 (2026-03-17)

### 34. Tier 1 — Data Integrity & Performance Fixes

#### @Transactional on Cascade Deletes
- **StudentService.delete()** — wrapped in `@Transactional` (deletes exam results → enrollments → student atomically)
- **FacultyService.delete()** — wrapped in `@Transactional` (deletes assignments → faculty atomically)
- **BranchService.delete()** — wrapped in `@Transactional` (full cascade: users → results → subjects → enrollments → assignments → progressions → exams → faculty → students → classes → branch)
- **OrganizationService.delete()** — wrapped in `@Transactional` (full cascade of all org data)
- **ExaminationService.deleteExamination()** and **deleteExamSubject()** — wrapped in `@Transactional`

#### Active Enrollment Guard on Class Deletion
- **AcademicClassService.delete()** — Now checks `enrollmentRepository.countByClassIdAndStatus(classId, ACTIVE)` before deleting; throws `IllegalStateException` if active enrollments exist
- Added `StudentEnrollmentRepository` dependency to AcademicClassService

#### Student.classId Dual-Write Removed
- **PromotionService** — Removed `student.setClassId(targetClass.getId())` from PROMOTE action and `student.setClassId(null)` from GRADUATE action; enrollment is now the sole source of truth for class assignment
- **StudentService.create()** — No longer writes `classId` to Student model on creation (enrollment handles it)
- **StudentService.update()** — Blocked direct `classId` updates with comment explaining enrollment is source of truth

#### Enrollment History Org-Scoped
- **StudentEnrollmentRepository** — Added `findByOrganizationIdAndStudentIdOrderByAcademicYearDesc(orgId, studentId)`
- **StudentEnrollmentService.getEnrollmentHistory()** — Now uses org-scoped query instead of bare `findByStudentIdOrderByAcademicYearDesc`, preventing cross-org data leakage

#### Optimistic Locking (@Version)
- Added `@Version private Long version` to 5 models: **Student**, **Faculty**, **StudentEnrollment**, **Organization**, **AcademicClass** (not added to read-only models)
- **GlobalExceptionHandler** — Added `OptimisticLockingFailureException` handler (409 Conflict) with user-friendly message "The record was modified by another request. Please refresh and try again."
- Prevents concurrent updates from silently overwriting each other (e.g., two admins editing same student, concurrent promotions)

#### N+1 Query Fix in PromotionService
- **preview()** — Batch-fetches all students via `studentRepository.findAllById(studentIds)` + `Collectors.toMap()` instead of N individual `findById()` calls; pre-fetches exam subjects per exam into a `Map<examId, List<ExamSubject>>` instead of querying per student
- **execute()** — Same batch-fetch pattern for students
- **Impact:** For 100 students + 2 exams, went from ~300+ queries to ~5 queries

#### Max Page Size Validation
- **WebConfig** — New config class implementing `WebMvcConfigurer` with `PageableHandlerMethodArgumentResolver` capping page size at 100 (default 20)
- Moved `@EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO)` from `ArkBackendServicesApplication` to `WebConfig` to co-locate pagination config
- Prevents OOM from malicious `size=1000000` requests

### 35. Tier 2 — Security Hardening

#### JWT Secret Externalized
- **application.yaml** — Changed `app.jwt.secret` from hardcoded value to `${JWT_SECRET:ark-jwt-secret-key-must-be-at-least-256-bits-long-for-hs256}` so production deployments use an environment variable; local dev falls back to the default

#### Organization Listing Restricted
- **OrganizationController.getAll()** — Added `@PreAuthorize("hasRole('SUPER_ADMIN')")` so only platform admins can list all organizations
- **OrganizationController.getById()** — Added org-scoping check: non-SUPER_ADMIN users can only view their own organization (`CurrentUser.belongsToOrg(id)`)

#### Password Hash Excluded from Logs
- **User.java** — Added `@ToString(exclude = "password")` so Lombok's `toString()` never includes the BCrypt hash (prevents accidental leakage in logs or error messages)

#### Failed Login Audit Logging
- **AuditLog.Action** — Added `LOGIN_FAILED` to the enum
- **AuthService.login()** — Now logs failed attempts with reason:
  - Unknown email → `log.warn()` only (no user ID to record in audit)
  - Wrong password → `LOGIN_FAILED` audit log + `log.warn()`
  - Inactive/locked account → `LOGIN_FAILED` audit log + `log.warn()`
- Added `@Slf4j` to AuthService for console logging of all failure types

#### CORS Headers Whitelist
- **CorsConfig** — Replaced `setAllowedHeaders(List.of("*"))` with explicit whitelist: `Authorization`, `Content-Type`, `Accept`, `X-Requested-With`

### 36. Comprehensive Test Data (v3)
- Rewrote `src/main/resources/test-data.js` with full coverage of all features
- **Data seeded:**
  - 3 Organizations (Active, Active, Inactive)
  - 6 Branches (2 per org, mix of Active/Inactive)
  - 12 Academic Classes (current + prior year for promotion testing)
  - 19 Students (all statuses: Active, Inactive, Graduated, Transferred, Dropped; all with `govtIdType`/`govtIdNumber` and `version`)
  - 10 Faculty (Active, On Leave, Resigned; all with govtId fields and `version`)
  - 11 Users across all 4 RBAC tiers (SUPER_ADMIN, ORG_ADMIN, ADMIN, USER) + Locked + Inactive accounts
  - 17 Student Enrollments (Active + Completed with PROMOTED exit reason for history)
  - 3 Class Progressions (Springfield Main 3-step, North 3-step, Oakwood 2-step with terminal)
  - 9 Faculty Assignments (SUBJECT_TEACHER, CLASS_TEACHER, BOTH types)
  - 4 Examinations (FINAL scheduled + MIDTERM completed)
  - 13 Exam Subjects across all exam/class combinations
  - 6 Exam Results (midterm: Pass + Fail scenarios for 3 students × 2 subjects)
- **Test scenarios covered:**
  - Promotion: 1st→2nd (non-terminal), 3rd (graduation/terminal), Year 8→9, Year 9 (graduation)
  - Exam results: PASS/FAIL with grades, midterm completed
  - Auth: login with all roles, locked account rejection, inactive account rejection
  - Enrollment history: active + completed (prior year promoted)
  - Multi-tenancy: data isolated across 3 organizations
- All indexes explicitly created in script to match model annotations
- **Test accounts (password: `Password@123` for all):**

| Role | Email | Org | Branch | Status |
|------|-------|-----|--------|--------|
| SUPER_ADMIN | superadmin@ark.platform | — | — | ACTIVE |
| ORG_ADMIN | orgadmin@springfield.edu | Springfield Academy | — | ACTIVE |
| ORG_ADMIN | orgadmin@riverside.edu | Riverside College | — | ACTIVE |
| ORG_ADMIN | orgadmin@oakwood.edu | Oakwood International | — | ACTIVE |
| ADMIN | admin.main@springfield.edu | Springfield Academy | Main Campus | ACTIVE |
| ADMIN | admin.north@springfield.edu | Springfield Academy | North Campus | ACTIVE |
| ADMIN | admin.downtown@riverside.edu | Riverside College | Downtown Campus | ACTIVE |
| USER | user.faculty@springfield.edu | Springfield Academy | Main Campus | ACTIVE |
| USER | user@riverside.edu | Riverside College | Downtown Campus | ACTIVE |
| USER | locked@springfield.edu | Springfield Academy | Main Campus | LOCKED |
| ADMIN | admin@oakwood.edu | Oakwood International | Central Branch | INACTIVE |

### 37. CSV Bulk Import Feature
- **New dependency:** Apache Commons CSV 1.12.0 (`pom.xml`)
- **New config:** `spring.servlet.multipart.max-file-size: 5MB` in `application.yaml`
- **New files:**
  - `dto/BulkImportResponse.java` — Response DTO with entityType, totalRows, successCount, failureCount, List of RowError(row, message)
  - `service/BulkImportService.java` — Core CSV parsing, row validation, batch persistence (batch size 100, max 10K rows)
  - `controller/BulkImportController.java` — REST endpoints under `/api/v1/organizations/{orgId}/bulk-import`
- **Endpoints:**

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/students` | SUPER_ADMIN, ORG_ADMIN, ADMIN | Upload student CSV (auto-creates enrollments if classId+academicYear present) |
| POST | `/faculty` | SUPER_ADMIN, ORG_ADMIN, ADMIN | Upload faculty CSV (pipe-delimited qualifications/specializations) |
| POST | `/academic-classes` | SUPER_ADMIN, ORG_ADMIN, ADMIN | Upload class CSV (skips duplicates by compound key) |
| POST | `/branches` | SUPER_ADMIN, ORG_ADMIN | Upload branch CSV (skips duplicates by name) |
| GET | `/samples/{entityType}` | Any authenticated | Download sample CSV template |

- **Processing:** Pre-caches branchIds/classIds for the org upfront to avoid N+1 queries during row validation; row-level errors collected (not thrown) so one bad row doesn't abort import; audit log entry per import operation
- **Duplicate handling:** Branches checked by name; classes checked by {orgId, branchId, name, section, academicYear}; within-CSV duplicates also tracked

### 38. Fix Students-by-Class Query (deprecated classId field)
- **Problem:** After removing `Student.classId` dual-write in #34, the `GET /students?classId=` endpoint queried the deprecated `Student.classId` field which is no longer written. This broke student listing by class in the frontend (Students page, EnterMarks, ExamResults, etc.)
- **StudentService.getAllByClass()** — Rewrote to query via `StudentEnrollmentRepository.findByOrganizationIdAndClassIdAndStatus(ACTIVE)`, collect studentIds, then fetch students via `findByIdInAndOrganizationId`. Manual pagination over the result set.
- **StudentRepository** — Added `findByIdInAndOrganizationId(Collection<String> ids, String organizationId)` method
- **DashboardService** — Fixed `studentsPerClass` metric to use `enrollmentRepository.countByClassIdAndStatus(classId, ACTIVE)` instead of deprecated `studentRepository.countByOrganizationIdAndClassId`
- **Impact:** All frontend pages that filter students by classId now work correctly without any frontend changes

### 39. Pagination, Sorting & Ordering Refactor

#### Phase 1 — AuditLogRepository: Remove Hardcoded Sort
- Removed `OrderByTimestampDesc` from all 5 paginated method names in `AuditLogRepository`
- Now sort is controlled by `Pageable` parameter (via `@SortDefault` or client `?sort=`)
- `AuditLogService` updated to call the renamed methods
- `findTop10ByOrderByTimestampDesc` kept as-is (fixed-limit dashboard queries)

#### Phase 2 — `@SortDefault` Per Controller Endpoint
Added entity-appropriate default sort to all paginated endpoints using Spring Data's `@SortDefault`:

| Controller | Endpoints | Default Sort |
|------------|-----------|-------------|
| StudentController | getAll | `firstName ASC` |
| FacultyController | getAll | `firstName ASC` |
| OrganizationController | getAll | `name ASC` |
| BranchController | getAll | `name ASC` |
| AcademicClassController | getAll | `name ASC` |
| UserController | getAll | `fullName ASC` |
| AuditLogController | getAll | `timestamp DESC` |
| ExaminationController | getByBranch | `startDate DESC` |
| ExaminationController | getExamSubjects | `subjectName ASC` |
| ExaminationController | getResultsBySubject, getResultsByClass | `createdAt DESC` |
| StudentEnrollmentController | getByClass, getByBranchAndYear | `enrolledAt DESC` |
| FacultyAssignmentController | getAllByFaculty, getByClass, getByBranch | `createdAt DESC` |

- All defaults are overridable by client via `?sort=fieldName,asc|desc`
- WebConfig global fallback (`createdAt DESC`) still applies if no `@SortDefault` present
