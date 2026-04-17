package com.app.ark_backend_services.controller;

import com.app.ark_backend_services.dto.*;
import com.app.ark_backend_services.service.ExaminationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.SortDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/organizations/{organizationId}")
@RequiredArgsConstructor
@Tag(name = "Examinations", description = "Manage examinations, exam subjects, and exam results. Exams are scoped to a branch and academic year.")
public class ExaminationController {

    private final ExaminationService examinationService;

    // ===================== Examination endpoints =====================

    @PostMapping("/branches/{branchId}/examinations")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORG_ADMIN', 'ADMIN')")
    @Operation(summary = "Create examination", description = "Creates a new examination under a branch for a specific academic year. Requires SUPER_ADMIN, ORG_ADMIN, or ADMIN role.")
    @ApiResponse(responseCode = "201", description = "Examination created")
    @ApiResponse(responseCode = "404", description = "Organization or branch not found")
    @ApiResponse(responseCode = "409", description = "Examination with same name already exists for this branch and academic year")
    public ResponseEntity<ExaminationResponse> createExamination(
            @Parameter(description = "Organization ID") @PathVariable String organizationId,
            @Parameter(description = "Branch ID") @PathVariable String branchId,
            @Valid @RequestBody CreateExaminationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(examinationService.createExamination(organizationId, branchId, request));
    }

    @GetMapping("/examinations/{examId}")
    @Operation(summary = "Get examination by ID", description = "Returns a single examination scoped to the organization.")
    @ApiResponse(responseCode = "200", description = "Examination found")
    @ApiResponse(responseCode = "404", description = "Examination not found")
    public ResponseEntity<ExaminationResponse> getExamination(
            @Parameter(description = "Organization ID") @PathVariable String organizationId,
            @Parameter(description = "Examination ID") @PathVariable String examId) {
        return ResponseEntity.ok(examinationService.getExaminationById(organizationId, examId));
    }

    @GetMapping("/branches/{branchId}/examinations")
    @Operation(summary = "List examinations by branch", description = "Returns a paginated list of examinations for a branch. Optionally filter by academicYear query parameter.")
    @ApiResponse(responseCode = "200", description = "Examinations retrieved")
    public ResponseEntity<Page<ExaminationResponse>> getExaminationsByBranch(
            @Parameter(description = "Organization ID") @PathVariable String organizationId,
            @Parameter(description = "Branch ID") @PathVariable String branchId,
            @Parameter(description = "Filter by academic year (e.g., 2025-2026)") @RequestParam(required = false) String academicYear,
            @SortDefault(sort = "startDate", direction = Sort.Direction.DESC) Pageable pageable) {
        if (academicYear != null) {
            return ResponseEntity.ok(examinationService.getExaminationsByBranchAndYear(organizationId, branchId, academicYear, pageable));
        }
        return ResponseEntity.ok(examinationService.getExaminationsByBranch(organizationId, branchId, pageable));
    }

    @PutMapping("/examinations/{examId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORG_ADMIN', 'ADMIN')")
    @Operation(summary = "Update examination", description = "Partially updates an examination. Only non-null fields are applied.")
    @ApiResponse(responseCode = "200", description = "Examination updated")
    @ApiResponse(responseCode = "404", description = "Examination not found")
    public ResponseEntity<ExaminationResponse> updateExamination(
            @Parameter(description = "Organization ID") @PathVariable String organizationId,
            @Parameter(description = "Examination ID") @PathVariable String examId,
            @Valid @RequestBody UpdateExaminationRequest request) {
        return ResponseEntity.ok(examinationService.updateExamination(organizationId, examId, request));
    }

