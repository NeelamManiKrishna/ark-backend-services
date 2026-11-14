# ARK API Guide

Integration guide for the frontend and future mobile clients. Everything here was checked against the running application's `/v3/api-docs`; when in doubt, that (or Swagger UI at `/swagger-ui/index.html`) is the source of truth. `architecture.md` has the full endpoint list with roles.

Base URL: `/api/v1`. `{org}` below stands for `/organizations/{organizationId}`.

## 1. Conventions

### Authentication
- `POST /api/v1/auth/login` with `{email, password, deviceId?, deviceName?}` returns `{accessToken, refreshToken, tokenType, expiresIn, user}`. Send `Authorization: Bearer <accessToken>` on every other call.
- Access tokens last 30 minutes. `POST /api/v1/auth/refresh` with `{refreshToken}` returns a new pair. **Each refresh token works once**: the response carries a new one that must replace the old one. Reusing an old token revokes that whole device session (`TOKEN_REVOKED`).
- Sending `deviceId` at login makes the refresh token last 30 days instead of 7 (use it for mobile apps).
- `POST /api/v1/auth/logout` `{refreshToken}` ends one device (always 204). `POST /api/v1/auth/logout-all` ends every device. `POST /api/v1/auth/change-password` `{currentPassword, newPassword}` ends all other sessions and returns a fresh token pair for the calling device.
- `GET /api/v1/auth/me` returns the user and their scope `{role, organizationId, branchIds[], studentIds[], allBranches}`. Use it as the single source of "who am I and what can I see".
- There is no self-registration. Accounts are created by admins (`POST {org}/users`).
- After 5 wrong passwords the account is locked for 15 minutes (`ACCOUNT_LOCKED`).

### Roles and scope
| Role | Sees | Can write |
|---|---|---|
| `SUPER_ADMIN` | every organization | everything |
| `ORG_ADMIN` | their organization, all branches | everything inside it except creating/deleting the organization |
| `ADMIN` | **their own branch only** | academic records in their branch; creates `USER` accounts in their branch |
| `USER` | their own branch only, government ID numbers masked | nothing |

List endpoints silently narrow to the caller's branch; asking explicitly for another branch returns 403. The per-endpoint role is in `architecture.md`.

### Errors
Every error has the same body:
```json
{ "error": "VALIDATION_FAILED", "message": "Validation failed", "timestamp": "2026-11-14T07:00:00Z",
  "path": "/api/v1/...", "fieldErrors": [ { "field": "email", "message": "Invalid email format" } ] }
```
Branch on `error`, never on `message`.

| `error` | Status | Meaning |
|---|---|---|
| `INVALID_CREDENTIALS` | 401 | Wrong email or password (same answer for unknown emails) |
| `UNAUTHORIZED` | 401 | No token sent |
| `TOKEN_EXPIRED` | 401 | Access or refresh token expired: refresh, then retry |
| `TOKEN_REVOKED` | 401 | Token invalid, reused, or the session ended: log in again |
| `ACCOUNT_LOCKED` | 403 | Locked or inactive account |
| `ORG_SUSPENDED` | 403 | Organization or branch is not active |
| `FORBIDDEN` | 403 | Role or branch not allowed |
| `NOT_FOUND` | 404 | Unknown id, or an id that belongs to another organization |
| `DUPLICATE` / `CONFLICT` | 409 | Unique value exists / the state forbids the action (for example promoting twice) |
| `VALIDATION_FAILED` | 422 | Bean validation failed; see `fieldErrors` |
| `BAD_REQUEST` | 400 | Malformed JSON, bad parameter, or a business rule (for example invalid academic year) |

### Lists, search, sorting
`GET ...?page=0&size=20&sort=field,asc`. Responses are Spring pages: `content`, `page{size,number,totalElements,totalPages}`.
`q=text` (students, faculty, users, classes, branches, academic years, subjects) is a case-insensitive **prefix** match on the name, email and id fields of that resource and always stays inside the caller's scope.
Three list endpoints return a plain array capped at 200 rows: a student's active assignments, a student's enrollment history, and a student's exam results.

### Academic years and subjects: text or catalog id
Academic years are always `YYYY-YYYY` with consecutive years (`2025-2026`); anything else returns 400 (or 422 where the body declares a pattern).
Requests that name an academic year accept either `academicYear` (text) or `academicYearId` (from `{org}/academic-years`); those that name a subject accept `subjectName` or `subjectId` (from `{org}/subjects`). This applies to creating classes, enrollments (also the initial enrollment of a student), examinations, exam subjects, faculty assignments and to promotion (`targetAcademicYearId`). Send either or both: they must agree (400 otherwise), an unknown id is a 404, and sending neither where one is required is a 400. Responses contain both the text and the id (`academicYearId`, `subjectId`, null for records that predate the catalog or whose label is not in it). Text-only requests keep working.

