# Student Enrollment & Faculty Assignment — Frontend Integration Guide

## Overview

This document covers two major architectural changes:

1. **StudentEnrollment** — replaces the direct `Student.classId` with a trackable enrollment system
2. **FacultyAssignment** — connects faculty to classes and subjects, enabling performance metrics

Both follow the same pattern: **separate identity from assignment.** A student or faculty member is a permanent entity. Their connection to a class/subject is temporal and tracked per academic year.

---

## Part 1: Student Enrollment

### Why This Change

Previously, `Student.classId` was a single mutable field. When a student was promoted, the old value was overwritten — history lost. Now, enrollment is a separate entity that tracks every class a student has been in, when they joined, when they left, and why.

### Data Model

**Student** (modified — `classId` removed):
```
Fields: id, arkId, organizationId, branchId, rollNumber, firstName, lastName,
        email, phone, dateOfBirth, gender, address, city, state, zipCode,
        guardianName, guardianPhone, guardianEmail, enrollmentDate, status,
        createdAt, updatedAt
```

**StudentEnrollment** (new):
```
Fields:
  - id                  — unique identifier
  - organizationId      — tenant isolation
  - branchId            — branch of the class
  - studentId           — references Student
  - classId             — references AcademicClass
  - academicYear        — e.g., "2025-2026"
  - enrolledAt          — when the student joined this class
  - exitedAt            — when the student left (null if still active)
  - exitReason          — why they left (null if still active)
  - status              — ACTIVE or COMPLETED
  - createdAt, updatedAt
```

### Enums

**EnrollmentStatus:**
| Value | Description |
|---|---|
| `ACTIVE` | Student is currently enrolled in this class |
| `COMPLETED` | Enrollment ended (promoted, graduated, transferred, etc.) |

**ExitReason:**
| Value | Description |
|---|---|
| `PROMOTED` | Student was promoted to the next class |
| `GRADUATED` | Student completed the terminal class |
| `HELD_BACK` | Student was held back (not promoted) |
| `TRANSFERRED` | Student transferred to another branch/school |
| `DROPPED` | Student dropped out |

### Constraints

- A student can have **only one ACTIVE enrollment at a time**
- Unique index: `{studentId, academicYear}` — one class per student per academic year
- A student can have **many COMPLETED enrollments** (one per year they attended)

---

### API Endpoints — Student Enrollment

Base: `/api/v1/organizations/{organizationId}`

#### Create Student (Modified)

**POST** `/students`

Now accepts `classId` and `academicYear` to create the initial enrollment alongside the student.

**Request Body:**
```json
{
  "branchId": "branch123",
  "classId": "class456",
  "academicYear": "2025-2026",
  "firstName": "Emma",
  "lastName": "Johnson",
  "rollNumber": "SA-2026-001",
  "email": "emma@school.edu",
  "phone": "+1-555-0001",
  "dateOfBirth": "2010-03-15",
  "gender": "Female",
  "guardianName": "Robert Johnson",
  "guardianPhone": "+1-555-0002"
}
```

| Field | Type | Required | Description |
|---|---|---|---|
| `branchId` | String | Yes | Branch the student belongs to |
| `classId` | String | No | Class to enroll in (creates StudentEnrollment) |
| `academicYear` | String | Conditional | Required if `classId` is provided |
| `firstName` | String | Yes | Student's first name |
| `lastName` | String | Yes | Student's last name |
| All other fields | Various | No | Personal and guardian info |

**What happens:**
1. Student record is created (personal info, no classId on student)
2. If `classId` + `academicYear` provided → StudentEnrollment is created with `status = ACTIVE`

**Response (201):**
```json
{
  "id": "stu001",
  "arkId": "ARK-STU-A1B2C3D4",
  "organizationId": "org001",
  "branchId": "branch123",
  "firstName": "Emma",
  "lastName": "Johnson",
  "rollNumber": "SA-2026-001",
  "status": "ACTIVE",
  "currentEnrollment": {
    "id": "enr001",
    "classId": "class456",
    "className": "Grade 10",
    "section": "A",
    "academicYear": "2025-2026",
    "enrolledAt": "2025-08-15T00:00:00Z",
    "status": "ACTIVE"
  },
  "createdAt": "...",
  "updatedAt": "..."
}
```

