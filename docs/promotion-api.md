# Student Promotion API — Frontend Integration Guide

## Overview

The Promotion feature allows ORG_ADMIN and ADMIN users to promote students from one class to the next at the end of an academic year. The flow is:

1. **Configure class progression** (one-time setup per branch)
2. **Preview promotion** — system returns list of students with pass/fail recommendations
3. **Execute promotion** — admin confirms, optionally overrides individual students
4. **View history** — promotion trail per student or per class

---

## Pre-requisite: Class Progression Setup

Before any promotion can happen, an ORG_ADMIN must configure the class order for each branch. This tells the system what "next class" means.

### PUT `/api/v1/organizations/{organizationId}/branches/{branchId}/class-progression`

**Roles:** `ORG_ADMIN`

**Request Body:**
```json
{
  "sequence": [
    { "className": "Grade 9", "displayOrder": 1, "isTerminal": false },
    { "className": "Grade 10", "displayOrder": 2, "isTerminal": false },
    { "className": "Grade 11", "displayOrder": 3, "isTerminal": false },
    { "className": "Grade 12", "displayOrder": 4, "isTerminal": true }
  ]
}
```

| Field | Type | Required | Description |
|---|---|---|---|
| `sequence` | Array | Yes | Ordered list of class levels |
| `sequence[].className` | String | Yes | Must match the `name` field in AcademicClass (e.g., "Grade 10") |
| `sequence[].displayOrder` | Integer | Yes | Position in sequence (1, 2, 3...) |
| `sequence[].isTerminal` | Boolean | Yes | `true` for the final year — students in this class will be graduated instead of promoted |

**Response (200):**
```json
{
  "id": "...",
  "organizationId": "...",
  "branchId": "...",
  "sequence": [
    { "className": "Grade 9", "displayOrder": 1, "isTerminal": false },
    { "className": "Grade 10", "displayOrder": 2, "isTerminal": false },
    { "className": "Grade 11", "displayOrder": 3, "isTerminal": false },
    { "className": "Grade 12", "displayOrder": 4, "isTerminal": true }
  ],
  "createdAt": "2026-03-10T...",
  "updatedAt": "2026-03-10T..."
}
```

**Error Responses:**
| Code | Reason |
|---|---|
| 400 | Empty sequence or invalid data |
| 403 | Not ORG_ADMIN |
| 404 | Organization or branch not found |

---

### GET `/api/v1/organizations/{organizationId}/branches/{branchId}/class-progression`

**Roles:** `ORG_ADMIN`, `ADMIN`

Returns the configured class progression for a branch. Returns `404` if not yet configured.

---

## Promotion Flow

### Step 1: Preview Promotion

Shows the admin which students are eligible and what the system recommends (promote, hold back, or graduate).

### GET `/api/v1/organizations/{organizationId}/branches/{branchId}/promotions/preview`

**Roles:** `ORG_ADMIN`, `ADMIN`

**Query Parameters:**

| Parameter | Type | Required | Description |
|---|---|---|---|
| `sourceClassId` | String | Yes | The class to promote FROM (e.g., "Grade 10 A, 2025-2026") |
| `targetAcademicYear` | String | Yes | The academic year to promote INTO (e.g., "2026-2027") |

**Example:** `GET .../promotions/preview?sourceClassId=abc123&targetAcademicYear=2026-2027`

**Response (200):**
```json
{
  "sourceClass": {
    "id": "abc123",
    "name": "Grade 10",
    "section": "A",
    "academicYear": "2025-2026",
    "branchId": "...",
    "status": "ACTIVE"
  },
  "targetClassName": "Grade 11",
  "targetAcademicYear": "2026-2027",
  "isTerminalClass": false,
  "totalEligible": 15,
  "totalRecommendedPromote": 12,
  "totalRecommendedHoldBack": 2,
  "totalRecommendedGraduate": 0,
  "totalNoExamData": 1,
  "candidates": [
    {
      "studentId": "stu001",
      "studentArkId": "ARK-STU-A1B2C3D4",
      "firstName": "Emma",
      "lastName": "Johnson",
      "rollNumber": "SA-2026-001",
      "recommendation": "PROMOTE",
      "hasFailingResults": false,
      "failedSubjects": [],
      "examSummary": "Passed 3/3 subjects",
      "hasExamData": true
    },
    {
      "studentId": "stu002",
      "studentArkId": "ARK-STU-E5F6G7H8",
      "firstName": "Olivia",
      "lastName": "Brown",
      "rollNumber": "SA-2026-003",
      "recommendation": "HOLD_BACK",
      "hasFailingResults": true,
      "failedSubjects": ["English"],
      "examSummary": "Passed 2/3 subjects (Failed: English)",
      "hasExamData": true
    },
    {
      "studentId": "stu003",
      "studentArkId": "ARK-STU-I9J0K1L2",
      "firstName": "Aiden",
      "lastName": "Clark",
      "rollNumber": "SA-2026-005",
      "recommendation": "PROMOTE",
      "hasFailingResults": false,
      "failedSubjects": [],
      "examSummary": "No exam data available",
      "hasExamData": false
    }
  ]
}
```