### Gender
`MALE`, `FEMALE` or `OTHER`. Input is case-insensitive, stored and returned upper-case; anything else is a 400 (a bulk-import row error).

### Government ID numbers
Stored encrypted. `USER` sees them masked (`******3456`); everyone else sees the full value.

## 2. Students and enrollments

A student is a permanent record. The link to a class is an **enrollment** per academic year; `Student` has no `classId`, and student responses carry no current class. To find a student's class use the enrollment endpoints below.

**Student** fields: `id, arkId, organizationId, branchId, rollNumber, firstName, lastName, email, phone, dateOfBirth, gender, address, city, state, zipCode, guardianName, guardianPhone, guardianEmail, govtIdType, govtIdNumber, enrollmentDate, status, createdAt, updatedAt`. `status`: `ACTIVE, INACTIVE, GRADUATED, TRANSFERRED, DROPPED`.

| Call | Notes |
|---|---|
| `POST /api/v1/organizations/{organizationId}/students` | Body needs `branchId, firstName, lastName`. If `classId` **and** `academicYear` are both sent, an `ACTIVE` enrollment is created too. |
| `GET /api/v1/organizations/{organizationId}/students` | Filters: `branchId`, `classId` (students actively enrolled in that class), `q`. |
| `PUT /api/v1/organizations/{organizationId}/students/{studentId}` | Partial update (null fields ignored). Moving a student to another branch is rejected while they have an active enrollment. |

**StudentEnrollment** fields: `id, arkId, organizationId, branchId, studentId, classId, academicYear, enrolledAt, exitedAt, exitReason, status, createdAt, updatedAt`.
`status`: `ACTIVE` or `COMPLETED`. `exitReason`: `PROMOTED, GRADUATED, HELD_BACK, TRANSFERRED, DROPPED`.
Rules: one `ACTIVE` enrollment per student; one enrollment per student per academic year; the class must belong to the branch in the path.

Base: `/api/v1/organizations/{organizationId}/branches/{branchId}/enrollments`

| Call | Notes |
|---|---|
| `POST /api/v1/organizations/{organizationId}/branches/{branchId}/enrollments` | `{studentId, classId, academicYear, enrolledAt?}` returns 201. 409 if the student already has an active enrollment or one for that year. |
| `GET /api/v1/organizations/{organizationId}/branches/{branchId}/enrollments/student/{studentId}/active` | 404 if none. |
| `GET /api/v1/organizations/{organizationId}/branches/{branchId}/enrollments/student/{studentId}/history` | Newest year first (max 200). This is also the promotion trail: `exitReason` records what happened each year. |
| `GET /api/v1/organizations/{organizationId}/branches/{branchId}/enrollments/class/{classId}` | Active roster of a class (paged). |
| `GET /api/v1/organizations/{organizationId}/branches/{branchId}/enrollments` | `?academicYear=` is required; all enrollments of the branch for that year (paged). |
| `PUT /api/v1/organizations/{organizationId}/branches/{branchId}/enrollments/{enrollmentId}/close` | `{exitReason, exitedAt?}`. `GRADUATED`, `TRANSFERRED` and `DROPPED` also set the student's own `status`. |
| `PUT /api/v1/organizations/{organizationId}/branches/{branchId}/enrollments/{enrollmentId}/reopen` | Undo a closure. Only the student's latest enrollment, and only when they have no active one. 409 otherwise. |

## 3. Faculty assignments

**FacultyAssignment** fields: `id, arkId, organizationId, branchId, facultyId, classId, subjectName, academicYear, assignmentType, status, createdAt, updatedAt`.
`assignmentType`: `SUBJECT_TEACHER, CLASS_TEACHER, BOTH`. `status`: `ACTIVE, COMPLETED`.
Uniqueness: one assignment per `(faculty, class, subjectName, academicYear)`; `subjectName` may be empty for a class teacher. The backend does **not** enforce "one class teacher per class"; the UI should.
The faculty member and the class must both belong to the branch in the path. `subjectName` must match the exam subject's `subjectName` exactly (case-sensitive) for performance reports to link them.

Base: `/api/v1/organizations/{organizationId}/branches/{branchId}/faculty-assignments`