---

#### Get Student by ID (Modified)

**GET** `/students/{studentId}`

**Response now includes `currentEnrollment`:**
```json
{
  "id": "stu001",
  "arkId": "ARK-STU-A1B2C3D4",
  "organizationId": "org001",
  "branchId": "branch123",
  "firstName": "Emma",
  "lastName": "Johnson",
  "rollNumber": "SA-2026-001",
  "status": "ACTIVE",
  "currentEnrollment": {
    "id": "enr001",
    "classId": "class456",
    "className": "Grade 10",
    "section": "A",
    "academicYear": "2025-2026",
    "enrolledAt": "2025-08-15T00:00:00Z",
    "status": "ACTIVE"
  },
  "createdAt": "...",
  "updatedAt": "..."
}
```

If the student has no active enrollment (e.g., graduated), `currentEnrollment` will be `null`.

---

#### List Students (Modified)

**GET** `/students?classId={classId}`

Previously filtered by `Student.classId`. Now queries via **active enrollments**.

| Query Parameter | Description |
|---|---|
| `classId` | Students actively enrolled in this class |
| `branchId` | Students belonging to this branch |
| `academicYear` | Students enrolled in any class for this year |
| (none) | All students in the organization |

---

#### Enroll Student in a Class

**POST** `/students/{studentId}/enrollments`

Used when a student needs to be enrolled in a class (e.g., new admission mid-year, re-enrollment after transfer).

**Request Body:**
```json
{
  "classId": "class789",
  "academicYear": "2025-2026"
}
```

| Field | Type | Required | Description |
|---|---|---|---|
| `classId` | String | Yes | Class to enroll in |
| `academicYear` | String | Yes | Academic year |

**Response (201):**
```json
{
  "id": "enr002",
  "studentId": "stu001",
  "classId": "class789",
  "className": "Grade 10",
  "section": "B",
  "academicYear": "2025-2026",
  "enrolledAt": "2025-11-01T00:00:00Z",
  "exitedAt": null,
  "exitReason": null,
  "status": "ACTIVE"
}
```

**Error Responses:**
| Code | Reason |
|---|---|
| 400 | Student already has an active enrollment |
| 404 | Student or class not found |

---

#### Get Enrollment History for a Student

**GET** `/students/{studentId}/enrollments`

Returns all enrollments (current and past), ordered by academic year descending.

**Response (200):**
```json
[
  {
    "id": "enr002",
    "classId": "class456",
    "className": "Grade 10",
    "section": "A",
    "academicYear": "2025-2026",
    "enrolledAt": "2025-08-15T00:00:00Z",
    "exitedAt": null,
    "exitReason": null,
    "status": "ACTIVE"
  },
  {
    "id": "enr001",
    "classId": "class123",
    "className": "Grade 9",
    "section": "A",
    "academicYear": "2024-2025",
    "enrolledAt": "2024-08-10T00:00:00Z",
    "exitedAt": "2025-04-15T00:00:00Z",
    "exitReason": "PROMOTED",
    "status": "COMPLETED"
  }
]
```

---

#### Get All Enrollments for a Class

**GET** `/branches/{branchId}/classes/{classId}/enrollments?academicYear=2025-2026`

Returns all students enrolled in a class (current year by default, or specify a past year for historical data).

**Response (200):** Paginated list of enrollment records with student info.

```json
{
  "content": [
    {
      "id": "enr001",
      "studentId": "stu001",
      "studentArkId": "ARK-STU-A1B2C3D4",
      "studentName": "Emma Johnson",
      "rollNumber": "SA-2026-001",
      "academicYear": "2025-2026",
      "enrolledAt": "2025-08-15T00:00:00Z",
      "exitedAt": null,
      "exitReason": null,
      "status": "ACTIVE"
    }
  ],
  "totalElements": 35,
  "totalPages": 2
}
```

---

#### Endpoint Summary — Student Enrollment