**Recommendation Logic:**

| Condition | Recommendation |
|---|---|
| Source class is terminal (isTerminal=true) | `GRADUATE` |
| Student passed all FINAL exam subjects | `PROMOTE` |
| Student failed 1+ FINAL exam subjects | `HOLD_BACK` |
| No FINAL exam data exists for student | `PROMOTE` (flagged with `hasExamData: false`) |
| Student has ABSENT/WITHHELD results | Flagged in `failedSubjects`, treated as fail |

**Only ACTIVE students are shown.** Students with status INACTIVE, TRANSFERRED, DROPPED, or GRADUATED are excluded.

**Error Responses:**
| Code | Reason |
|---|---|
| 400 | Class progression not configured for this branch |
| 400 | Source class not found in progression sequence |
| 404 | Source class not found |

---

### Step 2: Execute Promotion

Admin reviews the preview, optionally overrides individual students, and confirms.

### POST `/api/v1/organizations/{organizationId}/branches/{branchId}/promotions/execute`

**Roles:** `ORG_ADMIN`, `ADMIN`

**Request Body:**
```json
{
  "sourceClassId": "abc123",
  "targetAcademicYear": "2026-2027",
  "targetSection": "A",
  "studentOverrides": [
    {
      "studentId": "stu002",
      "action": "PROMOTE",
      "reason": "Passed supplementary exam"
    },
    {
      "studentId": "stu003",
      "action": "HOLD_BACK",
      "reason": "Poor attendance"
    }
  ]
}
```

| Field | Type | Required | Description |
|---|---|---|---|
| `sourceClassId` | String | Yes | The class to promote FROM |
| `targetAcademicYear` | String | Yes | Academic year to promote INTO |
| `targetSection` | String | No | Section for the target class. If null, keeps the same section as source |
| `studentOverrides` | Array | No | Override the system's default action for specific students |
| `studentOverrides[].studentId` | String | Yes | Student to override |
| `studentOverrides[].action` | Enum | Yes | `PROMOTE`, `HOLD_BACK`, or `GRADUATE` |
| `studentOverrides[].reason` | String | No | Reason for override (stored in promotion record) |

**What happens on execute:**

1. System resolves the **next class name** from ClassProgression (e.g., "Grade 10" → "Grade 11")
2. Finds or **auto-creates** the target AcademicClass (e.g., "Grade 11, Section A, 2026-2027") if it doesn't exist
3. For each ACTIVE student in the source class:
   - If student has an override → use that action
   - If source class is terminal → GRADUATE
   - Otherwise → PROMOTE (default)
4. For PROMOTED students: `student.classId` is updated to the new class
5. For GRADUATED students: `student.status` is set to `GRADUATED`
6. For HELD_BACK students: no changes (remain in current class)
7. A `PromotionRecord` is created for every student
8. Source class status is set to `COMPLETED`

**Response (200):**
```json
{
  "sourceClassId": "abc123",
  "sourceClassName": "Grade 10 A",
  "targetClassId": "def456",
  "targetClassName": "Grade 11 A",
  "sourceAcademicYear": "2025-2026",
  "targetAcademicYear": "2026-2027",
  "summary": {
    "totalProcessed": 15,
    "promoted": 12,
    "graduated": 0,
    "heldBack": 3
  },
  "records": [
    {
      "id": "rec001",
      "studentId": "stu001",
      "studentArkId": "ARK-STU-A1B2C3D4",
      "studentName": "Emma Johnson",
      "promotionType": "PROMOTED",
      "reason": null,
      "targetClassId": "def456"
    },
    {
      "id": "rec002",
      "studentId": "stu002",
      "studentArkId": "ARK-STU-E5F6G7H8",
      "studentName": "Olivia Brown",
      "promotionType": "PROMOTED",
      "reason": "Passed supplementary exam"
    },
    {
      "id": "rec003",
      "studentId": "stu003",
      "studentArkId": "ARK-STU-I9J0K1L2",
      "studentName": "Aiden Clark",
      "promotionType": "HELD_BACK",
      "reason": "Poor attendance"
    }
  ]
}
```

**Error Responses:**
| Code | Reason |
|---|---|
| 400 | Class progression not configured |
| 400 | Source class not in progression sequence |
| 400 | Student already promoted from this class (duplicate prevention) |
| 404 | Source class or student not found |

---

## Promotion History

### GET `/api/v1/organizations/{organizationId}/branches/{branchId}/promotions/history/student/{studentId}`

**Roles:** `ORG_ADMIN`, `ADMIN`, `USER`

Returns the full promotion trail for a student across all academic years.