    @DeleteMapping("/examinations/{examId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORG_ADMIN', 'ADMIN')")
    @Operation(summary = "Delete examination", description = "Deletes an examination and all its subjects and results (cascade). Requires SUPER_ADMIN, ORG_ADMIN, or ADMIN role.")
    @ApiResponse(responseCode = "204", description = "Examination deleted")
    @ApiResponse(responseCode = "404", description = "Examination not found")
    public ResponseEntity<Void> deleteExamination(
            @Parameter(description = "Organization ID") @PathVariable String organizationId,
            @Parameter(description = "Examination ID") @PathVariable String examId) {
        examinationService.deleteExamination(organizationId, examId);
        return ResponseEntity.noContent().build();
    }

    // ===================== Exam Subject endpoints =====================

    @PostMapping("/examinations/{examId}/subjects")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORG_ADMIN', 'ADMIN')")
    @Operation(summary = "Create exam subject", description = "Adds a subject to an examination for a specific class. Validates that the class belongs to the exam's branch. Passing marks must not exceed max marks.")
    @ApiResponse(responseCode = "201", description = "Exam subject created")
    @ApiResponse(responseCode = "404", description = "Examination or class not found")
    @ApiResponse(responseCode = "409", description = "Subject already exists for this exam and class")
    public ResponseEntity<ExamSubjectResponse> createExamSubject(
            @Parameter(description = "Organization ID") @PathVariable String organizationId,
            @Parameter(description = "Examination ID") @PathVariable String examId,
            @Valid @RequestBody CreateExamSubjectRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(examinationService.createExamSubject(organizationId, examId, request));
    }