| Method | Endpoint | Description | Roles |
|---|---|---|---|
| POST | `/students` | Create student + optional enrollment | SA, OA, ADMIN |
| GET | `/students/{studentId}` | Get student with current enrollment | All authenticated |
| GET | `/students?classId=&academicYear=` | List students by class/year | All authenticated |
| POST | `/students/{studentId}/enrollments` | Enroll student in a class | SA, OA, ADMIN |
| GET | `/students/{studentId}/enrollments` | Student's enrollment history | All authenticated |
| GET | `/branches/{branchId}/classes/{classId}/enrollments?academicYear=` | Class roster | All authenticated |

---

## Part 2: Faculty Assignment

### Why This Change

Currently, Faculty is just a profile (name, department, designation). There's no link to classes, subjects, or student performance. `FacultyAssignment` connects faculty to what they teach, enabling workload tracking and performance analytics.

### Data Model

**Faculty** (unchanged — stays as personal profile):
```
Fields: id, arkId, organizationId, branchId, employeeId, firstName, lastName,
        email, phone, dateOfBirth, gender, address, city, state, zipCode,
        department, designation, qualifications, specializations,
        joiningDate, status, createdAt, updatedAt
```

**FacultyAssignment** (new):
```
Fields:
  - id                  — unique identifier
  - organizationId      — tenant isolation
  - branchId            — branch of the class
  - facultyId           — references Faculty
  - classId             — references AcademicClass
  - academicYear        — e.g., "2025-2026"
  - subjectName         — what they teach (e.g., "Mathematics")
  - subjectCode         — optional, matches ExamSubject.subjectCode
  - assignmentType      — SUBJECT_TEACHER, CLASS_TEACHER, or BOTH
  - status              — ACTIVE, COMPLETED, or RELIEVED
  - assignedAt          — when the assignment started
  - relievedAt          — when the assignment ended (null if active)
  - createdAt, updatedAt
```

### Enums

**AssignmentType:**
| Value | Description |
|---|---|
| `SUBJECT_TEACHER` | Teaches a specific subject in a class |
| `CLASS_TEACHER` | Homeroom/class teacher responsible for overall class |
| `BOTH` | Class teacher who also teaches a subject in that class |

**AssignmentStatus:**
| Value | Description |
|---|---|
| `ACTIVE` | Currently assigned |
| `COMPLETED` | Academic year ended, assignment archived |
| `RELIEVED` | Removed from assignment mid-year |

### Constraints

- Unique index: `{facultyId, classId, subjectName, academicYear}` — no duplicate assignments
- A class can have **only one CLASS_TEACHER** (or BOTH) per academic year
- A faculty member can be CLASS_TEACHER for **at most one class** per academic year
- A faculty member can be SUBJECT_TEACHER for **multiple classes** simultaneously

---

### API Endpoints — Faculty Assignment

Base: `/api/v1/organizations/{organizationId}/branches/{branchId}/faculty-assignments`

#### Create Faculty Assignment

**POST** `/`

Assigns a faculty member to teach a subject in a class, or as class teacher.

**Request Body:**
```json
{
  "facultyId": "fac001",
  "classId": "class456",
  "academicYear": "2025-2026",
  "subjectName": "Mathematics",
  "subjectCode": "MATH-10",
  "assignmentType": "SUBJECT_TEACHER"
}
```

| Field | Type | Required | Description |
|---|---|---|---|
| `facultyId` | String | Yes | Faculty member to assign |
| `classId` | String | Yes | Class to assign to |
| `academicYear` | String | Yes | Academic year |
| `subjectName` | String | Conditional | Required for SUBJECT_TEACHER and BOTH. Not required for CLASS_TEACHER |
| `subjectCode` | String | No | Optional subject code |
| `assignmentType` | Enum | Yes | `SUBJECT_TEACHER`, `CLASS_TEACHER`, or `BOTH` |

**Response (201):**
```json
{
  "id": "asgn001",
  "organizationId": "org001",
  "branchId": "branch123",
  "facultyId": "fac001",
  "facultyName": "Dr. Margaret Chen",
  "classId": "class456",
  "className": "Grade 10",
  "section": "A",
  "academicYear": "2025-2026",
  "subjectName": "Mathematics",
  "subjectCode": "MATH-10",
  "assignmentType": "SUBJECT_TEACHER",
  "status": "ACTIVE",
  "assignedAt": "2025-06-01T00:00:00Z",
  "relievedAt": null,
  "createdAt": "...",
  "updatedAt": "..."
}
```

