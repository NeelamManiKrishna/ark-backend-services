# Architecture

## Layered Structure
```
controller/  → REST endpoints (@RestController)
dto/         → Request/Response DTOs (decoupled from model)
service/     → Business logic (@Service)
repository/  → Data access (MongoRepository interfaces)
model/       → MongoDB documents (@Document)
config/      → Spring configuration beans (Security, CORS, Mongo)
exception/   → Custom exceptions + GlobalExceptionHandler
security/    → JWT auth (JwtUtil, JwtAuthFilter, AuthEntryPoint, AccessDeniedHandler, CurrentUser)
```

## API Versioning
- Base path: `/api/v1/`

## Patterns
- DTOs separate from domain models (CreateXxxRequest, UpdateXxxRequest, XxxResponse)
- Response DTOs have static `from(Entity)` factory methods
- Update endpoints use partial update (null fields are skipped)
- GlobalExceptionHandler maps exceptions to structured JSON: `{error, message, timestamp}`
- MongoDB auditing enabled via `@EnableMongoAuditing` for `@CreatedDate`/`@LastModifiedDate`
- CORS configured via `CorsConfigurationSource` bean integrated with Spring Security (localhost:3000, localhost:5173)

## Authentication & Authorization
- JWT-based stateless authentication (JJWT 0.12.6)
- Access token (1h) + Refresh token (7d), BCrypt password hashing
- 4-tier RBAC: SUPER_ADMIN → ORG_ADMIN → ADMIN → USER
- SUPER_ADMIN: platform-wide access, manages orgs
- ORG_ADMIN: full admin within their org
- ADMIN: manages records within their org
- USER: read-only/limited access within assigned scope
- Non-SUPER_ADMIN users are scoped to an organization
- `CurrentUser` utility for accessing authenticated user in services

## Patterns (continued)
- Nested resources use parent ID in URL path (e.g., `/organizations/{orgId}/branches`)
- Compound unique indexes for uniqueness scoped to parent (e.g., branch name unique per org)
- Parent existence validated on create/list operations for nested resources

## Implemented APIs
- **Organization** (`/api/v1/organizations`)
  - POST / → Create org (returns 201)
  - GET /{id} → Get by ID
  - GET / → Get all (paginated)
  - PUT /{id} → Update org (partial)
  - DELETE /{id} → Delete org + cascade delete faculty, students, classes, branches (returns 204)
- **Branch** (`/api/v1/organizations/{organizationId}/branches`)
  - POST / → Create branch under org (returns 201)
  - GET /{branchId} → Get branch by ID (scoped to org)
  - GET / → Get all branches for org (paginated)
  - PUT /{branchId} → Update branch (partial)
  - DELETE /{branchId} → Delete branch + cascade delete classes (returns 204)
- **AcademicClass** (`/api/v1/organizations/{organizationId}/branches/{branchId}/classes`)
  - POST / → Create class under branch (returns 201)
  - GET /{classId} → Get class by ID
  - GET / → Get all classes for branch (paginated)
  - PUT /{classId} → Update class (partial)
  - DELETE /{classId} → Delete class (returns 204)
- **Student** (`/api/v1/organizations/{organizationId}/students`)
  - POST / → Create student (returns 201)
  - GET /{studentId} → Get student by ID
  - GET / → Get all students (paginated, filterable by ?branchId= or ?classId=)
  - PUT /{studentId} → Update student (partial)
  - DELETE /{studentId} → Delete student (returns 204)
- **Faculty** (`/api/v1/organizations/{organizationId}/faculty`)
  - POST / → Create faculty (returns 201)
  - GET /{facultyId} → Get faculty by ID
  - GET / → Get all faculty (paginated, filterable by ?branchId= or ?department=)
  - PUT /{facultyId} → Update faculty (partial)
  - DELETE /{facultyId} → Delete faculty (returns 204)
