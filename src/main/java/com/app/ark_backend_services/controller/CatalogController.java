package com.app.ark_backend_services.controller;

import com.app.ark_backend_services.dto.AcademicYearDtos;
import com.app.ark_backend_services.dto.SubjectDtos;
import com.app.ark_backend_services.service.CatalogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
@RequestMapping("/api/v1/organizations/{organizationId}")
@RequiredArgsConstructor
@Tag(name = "Catalogs", description = "Academic years and subjects of an organization. Readable by all roles, writable by SUPER_ADMIN and ORG_ADMIN.")
public class CatalogController {

    private static final String WRITE = "hasAnyRole('SUPER_ADMIN', 'ORG_ADMIN')";

    private final CatalogService catalogService;

    // ---- academic years ----

    @PostMapping("/academic-years")
    @PreAuthorize(WRITE)
    @Operation(summary = "Create academic year", description = "Label must be YYYY-YYYY with consecutive years. Setting current=true clears the flag on the others.")
    public ResponseEntity<AcademicYearDtos.Response> createYear(
            @Parameter(description = "Organization ID") @PathVariable String organizationId,
            @Valid @RequestBody AcademicYearDtos.CreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(catalogService.createYear(organizationId, request));
    }

    @GetMapping("/academic-years")
    @Operation(summary = "List academic years", description = "Paginated; q matches the label prefix.")
    public ResponseEntity<Page<AcademicYearDtos.Response>> listYears(
            @Parameter(description = "Organization ID") @PathVariable String organizationId,
            @RequestParam(required = false) String q,
            @SortDefault(sort = "label", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(catalogService.listYears(organizationId, q, pageable));
    }

    @GetMapping("/academic-years/{id}")
    @Operation(summary = "Get academic year")
    public ResponseEntity<AcademicYearDtos.Response> getYear(@PathVariable String organizationId, @PathVariable String id) {
        return ResponseEntity.ok(catalogService.getYear(organizationId, id));
    }

    @PutMapping("/academic-years/{id}")
    @PreAuthorize(WRITE)
    @Operation(summary = "Update academic year", description = "Dates and the current flag can change; the label cannot.")
    public ResponseEntity<AcademicYearDtos.Response> updateYear(
            @PathVariable String organizationId, @PathVariable String id,
            @Valid @RequestBody AcademicYearDtos.UpdateRequest request) {
        return ResponseEntity.ok(catalogService.updateYear(organizationId, id, request));
    }

    @DeleteMapping("/academic-years/{id}")
    @PreAuthorize(WRITE)
    @Operation(summary = "Delete academic year", description = "Rejected while classes use the year.")
    public ResponseEntity<Void> deleteYear(@PathVariable String organizationId, @PathVariable String id) {
        catalogService.deleteYear(organizationId, id);
        return ResponseEntity.noContent().build();
    }

    // ---- subjects ----

    @PostMapping("/subjects")
    @PreAuthorize(WRITE)
    @Operation(summary = "Create subject", description = "Codes are unique per organization and stored upper-case.")
    public ResponseEntity<SubjectDtos.Response> createSubject(
            @PathVariable String organizationId, @Valid @RequestBody SubjectDtos.CreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(catalogService.createSubject(organizationId, request));
    }

    @GetMapping("/subjects")
    @Operation(summary = "List subjects", description = "Paginated; q matches the code or name prefix.")
    public ResponseEntity<Page<SubjectDtos.Response>> listSubjects(
            @PathVariable String organizationId, @RequestParam(required = false) String q,
            @SortDefault(sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(catalogService.listSubjects(organizationId, q, pageable));
    }

    @PutMapping("/subjects/{id}")
    @PreAuthorize(WRITE)
    @Operation(summary = "Update subject")
    public ResponseEntity<SubjectDtos.Response> updateSubject(
            @PathVariable String organizationId, @PathVariable String id,
            @Valid @RequestBody SubjectDtos.UpdateRequest request) {
        return ResponseEntity.ok(catalogService.updateSubject(organizationId, id, request));
    }

    @DeleteMapping("/subjects/{id}")
    @PreAuthorize(WRITE)
    @Operation(summary = "Delete subject")
    public ResponseEntity<Void> deleteSubject(@PathVariable String organizationId, @PathVariable String id) {
        catalogService.deleteSubject(organizationId, id);
        return ResponseEntity.noContent().build();
    }
}