**Error Responses:**
| Code | Reason |
|---|---|
| 400 | `subjectName` missing for SUBJECT_TEACHER/BOTH |
| 400 | Class already has a class teacher for this year |
| 400 | Faculty already a class teacher for another class this year |
| 404 | Faculty or class not found |
| 409 | Duplicate assignment (same faculty, class, subject, year) |

---

#### Get Assignments for a Faculty

**GET** `/faculty/{facultyId}?academicYear=2025-2026`

Returns all class/subject assignments for a faculty member. If `academicYear` is omitted, returns all years.

**Response (200):**
```json
[
  {
    "id": "asgn001",
    "classId": "class456",
    "className": "Grade 10",
    "section": "A",
    "academicYear": "2025-2026",
    "subjectName": "Mathematics",
    "subjectCode": "MATH-10",
    "assignmentType": "SUBJECT_TEACHER",
    "status": "ACTIVE",
    "assignedAt": "2025-06-01T00:00:00Z"
  },
  {
    "id": "asgn002",
    "classId": "class789",
    "className": "Grade 10",
    "section": "B",
    "academicYear": "2025-2026",
    "subjectName": "Mathematics",
    "subjectCode": "MATH-10",
    "assignmentType": "BOTH",
    "status": "ACTIVE",
    "assignedAt": "2025-06-01T00:00:00Z"
  }
]
```

---

#### Get Faculty Assigned to a Class

**GET** `/class/{classId}?academicYear=2025-2026`

Returns all faculty assigned to a specific class.

**Response (200):**
```json
[
  {
    "id": "asgn001",
    "facultyId": "fac001",
    "facultyName": "Dr. Margaret Chen",
    "employeeId": "SA-FAC-001",
    "subjectName": "Mathematics",
    "assignmentType": "SUBJECT_TEACHER",
    "status": "ACTIVE"
  },
  {
    "id": "asgn003",
    "facultyId": "fac002",
    "facultyName": "James Patterson",
    "employeeId": "SA-FAC-002",
    "subjectName": null,
    "assignmentType": "CLASS_TEACHER",
    "status": "ACTIVE"
  }
]
```

---

#### Get Class Teacher

**GET** `/class/{classId}/class-teacher?academicYear=2025-2026`

Returns the class teacher for a specific class and year.

**Response (200):**
```json
{
  "id": "asgn003",
  "facultyId": "fac002",
  "facultyName": "James Patterson",
  "employeeId": "SA-FAC-002",
  "assignmentType": "CLASS_TEACHER",
  "status": "ACTIVE",
  "assignedAt": "2025-06-01T00:00:00Z"
}
```

Returns `404` if no class teacher assigned.

---

#### Update Faculty Assignment

**PUT** `/{assignmentId}`

Update assignment type, subject, or relieve faculty from assignment.

**Request Body:**
```json
{
  "subjectName": "Advanced Mathematics",
  "subjectCode": "MATH-10-ADV",
  "assignmentType": "BOTH",
  "status": "RELIEVED"
}
```

All fields optional. If `status` is set to `RELIEVED`, `relievedAt` is auto-set.

---

#### Delete Faculty Assignment

**DELETE** `/{assignmentId}`

Removes the assignment entirely. Use `PUT` with `status: RELIEVED` instead if you want to keep history.

---

#### Endpoint Summary — Faculty Assignment

| Method | Endpoint | Description | Roles |
|---|---|---|---|
| POST | `.../faculty-assignments/` | Assign faculty to class/subject | OA, ADMIN |
| GET | `.../faculty-assignments/faculty/{facultyId}?academicYear=` | Faculty's assignments | OA, ADMIN, USER |
| GET | `.../faculty-assignments/class/{classId}?academicYear=` | Faculty assigned to a class | OA, ADMIN, USER |
| GET | `.../faculty-assignments/class/{classId}/class-teacher?academicYear=` | Class teacher | OA, ADMIN, USER |
| PUT | `.../faculty-assignments/{assignmentId}` | Update assignment | OA, ADMIN |
| DELETE | `.../faculty-assignments/{assignmentId}` | Remove assignment | OA, ADMIN |

---

## Part 3: Faculty Performance Metrics