- **User** (`/api/v1/organizations/{organizationId}/users`)
  - POST / → Create user in org (returns 201) — SUPER_ADMIN, ORG_ADMIN, ADMIN
  - GET /{userId} → Get user by ID — SUPER_ADMIN, ORG_ADMIN, ADMIN
  - GET / → Get all users (paginated, filterable by ?branchId=) — SUPER_ADMIN, ORG_ADMIN, ADMIN
  - PUT /{userId} → Update user (partial) — SUPER_ADMIN, ORG_ADMIN
  - DELETE /{userId} → Delete user (returns 204) — SUPER_ADMIN, ORG_ADMIN
- **Auth** (`/api/v1/auth`) — public, no auth required
  - POST /register → Register new user (returns 201 + tokens)
  - POST /login → Login with email/password (returns tokens)
  - POST /refresh → Refresh access token using refresh token
- **Audit Log** (`/api/v1/audit-logs`) — SUPER_ADMIN only
  - GET / → Get all audit logs (paginated, filterable by ?organizationId=, ?action=, ?entityType=, ?userId=)
- **Dashboard** (`/api/v1/dashboard`)
  - GET /platform → Platform-wide metrics (SUPER_ADMIN only)
  - GET /organization/{orgId} → Org-level metrics (SUPER_ADMIN, ORG_ADMIN, ADMIN)
  - GET /organization/{orgId}/branch/{branchId} → Branch-level metrics (SUPER_ADMIN, ORG_ADMIN, ADMIN)
- **Examination** (`/api/v1/organizations/{organizationId}`)
  - POST /branches/{branchId}/examinations → Create exam (SA, OA, ADMIN)
  - GET /examinations/{examId} → Get exam by ID
  - GET /branches/{branchId}/examinations?academicYear= → List exams by branch (optionally filter by year)
  - PUT /examinations/{examId} → Update exam (SA, OA, ADMIN)
  - DELETE /examinations/{examId} → Delete exam + cascade delete subjects and results (SA, OA, ADMIN)
  - POST /examinations/{examId}/subjects → Create exam subject (SA, OA, ADMIN)
  - GET /examinations/{examId}/subjects → List exam subjects (paginated)
  - PUT /examinations/{examId}/subjects/{subjectId} → Update exam subject (SA, OA, ADMIN)
  - DELETE /examinations/{examId}/subjects/{subjectId} → Delete subject + cascade delete results (SA, OA, ADMIN)
  - POST /examinations/{examId}/subjects/{subjectId}/results → Create exam result (SA, OA, ADMIN)
  - GET /examinations/{examId}/subjects/{subjectId}/results → List results by subject (paginated)
  - GET /examinations/{examId}/students/{studentId}/results → Get student's results across all subjects
  - PUT /examinations/{examId}/results/{resultId} → Update result (SA, OA, ADMIN)

## Audit Trail
- Every CREATE, UPDATE, DELETE, LOGIN, REGISTER action is logged to `audit_logs` collection
- Each log entry captures: action, entityType, entityId, entityName, organizationId, who performed it (userId, email, role), details, timestamp
- Indexed by `(organizationId, timestamp)` and `(entityType, entityId)` for efficient querying

## Endpoint Security
| Endpoint | Access |
|---|---|
| `/api/v1/auth/**` | Public |
| `POST /api/v1/organizations` | SUPER_ADMIN only |
| `DELETE /api/v1/organizations/**` | SUPER_ADMIN only |
| `GET /api/v1/audit-logs` | SUPER_ADMIN only |
| `GET /api/v1/dashboard/platform` | SUPER_ADMIN only |
| `GET /api/v1/dashboard/organization/{orgId}` | SUPER_ADMIN, ORG_ADMIN, ADMIN |
| `GET /api/v1/dashboard/organization/{orgId}/branch/{branchId}` | SUPER_ADMIN, ORG_ADMIN, ADMIN |
| All other `/api/**` | Authenticated |
| Swagger UI, Actuator health | Public |