**Response (200):**
```json
[
  {
    "id": "rec001",
    "sourceClassId": "class_9a_2024",
    "sourceClassName": "Grade 9 A",
    "sourceAcademicYear": "2024-2025",
    "targetClassId": "class_10a_2025",
    "targetClassName": "Grade 10 A",
    "targetAcademicYear": "2025-2026",
    "promotionType": "PROMOTED",
    "reason": null,
    "promotedBy": "admin@springfieldacademy.edu",
    "promotedAt": "2025-04-15T10:30:00Z"
  },
  {
    "id": "rec002",
    "sourceClassId": "class_10a_2025",
    "sourceClassName": "Grade 10 A",
    "sourceAcademicYear": "2025-2026",
    "targetClassId": "class_11a_2026",
    "targetClassName": "Grade 11 A",
    "targetAcademicYear": "2026-2027",
    "promotionType": "PROMOTED",
    "reason": null,
    "promotedBy": "orgadmin@springfieldacademy.edu",
    "promotedAt": "2026-04-10T14:00:00Z"
  }
]
```

---

### GET `/api/v1/organizations/{organizationId}/branches/{branchId}/promotions/history/class/{classId}`

**Roles:** `ORG_ADMIN`, `ADMIN`

Returns all promotion records for a specific class (who was promoted, held back, or graduated from this class).

**Response:** Same structure as above but for all students in the class (paginated).

---

## Enums Reference

### PromotionType (used in responses and overrides)

| Value | Description |
|---|---|
| `PROMOTED` | Student moved to the next class |
| `GRADUATED` | Student completed the terminal class |
| `HELD_BACK` | Student remains in the current class |

### Recommendation (preview only)

| Value | Description |
|---|---|
| `PROMOTE` | System recommends promotion (passed all finals) |
| `HOLD_BACK` | System recommends holding back (failed 1+ finals) |
| `GRADUATE` | Terminal class — student should graduate |

---

## Frontend UI Suggestions

### Class Progression Setup Screen
- Branch selector → Show current progression (if configured) or empty state
- Drag-and-drop or ordered list to define class names
- Checkbox to mark the terminal/graduation class
- This is a one-time setup per branch, updatable anytime

### Promotion Screen Flow
1. **Select source:** Branch → Class → Academic Year dropdown
2. **Click "Preview Promotion"** → calls the preview endpoint
3. **Review table:**
   - Columns: Student Name, ARK ID, Roll Number, Exam Summary, Recommendation, Action
   - Color coding: Green = PROMOTE, Red = HOLD_BACK, Blue = GRADUATE, Grey = NO_DATA
   - Each row has an action dropdown (Promote / Hold Back / Graduate) pre-filled with the recommendation
   - Admin can change any individual student's action
4. **Enter target academic year** (e.g., "2026-2027")
5. **Click "Execute Promotion"** → calls execute endpoint with any overrides
6. **Show result summary:** X promoted, Y graduated, Z held back

### Promotion History
- On student detail page → "Promotion History" tab showing year-by-year trail
- On class detail page → "Promotion Records" tab showing all students and their outcomes

---

## API Endpoint Summary

| Method | Endpoint | Description | Roles |
|---|---|---|---|
| PUT | `/api/v1/organizations/{orgId}/branches/{branchId}/class-progression` | Set class order | ORG_ADMIN |
| GET | `/api/v1/organizations/{orgId}/branches/{branchId}/class-progression` | Get class order | ORG_ADMIN, ADMIN |
| GET | `/api/v1/organizations/{orgId}/branches/{branchId}/promotions/preview?sourceClassId=&targetAcademicYear=` | Preview candidates | ORG_ADMIN, ADMIN |
| POST | `/api/v1/organizations/{orgId}/branches/{branchId}/promotions/execute` | Execute promotion | ORG_ADMIN, ADMIN |
| GET | `/api/v1/organizations/{orgId}/branches/{branchId}/promotions/history/student/{studentId}` | Student history | ORG_ADMIN, ADMIN, USER |
| GET | `/api/v1/organizations/{orgId}/branches/{branchId}/promotions/history/class/{classId}` | Class history | ORG_ADMIN, ADMIN |

---

## Important Notes for Frontend

1. **Class Progression must be configured before any promotion.** If not configured, preview/execute will return `400`.
2. **`className` in progression must exactly match the `name` field in AcademicClass.** For example, if classes are created as "Grade 10", the progression must say "Grade 10" (not "10th Grade").
3. **Sections are handled separately.** The progression defines class name order only (Grade 9 → Grade 10). Sections (A, B, C) are specified at promotion time via `targetSection`.
4. **Target class is auto-created** if it doesn't exist for the new academic year. No need to manually create classes before promoting.
5. **Only ACTIVE students appear in preview.** Students with status INACTIVE, TRANSFERRED, DROPPED, or GRADUATED are automatically excluded.
6. **Only FINAL exam results are considered** for the recommendation. Midterm, unit test, and quarterly exam results are ignored.
7. **Promotion is irreversible in MVP.** There is no undo endpoint. Admin should review carefully before executing.
8. **Duplicate prevention:** A student cannot be promoted from the same source class twice.