| Call | Notes |
|---|---|
| `POST /api/v1/organizations/{organizationId}/branches/{branchId}/faculty-assignments` | `{facultyId, classId, academicYear, assignmentType, subjectName?}` returns 201; 409 for a duplicate. |
| `GET /api/v1/organizations/{organizationId}/branches/{branchId}/faculty-assignments` | `?academicYear=` required. |
| `GET /api/v1/organizations/{organizationId}/branches/{branchId}/faculty-assignments/{assignmentId}` | |
| `GET /api/v1/organizations/{organizationId}/branches/{branchId}/faculty-assignments/faculty/{facultyId}` | All of a faculty member's assignments (paged). |
| `GET /api/v1/organizations/{organizationId}/branches/{branchId}/faculty-assignments/faculty/{facultyId}/active` | Active only (max 200, plain array). |
| `GET /api/v1/organizations/{organizationId}/branches/{branchId}/faculty-assignments/class/{classId}` | `?academicYear=` required. |
| `PUT /api/v1/organizations/{organizationId}/branches/{branchId}/faculty-assignments/{assignmentId}` | Only `assignmentType` and `status` can change. |
| `DELETE /api/v1/organizations/{organizationId}/branches/{branchId}/faculty-assignments/{assignmentId}` | Prefer `status: COMPLETED` to keep history. |

Moving a faculty member to another branch is rejected while they have active assignments.

## 4. Faculty performance

Derived from assignments and exam results: assignment `(faculty, class, subjectName, year)` links to exam subjects of that class and name in that year's exams, then to their results. Averages and pass rates only count students who appeared (`ABSENT` and `WITHHELD` results and results without marks are excluded). `studentsCount` is distinct students. Without exams the numbers are zero.
All three need `ORG_ADMIN`, `ADMIN` or `SUPER_ADMIN`.

| Call | Response fields |
|---|---|
| `GET /api/v1/organizations/{organizationId}/faculty/{facultyId}/performance` (`?academicYear=`) | `facultyId, facultyName, employeeId, academicYear, summary{totalClassesAssigned, totalSubjectsTaught, totalStudentsTaught, overallAverageMarks, overallPassPercentage, overallGradeDistribution}, subjectWise[{subjectName, classesCount, classes[{classId, className, studentsCount, averageMarks, passPercentage, gradeDistribution}], totalStudents, averageMarks, passPercentage}], classTeacherOf{classId, className, studentsCount, classOverallAverage, classPassPercentage}` |
| `GET /api/v1/organizations/{organizationId}/faculty/{facultyId}/performance/class/{classId}` (`?academicYear=`) | `facultyId, facultyName, classId, className, academicYear, subjectName, assignmentType, studentsCount, averageMarks, highestMarks, lowestMarks, passPercentage, gradeDistribution, examWise[{examinationId, examinationName, examType, averageMarks, passPercentage, studentsAppeared, studentsAbsent}]` |
| `GET /api/v1/organizations/{organizationId}/faculty/{facultyId}/performance/subject/{subjectName}` (`?academicYear=`) | `facultyId, facultyName, subjectName, academicYear, totalClasses, totalStudents, overallAverage, overallPassPercentage, classBreakdown[{classId, className, studentsCount, averageMarks, passPercentage}]` |

Grades: `A+` ≥ 90%, `A` ≥ 80, `B+` ≥ 70, `B` ≥ 60, `C` ≥ 50, `D` ≥ 40, otherwise `F`.

## 5. Examinations and results

`Examination → ExamSubject (per class) → ExamResult (per student)`. Under `/api/v1/organizations/{organizationId}`:

- Exams are created per branch: `POST .../branches/{branchId}/examinations` with `academicYear` as `YYYY-YYYY`. The year cannot change once the exam has subjects.
- `POST .../examinations/{examId}/subjects` needs a class of the exam's branch, `maxMarks > 0`, `passingMarks <= maxMarks`.
- `POST .../examinations/{examId}/subjects/{subjectId}/results` `{studentId, marksObtained, remarks?}`: the student must have an enrollment **for the exam's academic year** in the subject's class (it does not have to be their current one). Grade and `PASS`/`FAIL` are calculated by the server.
- Changing a subject's `maxMarks` or `passingMarks` recalculates grade and status of its existing results (`ABSENT` and `WITHHELD` are left alone).
- `GET .../examinations/{examId}/subjects/{subjectId}/results` returns that subject's results only. `PUT` and `DELETE .../examinations/{examId}/results/{resultId}` require the result to belong to that exam (404 otherwise).

## 6. Class progression and promotion

### Class progression (one per branch)
`PUT /api/v1/organizations/{organizationId}/branches/{branchId}/class-progression`
```json
{ "sequence": [
  { "className": "Grade 9", "displayOrder": 1, "isTerminal": false },
  { "className": "Grade 12", "displayOrder": 2, "isTerminal": true } ] }
```
`className` must equal the class `name`. The sequence must be gap-free (`displayOrder` 1..n, because promotion looks for "order + 1"), names must be unique, and **exactly one** level must be terminal and it must be the last (400 otherwise). The response has `classesNotCreatedYet`: levels with no class in the branch yet (fine: promotion creates the next class on demand). `GET` returns it (404 if not configured), `DELETE` removes it.