    @GetMapping("/examinations/{examId}/subjects")
    @Operation(summary = "List exam subjects", description = "Returns a paginated list of subjects for an examination.")
    @ApiResponse(responseCode = "200", description = "Exam subjects retrieved")
    @ApiResponse(responseCode = "404", description = "Examination not found")
    public ResponseEntity<Page<ExamSubjectResponse>> getExamSubjects(
            @Parameter(description = "Organization ID") @PathVariable String organizationId,
            @Parameter(description = "Examination ID") @PathVariable String examId,
            @SortDefault(sort = "subjectName", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(examinationService.getExamSubjects(organizationId, examId, pageable));
    }

    @PutMapping("/examinations/{examId}/subjects/{subjectId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORG_ADMIN', 'ADMIN')")
    @Operation(summary = "Update exam subject", description = "Partially updates an exam subject. Only non-null fields are applied.")
    @ApiResponse(responseCode = "200", description = "Exam subject updated")
    @ApiResponse(responseCode = "404", description = "Examination or subject not found")
    public ResponseEntity<ExamSubjectResponse> updateExamSubject(
            @Parameter(description = "Organization ID") @PathVariable String organizationId,
            @Parameter(description = "Examination ID") @PathVariable String examId,
            @Parameter(description = "Exam Subject ID") @PathVariable String subjectId,
            @Valid @RequestBody UpdateExamSubjectRequest request) {
        return ResponseEntity.ok(examinationService.updateExamSubject(organizationId, examId, subjectId, request));
    }

    @DeleteMapping("/examinations/{examId}/subjects/{subjectId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORG_ADMIN', 'ADMIN')")
    @Operation(summary = "Delete exam subject", description = "Deletes an exam subject and all its results (cascade).")
    @ApiResponse(responseCode = "204", description = "Exam subject deleted")
    @ApiResponse(responseCode = "404", description = "Examination or subject not found")
    public ResponseEntity<Void> deleteExamSubject(
            @Parameter(description = "Organization ID") @PathVariable String organizationId,
            @Parameter(description = "Examination ID") @PathVariable String examId,
            @Parameter(description = "Exam Subject ID") @PathVariable String subjectId) {
        examinationService.deleteExamSubject(organizationId, examId, subjectId);
        return ResponseEntity.noContent().build();
    }

    // ===================== Exam Result endpoints =====================

    @PostMapping("/examinations/{examId}/subjects/{subjectId}/results")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORG_ADMIN', 'ADMIN')")
    @Operation(summary = "Create exam result", description = "Records a student's result for an exam subject. Auto-calculates grade (A+ to F) and pass/fail status based on marks and passing threshold.")
    @ApiResponse(responseCode = "201", description = "Exam result created")
    @ApiResponse(responseCode = "404", description = "Examination, subject, or student not found")
    @ApiResponse(responseCode = "400", description = "Marks exceed max marks")
    @ApiResponse(responseCode = "409", description = "Result already exists for this student and subject")
    public ResponseEntity<ExamResultResponse> createExamResult(
            @Parameter(description = "Organization ID") @PathVariable String organizationId,
            @Parameter(description = "Examination ID") @PathVariable String examId,
            @Parameter(description = "Exam Subject ID") @PathVariable String subjectId,
            @Valid @RequestBody CreateExamResultRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(examinationService.createExamResult(organizationId, examId, subjectId, request));
    }

    @GetMapping("/examinations/{examId}/subjects/{subjectId}/results")
    @Operation(summary = "List results by subject", description = "Returns a paginated list of exam results for a specific subject.")
    @ApiResponse(responseCode = "200", description = "Exam results retrieved")
    @ApiResponse(responseCode = "404", description = "Examination not found")
    public ResponseEntity<Page<ExamResultResponse>> getResultsBySubject(
            @Parameter(description = "Organization ID") @PathVariable String organizationId,
            @Parameter(description = "Examination ID") @PathVariable String examId,
            @Parameter(description = "Exam Subject ID") @PathVariable String subjectId,
            @SortDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(examinationService.getResultsBySubject(organizationId, examId, subjectId, pageable));
    }

    @GetMapping("/examinations/{examId}/classes/{classId}/results")
    @Operation(summary = "Get class results", description = "Returns all results for a class across all subjects in an examination. Useful for class teachers to view their class report.")
    @ApiResponse(responseCode = "200", description = "Class results retrieved")
    @ApiResponse(responseCode = "404", description = "Examination not found")
    public ResponseEntity<Page<ExamResultResponse>> getResultsByClass(
            @Parameter(description = "Organization ID") @PathVariable String organizationId,
            @Parameter(description = "Examination ID") @PathVariable String examId,
            @Parameter(description = "Class ID") @PathVariable String classId,
            @SortDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(examinationService.getResultsByClass(organizationId, examId, classId, pageable));
    }

    @GetMapping("/examinations/{examId}/students/{studentId}/results")
    @Operation(summary = "Get student's exam results", description = "Returns all results for a student across all subjects in an examination.")
    @ApiResponse(responseCode = "200", description = "Student results retrieved")
    @ApiResponse(responseCode = "404", description = "Examination not found")
    public ResponseEntity<List<ExamResultResponse>> getStudentResults(
            @Parameter(description = "Organization ID") @PathVariable String organizationId,
            @Parameter(description = "Examination ID") @PathVariable String examId,
            @Parameter(description = "Student ID") @PathVariable String studentId) {
        return ResponseEntity.ok(examinationService.getStudentResults(organizationId, examId, studentId));
    }

    @PutMapping("/examinations/{examId}/results/{resultId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORG_ADMIN', 'ADMIN')")
    @Operation(summary = "Update exam result", description = "Updates an exam result. If marks are changed, grade and pass/fail status are auto-recalculated.")
    @ApiResponse(responseCode = "200", description = "Exam result updated")
    @ApiResponse(responseCode = "404", description = "Examination or result not found")
    @ApiResponse(responseCode = "400", description = "Marks exceed max marks")
    public ResponseEntity<ExamResultResponse> updateExamResult(
            @Parameter(description = "Organization ID") @PathVariable String organizationId,
            @Parameter(description = "Examination ID") @PathVariable String examId,
            @Parameter(description = "Exam Result ID") @PathVariable String resultId,
            @Valid @RequestBody UpdateExamResultRequest request) {
        return ResponseEntity.ok(examinationService.updateExamResult(organizationId, examId, resultId, request));
    }
}