### How Performance is Calculated

Faculty performance is derived by linking:

```
FacultyAssignment (facultyId + classId + subjectName + academicYear)
        ↓
ExamSubject (where examinationId.academicYear matches, classId matches, subjectName matches)
        ↓
ExamResult (student scores for those exam subjects)
        ↓
Aggregation: averages, pass %, grade distribution
```

### API Endpoints — Faculty Performance

Base: `/api/v1/organizations/{organizationId}/faculty/{facultyId}/performance`

#### Overall Performance

**GET** `/?academicYear=2025-2026`

**Roles:** `ORG_ADMIN`, `ADMIN`

Returns a comprehensive performance summary for a faculty member.

**Response (200):**
```json
{
  "facultyId": "fac001",
  "facultyName": "Dr. Margaret Chen",
  "employeeId": "SA-FAC-001",
  "academicYear": "2025-2026",
  "summary": {
    "totalClassesAssigned": 3,
    "totalSubjectsTaught": 2,
    "totalStudentsTaught": 95,
    "overallAverageMarks": 72.5,
    "overallPassPercentage": 89.5,
    "overallGradeDistribution": {
      "A+": 8,
      "A": 20,
      "B+": 25,
      "B": 22,
      "C": 12,
      "D": 5,
      "F": 3
    }
  },
  "subjectWise": [
    {
      "subjectName": "Mathematics",
      "classesCount": 2,
      "classes": [
        {
          "classId": "class456",
          "className": "Grade 10 A",
          "studentsCount": 35,
          "averageMarks": 74.2,
          "passPercentage": 94.3,
          "gradeDistribution": {
            "A+": 5, "A": 10, "B+": 10, "B": 6, "C": 3, "D": 1, "F": 0
          }
        },
        {
          "classId": "class789",
          "className": "Grade 10 B",
          "studentsCount": 30,
          "averageMarks": 69.8,
          "passPercentage": 86.7,
          "gradeDistribution": {
            "A+": 2, "A": 7, "B+": 9, "B": 6, "C": 4, "D": 1, "F": 1
          }
        }
      ],
      "totalStudents": 65,
      "averageMarks": 72.2,
      "passPercentage": 90.8
    },
    {
      "subjectName": "Science",
      "classesCount": 1,
      "classes": [
        {
          "classId": "class456",
          "className": "Grade 10 A",
          "studentsCount": 30,
          "averageMarks": 68.8,
          "passPercentage": 83.3,
          "gradeDistribution": {
            "A+": 1, "A": 3, "B+": 6, "B": 10, "C": 5, "D": 3, "F": 2
          }
        }
      ],
      "totalStudents": 30,
      "averageMarks": 68.8,
      "passPercentage": 83.3
    }
  ],
  "classTeacherOf": {
    "classId": "class789",
    "className": "Grade 10 B",
    "studentsCount": 30,
    "classOverallAverage": 68.5,
    "classPassPercentage": 85.0
  }
}
```

---

#### Performance in a Specific Class

**GET** `/class/{classId}?academicYear=2025-2026`

**Roles:** `ORG_ADMIN`, `ADMIN`

Returns performance for a faculty member in a specific class.

**Response (200):**
```json
{
  "facultyId": "fac001",
  "facultyName": "Dr. Margaret Chen",
  "classId": "class456",
  "className": "Grade 10 A",
  "academicYear": "2025-2026",
  "subjectName": "Mathematics",
  "assignmentType": "SUBJECT_TEACHER",
  "studentsCount": 35,
  "averageMarks": 74.2,
  "highestMarks": 98,
  "lowestMarks": 28,
  "passPercentage": 94.3,
  "gradeDistribution": {
    "A+": 5, "A": 10, "B+": 10, "B": 6, "C": 3, "D": 1, "F": 0
  },
  "examWise": [
    {
      "examinationId": "exam001",
      "examinationName": "Mid-Term Examination",
      "examType": "MIDTERM",
      "averageMarks": 71.5,
      "passPercentage": 91.4,
      "studentsAppeared": 34,
      "studentsAbsent": 1
    },
    {
      "examinationId": "exam002",
      "examinationName": "Final Examination",
      "examType": "FINAL",
      "averageMarks": 76.8,
      "passPercentage": 97.1,
      "studentsAppeared": 35,
      "studentsAbsent": 0
    }
  ]
}
```

