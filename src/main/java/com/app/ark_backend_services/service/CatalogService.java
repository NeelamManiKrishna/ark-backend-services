package com.app.ark_backend_services.service;

import com.app.ark_backend_services.dto.AcademicYearDtos;
import com.app.ark_backend_services.dto.SubjectDtos;
import com.app.ark_backend_services.exception.DuplicateResourceException;
import com.app.ark_backend_services.exception.ResourceNotFoundException;
import com.app.ark_backend_services.model.AcademicYear;
import com.app.ark_backend_services.model.AuditLog.Action;
import com.app.ark_backend_services.model.Subject;
import com.app.ark_backend_services.repository.AcademicClassRepository;
import com.app.ark_backend_services.repository.AcademicYearRepository;
import com.app.ark_backend_services.repository.OrganizationRepository;
import com.app.ark_backend_services.repository.SubjectRepository;
import com.app.ark_backend_services.security.AccessPolicy;
import com.app.ark_backend_services.util.AcademicYears;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

import java.util.List;

/** Organization-level catalogs: academic years and subjects. Readable by every role, writable by org admins. */
@Service
@RequiredArgsConstructor
public class CatalogService {

    private final AcademicYearRepository yearRepository;
    private final SubjectRepository subjectRepository;
    private final AcademicClassRepository classRepository;
    private final OrganizationRepository organizationRepository;
    private final SearchService searchService;
    private final AuditLogService auditLogService;

    // ---------------- academic years ----------------

    public AcademicYearDtos.Response createYear(String organizationId, AcademicYearDtos.CreateRequest request) {
        AccessPolicy.assertOrg(organizationId);
        requireOrg(organizationId);
        AcademicYears.validate(request.getLabel());
        checkDates(request.getStartDate(), request.getEndDate());
        if (yearRepository.existsByOrganizationIdAndLabel(organizationId, request.getLabel())) {
            throw new DuplicateResourceException("Academic year " + request.getLabel() + " already exists");
        }
        AcademicYear year = new AcademicYear();
        year.setOrganizationId(organizationId);
        year.setLabel(request.getLabel());
        year.setStartDate(request.getStartDate());
        year.setEndDate(request.getEndDate());
        boolean makeCurrent = Boolean.TRUE.equals(request.getCurrent());
        if (makeCurrent) {
            clearCurrent(organizationId);
        }
        year.setCurrent(makeCurrent);
        year = yearRepository.save(year);
        auditLogService.log(Action.CREATE, "AcademicYear", year.getId(), year.getLabel(), organizationId, "Academic year created");
        return AcademicYearDtos.Response.from(year);
    }

    public Page<AcademicYearDtos.Response> listYears(String organizationId, String q, Pageable pageable) {
        AccessPolicy.assertOrg(organizationId);
        return searchService.search(AcademicYear.class, Criteria.where("organizationId").is(organizationId), q,
                List.of("label"), pageable, AcademicYearDtos.Response::from);
    }

    public AcademicYearDtos.Response getYear(String organizationId, String id) {
        AccessPolicy.assertOrg(organizationId);
        return AcademicYearDtos.Response.from(loadYear(organizationId, id));
    }

    public AcademicYearDtos.Response updateYear(String organizationId, String id, AcademicYearDtos.UpdateRequest request) {
        AccessPolicy.assertOrg(organizationId);
        AcademicYear year = loadYear(organizationId, id);
        if (request.getStartDate() != null) year.setStartDate(request.getStartDate());
        if (request.getEndDate() != null) year.setEndDate(request.getEndDate());
        checkDates(year.getStartDate(), year.getEndDate());
        if (request.getCurrent() != null) {
            if (request.getCurrent()) {
                clearCurrent(organizationId);
            }
            year.setCurrent(request.getCurrent());
        }
        year = yearRepository.save(year);
        auditLogService.log(Action.UPDATE, "AcademicYear", id, year.getLabel(), organizationId, "Academic year updated");
        return AcademicYearDtos.Response.from(year);
    }

    public void deleteYear(String organizationId, String id) {
        AccessPolicy.assertOrg(organizationId);
        AcademicYear year = loadYear(organizationId, id);
        if (classRepository.existsByOrganizationIdAndAcademicYear(organizationId, year.getLabel())) {
            throw new IllegalStateException("Academic year " + year.getLabel() + " is used by classes and cannot be deleted");
        }
        yearRepository.delete(year);
        auditLogService.log(Action.DELETE, "AcademicYear", id, year.getLabel(), organizationId, "Academic year deleted");
    }

    private AcademicYear loadYear(String organizationId, String id) {
        return yearRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Academic year not found: " + id));
    }

    private void clearCurrent(String organizationId) {
        List<AcademicYear> currents = yearRepository.findByOrganizationIdAndCurrentTrue(organizationId);
        currents.forEach(y -> y.setCurrent(false));
        yearRepository.saveAll(currents);
    }

    private static void checkDates(java.time.LocalDate start, java.time.LocalDate end) {
        if (start != null && end != null && !start.isBefore(end)) {
            throw new IllegalArgumentException("Start date must be before end date");
        }
    }

    // ---------------- subjects ----------------

    public SubjectDtos.Response createSubject(String organizationId, SubjectDtos.CreateRequest request) {
        AccessPolicy.assertOrg(organizationId);
        requireOrg(organizationId);
        String code = request.getCode().trim().toUpperCase();
        if (subjectRepository.existsByOrganizationIdAndCode(organizationId, code)) {
            throw new DuplicateResourceException("Subject code " + code + " already exists");
        }
        Subject s = new Subject();
        s.setOrganizationId(organizationId);
        s.setCode(code);
        s.setName(request.getName().trim());
        s = subjectRepository.save(s);
        auditLogService.log(Action.CREATE, "Subject", s.getId(), s.getName(), organizationId, "Subject created: " + code);
        return SubjectDtos.Response.from(s);
    }

    public Page<SubjectDtos.Response> listSubjects(String organizationId, String q, Pageable pageable) {
        AccessPolicy.assertOrg(organizationId);
        return searchService.search(Subject.class, Criteria.where("organizationId").is(organizationId), q,
                List.of("code", "name"), pageable, SubjectDtos.Response::from);
    }

    public SubjectDtos.Response updateSubject(String organizationId, String id, SubjectDtos.UpdateRequest request) {
        AccessPolicy.assertOrg(organizationId);
        Subject s = subjectRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found: " + id));
        if (request.getCode() != null) {
            String code = request.getCode().trim().toUpperCase();
            if (!code.equals(s.getCode()) && subjectRepository.existsByOrganizationIdAndCode(organizationId, code)) {
                throw new DuplicateResourceException("Subject code " + code + " already exists");
            }
            s.setCode(code);
        }
        if (request.getName() != null) s.setName(request.getName().trim());
        s = subjectRepository.save(s);
        auditLogService.log(Action.UPDATE, "Subject", id, s.getName(), organizationId, "Subject updated");
        return SubjectDtos.Response.from(s);
    }

    public void deleteSubject(String organizationId, String id) {
        AccessPolicy.assertOrg(organizationId);
        Subject s = subjectRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found: " + id));
        subjectRepository.delete(s);
        auditLogService.log(Action.DELETE, "Subject", id, s.getName(), organizationId, "Subject deleted");
    }

    private void requireOrg(String organizationId) {
        if (!organizationRepository.existsById(organizationId)) {
            throw new ResourceNotFoundException("Organization not found with id: " + organizationId);
        }
    }
}