### Preview
`GET /api/v1/organizations/{organizationId}/branches/{branchId}/promotions/preview?sourceClassId=&targetAcademicYear=`

Returns `sourceClass{id, name, section, academicYear, branchId, status}`, `targetClassName`, `targetAcademicYear`, `terminalClass`, `totalEligible, totalRecommendedPromote, totalRecommendedHoldBack, totalRecommendedGraduate, totalNoExamData` and `candidates[{studentId, studentArkId, firstName, lastName, rollNumber, recommendation, hasFailingResults, failedSubjects[], examSummary, hasExamData}]`.

| Condition | `recommendation` |
|---|---|
| Source class is terminal | `GRADUATE` |
| Passed every FINAL-exam subject | `PROMOTE` |
| Failed (or was absent for) at least one FINAL-exam subject | `HOLD_BACK` |
| No FINAL-exam data | `PROMOTE`, with `hasExamData: false` |

Only `ACTIVE` students are listed; only exams of type `FINAL` count.

### Execute
`POST /api/v1/organizations/{organizationId}/branches/{branchId}/promotions/execute`
```json
{ "sourceClassId": "...", "targetAcademicYear": "2026-2027", "targetSection": "A",
  "studentOverrides": [ { "studentId": "...", "action": "HOLD_BACK", "reason": "Poor attendance" } ] }
```
`action` is `PROMOTE`, `HOLD_BACK` or `GRADUATE` (unknown values return 400). `targetSection` defaults to the source section.

Validation (all return before anything changes): `targetAcademicYear` must be `YYYY-YYYY` and later than the source class's year (400); the source class must not already be `COMPLETED` (409, so a second run does nothing); an override must name a student actively enrolled in the source class, at most once; `PROMOTE` is not allowed in the terminal class and `GRADUATE` is not allowed elsewhere (400).

What happens, per active student: no override means `GRADUATE` in the terminal class and `PROMOTE` otherwise.

| Action | Effect |
|---|---|
| `PROMOTE` | Enrollment closed (`PROMOTED`); new enrollment in the next class (auto-created for the target year when missing) |
| `HOLD_BACK` | Enrollment closed (`HELD_BACK`); new enrollment in the **same** class level for the target year |
| `GRADUATE` | Enrollment closed (`GRADUATED`); student `status` becomes `GRADUATED` |

Students who already have an enrollment in the target year are left untouched and reported as `SKIPPED`. The source class becomes `COMPLETED`.

Response: `sourceClassId, sourceClassName, targetClassId, targetClassName, sourceAcademicYear, targetAcademicYear, summary{totalProcessed, promoted, graduated, heldBack, skipped}, records[{studentId, studentArkId, studentName, promotionType, reason, targetClassId}]`. `promotionType`: `PROMOTED, GRADUATED, HELD_BACK, SKIPPED`.

There is no separate promotion history and no undo: use a student's enrollment history (section 2) as the trail. Each change is also written to the audit log.

## 7. Catalogs, search and reports

- **Academic years**: `GET|POST {org}/academic-years`, `GET|PUT|DELETE {org}/academic-years/{id}`. Create with `{label, startDate?, endDate?, current?}`; setting `current: true` clears the flag on the other years; a year used by classes cannot be deleted. Only `ORG_ADMIN`/`SUPER_ADMIN` can write; everyone can read.
- **Subjects**: `GET|POST {org}/subjects`, `PUT|DELETE {org}/subjects/{id}`. `{code, name}`, codes are upper-cased and unique per organization.
- Records store the catalog id next to the text label/name (see Conventions); deleting a catalog entry is blocked while classes use the year.
- **Reports** (`ORG_ADMIN`, `ADMIN`, `SUPER_ADMIN`): `GET /api/v1/organizations/{organizationId}/reports/enrollment` and `.../reports/performance` with `academicYear` (required), `branchId`, `format=json|csv`. JSON is `{title, academicYear, columns[], rows[{...}]}`; the enrollment report ends with a `TOTAL` row. Performance rows: `examination, subject, class, section, appeared, absent, passed, passPercentage, average, highest, lowest`.

## 8. Audit trail

`GET /api/v1/audit-logs` (`SUPER_ADMIN`) and `GET /api/v1/organizations/{organizationId}/audit-logs` (`ORG_ADMIN`, `SUPER_ADMIN`) with optional `action`, `entityType`, `userId` (combined). Entries carry who, when, what and, for updates, `before`, `after` and `changedFields` (passwords and government IDs are never recorded). Cascade deletes list how many child records were removed.