---

#### Performance Across All Classes for a Subject

**GET** `/subject/{subjectName}?academicYear=2025-2026`

**Roles:** `ORG_ADMIN`, `ADMIN`

Useful for comparing how a faculty member performs teaching the same subject across different classes.

**Response (200):**
```json
{
  "facultyId": "fac001",
  "facultyName": "Dr. Margaret Chen",
  "subjectName": "Mathematics",
  "academicYear": "2025-2026",
  "totalClasses": 2,
  "totalStudents": 65,
  "overallAverage": 72.2,
  "overallPassPercentage": 90.8,
  "classBreakdown": [
    {
      "classId": "class456",
      "className": "Grade 10 A",
      "studentsCount": 35,
      "averageMarks": 74.2,
      "passPercentage": 94.3
    },
    {
      "classId": "class789",
      "className": "Grade 10 B",
      "studentsCount": 30,
      "averageMarks": 69.8,
      "passPercentage": 86.7
    }
  ]
}
```

---

#### Endpoint Summary — Faculty Performance

| Method | Endpoint | Description | Roles |
|---|---|---|---|
| GET | `/faculty/{facultyId}/performance?academicYear=` | Overall performance | OA, ADMIN |
| GET | `/faculty/{facultyId}/performance/class/{classId}?academicYear=` | Per-class performance | OA, ADMIN |
| GET | `/faculty/{facultyId}/performance/subject/{subjectName}?academicYear=` | Per-subject performance | OA, ADMIN |

---

## Part 4: How Promotion Works with Enrollments

Promotion now operates on enrollments instead of directly modifying students.

### Preview

Same as before — system looks up ClassProgression, finds ACTIVE enrollments in the source class, checks FINAL exam results, and returns recommendations.

### Execute

**What changes:**

| Step | Action |
|---|---|
| 1 | Resolve target class name from ClassProgression |
| 2 | Find or auto-create target AcademicClass for new academic year |
| 3 | For each ACTIVE enrollment in source class: |
| 3a | If PROMOTE → close enrollment (exitReason=PROMOTED, status=COMPLETED), create new enrollment in target class |
| 3b | If GRADUATE → close enrollment (exitReason=GRADUATED), set Student.status = GRADUATED |
| 3c | If HOLD_BACK → close enrollment (exitReason=HELD_BACK), create new enrollment in **same class** for new academic year |
| 4 | Mark source AcademicClass as COMPLETED |
| 5 | FacultyAssignments for old year stay as-is (historical). Admin creates new assignments for the new year separately |

**Key point:** No `PromotionRecord` model needed. The enrollment history with `exitReason` IS the promotion trail.

### Querying Promotion History

Instead of a separate promotion history endpoint, use the enrollment history:

```
GET /students/{studentId}/enrollments
```

Each enrollment with `exitReason = PROMOTED` or `GRADUATED` or `HELD_BACK` tells the story.

---

## Part 5: How Exam Results Connect

### Creating Exam Results (Modified)

When creating an exam result, the system resolves the student's classId from their **active enrollment** instead of `Student.classId`.

**Flow:**
1. Frontend sends `POST /examinations/{examId}/subjects/{subjectId}/results` with `{ studentId, marksObtained }`
2. Backend looks up `StudentEnrollment where studentId = X and status = ACTIVE`
3. Uses the enrollment's `classId` to populate `ExamResult.classId`
4. Validates that the enrollment's classId matches the ExamSubject's classId

This ensures exam results are always linked to the correct class at the time of the exam.

---

## Complete Entity Relationship Diagram

```
Organization
    └──→ Branch
            ├──→ AcademicClass (name, section, academicYear)
            │        │
            │        ├──→ StudentEnrollment ──→ Student (personal info)
            │        │       • enrolledAt, exitedAt, exitReason
            │        │       • status: ACTIVE / COMPLETED
            │        │
            │        ├──→ FacultyAssignment ──→ Faculty (personal info)
            │        │       • subjectName, assignmentType
            │        │       • status: ACTIVE / COMPLETED / RELIEVED
            │        │
            │        └──→ ExamSubject ──→ Examination
            │                 │
            │                 └──→ ExamResult ──→ Student
            │
            ├──→ ClassProgression (class ordering config)
            │
            └──→ Examination (scoped to branch + year)
```

