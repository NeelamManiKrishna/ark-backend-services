package com.app.ark_backend_services.service;

import com.app.ark_backend_services.exception.ResourceNotFoundException;
import com.app.ark_backend_services.model.AcademicYear;
import com.app.ark_backend_services.model.Subject;
import com.app.ark_backend_services.repository.AcademicYearRepository;
import com.app.ark_backend_services.repository.SubjectRepository;
import com.app.ark_backend_services.util.AcademicYears;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Lets requests identify an academic year or subject either by catalog id or by the legacy text.
 * Whatever the caller sends, the resolved text is what existing code keeps using, and the catalog id is
 * stored next to it when the catalog knows the entry.
 */
@Service
@RequiredArgsConstructor
public class CatalogResolver {

    public record Year(String id, String label) {}

    public record SubjectRef(String id, String name) {}

    private final AcademicYearRepository yearRepository;
    private final SubjectRepository subjectRepository;

    /** Academic year by id and/or label. One of them is required; both must agree when both are sent. */
    public Year year(String organizationId, String id, String label) {
        boolean hasId = id != null && !id.isBlank();
        boolean hasLabel = label != null && !label.isBlank();
        if (!hasId && !hasLabel) {
            throw new IllegalArgumentException("Academic year is required (send academicYear or academicYearId)");
        }
        if (hasId) {
            AcademicYear year = yearRepository.findByIdAndOrganizationId(id, organizationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Academic year not found: " + id));
            if (hasLabel && !year.getLabel().equals(label)) {
                throw new IllegalArgumentException("academicYear '" + label + "' does not match academicYearId (" + year.getLabel() + ")");
            }
            return new Year(year.getId(), year.getLabel());
        }
        AcademicYears.validate(label);
        return new Year(yearRepository.findByOrganizationIdAndLabel(organizationId, label).map(AcademicYear::getId).orElse(null), label);
    }

    /** Subject by id and/or name. When neither is sent the result is empty (or an error if required). */
    public SubjectRef subject(String organizationId, String id, String name, boolean required) {
        boolean hasId = id != null && !id.isBlank();
        boolean hasName = name != null && !name.isBlank();
        if (!hasId && !hasName) {
            if (required) {
                throw new IllegalArgumentException("Subject is required (send subjectName or subjectId)");
            }
            return new SubjectRef(null, name);
        }
        if (hasId) {
            Subject s = subjectRepository.findByIdAndOrganizationId(id, organizationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Subject not found: " + id));
            if (hasName && !s.getName().equals(name)) {
                throw new IllegalArgumentException("subjectName '" + name + "' does not match subjectId (" + s.getName() + ")");
            }
            return new SubjectRef(s.getId(), s.getName());
        }
        return new SubjectRef(subjectRepository.findFirstByOrganizationIdAndNameIgnoreCase(organizationId, name)
                .map(Subject::getId).orElse(null), name);
    }
}
