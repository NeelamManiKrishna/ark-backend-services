package com.app.ark_backend_services.controller;

import com.app.ark_backend_services.dto.CreateStudentRequest;
import com.app.ark_backend_services.dto.StudentResponse;
import com.app.ark_backend_services.dto.UpdateStudentRequest;
import com.app.ark_backend_services.service.StudentService;
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

@RestController
@RequestMapping("/api/v1/organizations/{organizationId}/students")
@RequiredArgsConstructor
@Tag(name = "Students", description = "Manage student records within an organization. SUPER_ADMIN, ORG_ADMIN, and ADMIN can create/update/delete.")
public class StudentController {

    private final StudentService studentService;

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORG_ADMIN', 'ADMIN')")
    @Operation(summary = "Create student", description = "Creates a new student in the organization. Requires SUPER_ADMIN, ORG_ADMIN, or ADMIN role.")
    @ApiResponse(responseCode = "201", description = "Student created")
    @ApiResponse(responseCode = "404", description = "Organization, branch, or class not found")
    @ApiResponse(responseCode = "409", description = "Student with same roll number already exists")
    public ResponseEntity<StudentResponse> create(
            @Parameter(description = "Organization ID") @PathVariable String organizationId,
            @Valid @RequestBody CreateStudentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(studentService.create(organizationId, request));
    }

    @GetMapping("/{studentId}")
    @Operation(summary = "Get student by ID", description = "Returns a single student scoped to the organization.")
    @ApiResponse(responseCode = "200", description = "Student found")
    @ApiResponse(responseCode = "404", description = "Student not found")
    public ResponseEntity<StudentResponse> getById(
            @Parameter(description = "Organization ID") @PathVariable String organizationId,
            @Parameter(description = "Student ID") @PathVariable String studentId) {
        return ResponseEntity.ok(studentService.getById(organizationId, studentId));
    }

    @GetMapping
    @Operation(summary = "List students", description = "Returns a paginated list of students. Optionally filter by branchId or classId query parameter.")
    @ApiResponse(responseCode = "200", description = "Students retrieved")
    public ResponseEntity<Page<StudentResponse>> getAll(
            @Parameter(description = "Organization ID") @PathVariable String organizationId,
            @Parameter(description = "Filter by branch ID") @RequestParam(required = false) String branchId,
            @Parameter(description = "Filter by class ID") @RequestParam(required = false) String classId,
            @SortDefault(sort = "firstName", direction = Sort.Direction.ASC) Pageable pageable) {
        if (classId != null) {
            return ResponseEntity.ok(studentService.getAllByClass(organizationId, classId, pageable));
        }
        if (branchId != null) {
            return ResponseEntity.ok(studentService.getAllByBranch(organizationId, branchId, pageable));
        }
        return ResponseEntity.ok(studentService.getAllByOrganization(organizationId, pageable));
    }

    @PutMapping("/{studentId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORG_ADMIN', 'ADMIN')")
    @Operation(summary = "Update student", description = "Partially updates a student record. Only non-null fields are applied.")
    @ApiResponse(responseCode = "200", description = "Student updated")
    @ApiResponse(responseCode = "404", description = "Student not found")
    @ApiResponse(responseCode = "409", description = "Roll number already exists")
    public ResponseEntity<StudentResponse> update(
            @Parameter(description = "Organization ID") @PathVariable String organizationId,
            @Parameter(description = "Student ID") @PathVariable String studentId,
            @Valid @RequestBody UpdateStudentRequest request) {
        return ResponseEntity.ok(studentService.update(organizationId, studentId, request));
    }

    @DeleteMapping("/{studentId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORG_ADMIN', 'ADMIN')")
    @Operation(summary = "Delete student", description = "Deletes a student record. Requires SUPER_ADMIN, ORG_ADMIN, or ADMIN role.")
    @ApiResponse(responseCode = "204", description = "Student deleted")
    @ApiResponse(responseCode = "404", description = "Student not found")
    public ResponseEntity<Void> delete(
            @Parameter(description = "Organization ID") @PathVariable String organizationId,
            @Parameter(description = "Student ID") @PathVariable String studentId) {
        studentService.delete(organizationId, studentId);
        return ResponseEntity.noContent().build();
    }
}