---

## Complete Enums Reference (New + Modified)

| Entity | Field | Values | Notes |
|---|---|---|---|
| StudentEnrollment | `status` | `ACTIVE`, `COMPLETED` | ACTIVE = currently enrolled |
| StudentEnrollment | `exitReason` | `PROMOTED`, `GRADUATED`, `HELD_BACK`, `TRANSFERRED`, `DROPPED` | null if still active |
| FacultyAssignment | `assignmentType` | `SUBJECT_TEACHER`, `CLASS_TEACHER`, `BOTH` | Required on create |
| FacultyAssignment | `status` | `ACTIVE`, `COMPLETED`, `RELIEVED` | RELIEVED = removed mid-year |
| AcademicClass | `status` | `ACTIVE`, `INACTIVE`, `COMPLETED` | COMPLETED = year ended / all promoted |
| ClassProgression | (sequence item) `isTerminal` | `true`, `false` | true = graduation class |

---

## All New Endpoints Summary

| Method | Endpoint | Description | Roles |
|---|---|---|---|
| **Student Enrollment** | | | |
| POST | `/students` | Create student + optional enrollment | SA, OA, ADMIN |
| POST | `/students/{studentId}/enrollments` | Enroll student in a class | SA, OA, ADMIN |
| GET | `/students/{studentId}/enrollments` | Enrollment history | All authenticated |
| GET | `/branches/{branchId}/classes/{classId}/enrollments?academicYear=` | Class roster | All authenticated |
| **Faculty Assignment** | | | |
| POST | `.../faculty-assignments/` | Assign faculty to class/subject | OA, ADMIN |
| GET | `.../faculty-assignments/faculty/{facultyId}?academicYear=` | Faculty's assignments | OA, ADMIN, USER |
| GET | `.../faculty-assignments/class/{classId}?academicYear=` | Class faculty list | OA, ADMIN, USER |
| GET | `.../faculty-assignments/class/{classId}/class-teacher?academicYear=` | Class teacher | OA, ADMIN, USER |
| PUT | `.../faculty-assignments/{assignmentId}` | Update assignment | OA, ADMIN |
| DELETE | `.../faculty-assignments/{assignmentId}` | Remove assignment | OA, ADMIN |
| **Faculty Performance** | | | |
| GET | `/faculty/{facultyId}/performance?academicYear=` | Overall metrics | OA, ADMIN |
| GET | `/faculty/{facultyId}/performance/class/{classId}?academicYear=` | Per-class metrics | OA, ADMIN |
| GET | `/faculty/{facultyId}/performance/subject/{subjectName}?academicYear=` | Per-subject metrics | OA, ADMIN |
| **Class Progression** | | | |
| PUT | `.../branches/{branchId}/class-progression` | Set class order | OA |
| GET | `.../branches/{branchId}/class-progression` | Get class order | OA, ADMIN |
| **Promotion** | | | |
| GET | `.../promotions/preview?sourceClassId=&targetAcademicYear=` | Preview | OA, ADMIN |
| POST | `.../promotions/execute` | Execute promotion | OA, ADMIN |

---

## Important Notes for Frontend

1. **`Student.classId` no longer exists.** Always use `currentEnrollment.classId` from the student response to get the current class.
2. **Enrollment is the source of truth** for "which class is a student in." Not the student record.
3. **Faculty assignments are per academic year.** At the start of each year, admin creates new assignments. Old assignments stay as COMPLETED for historical reference.
4. **Performance metrics require both FacultyAssignment and exam data.** If a faculty has assignments but no exams have been conducted, performance endpoints return zeroes.
5. **`subjectName` in FacultyAssignment must match `subjectName` in ExamSubject** for performance linking. Case-sensitive exact match.
6. **One active enrollment per student at a time.** If you try to create a new enrollment while one is active, it will return `400`.
7. **Promotion closes enrollments automatically.** Don't manually close enrollments before triggering promotion.
8. **Academic year format** should be consistent: `"2025-2026"` (start year - end year, hyphen separated).