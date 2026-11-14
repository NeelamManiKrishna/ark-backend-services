package com.app.ark_backend_services.service;

import com.app.ark_backend_services.dto.*;
import com.app.ark_backend_services.exception.DuplicateResourceException;
import com.app.ark_backend_services.exception.ResourceNotFoundException;
import com.app.ark_backend_services.model.Examination;
import com.app.ark_backend_services.model.Examination.ExamStatus;
import com.app.ark_backend_services.model.ExamSubject;
import com.app.ark_backend_services.model.ExamSubject.ExamSubjectStatus;
import com.app.ark_backend_services.model.ExamResult;
import com.app.ark_backend_services.model.AuditLog.Action;
import com.app.ark_backend_services.model.StudentEnrollment;
import com.app.ark_backend_services.repository.BranchRepository;
import com.app.ark_backend_services.repository.AcademicClassRepository;
import com.app.ark_backend_services.repository.ExaminationRepository;
import com.app.ark_backend_services.repository.ExamSubjectRepository;
import com.app.ark_backend_services.repository.ExamResultRepository;
import com.app.ark_backend_services.repository.StudentRepository;
import com.app.ark_backend_services.repository.StudentEnrollmentRepository;
import com.app.ark_backend_services.security.AccessPolicy;
import com.app.ark_backend_services.util.AcademicYears;
import com.app.ark_backend_services.util.ArkIdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ExaminationService {

    private final ExaminationRepository examinationRepository;
    private final ExamSubjectRepository examSubjectRepository;
    private final ExamResultRepository examResultRepository;
    private final BranchRepository branchRepository;
    private final AcademicClassRepository academicClassRepository;
    private final StudentRepository studentRepository;
    private final StudentEnrollmentRepository enrollmentRepository;
    private final AuditLogService auditLogService;
    private final CatalogResolver catalogResolver;

    // ===================== Examination CRUD =====================

    public ExaminationResponse createExamination(String organizationId, String branchId, CreateExaminationRequest request) {
        AccessPolicy.assertOrg(organizationId);
        AccessPolicy.assertBranch(branchId);
        validateBranchExists(organizationId, branchId);

        var year = catalogResolver.year(organizationId, request.getAcademicYearId(), request.getAcademicYear());
        request.setAcademicYear(year.label());

        if (examinationRepository.existsByOrganizationIdAndBranchIdAndAcademicYearAndName(
                organizationId, branchId, request.getAcademicYear(), request.getName())) {
            throw new DuplicateResourceException("Examination '" + request.getName()
                    + "' already exists for academic year " + request.getAcademicYear() + " in this branch");
        }

        if (request.getStartDate() != null && request.getEndDate() != null
                && request.getStartDate().isAfter(request.getEndDate())) {
            throw new IllegalArgumentException("Start date cannot be after end date");
        }

        Examination exam = new Examination();
        exam.setArkId(ArkIdGenerator.generateExamId());
        exam.setOrganizationId(organizationId);
        exam.setBranchId(branchId);
        exam.setName(request.getName());
        exam.setAcademicYear(request.getAcademicYear());
        exam.setAcademicYearId(year.id());
        exam.setExamType(request.getExamType());
        exam.setStartDate(request.getStartDate());
        exam.setEndDate(request.getEndDate());
        exam.setDescription(request.getDescription());
        exam.setStatus(ExamStatus.SCHEDULED);

        ExaminationResponse response = ExaminationResponse.from(examinationRepository.save(exam));
        auditLogService.log(Action.CREATE, "Examination", response.getId(), exam.getName(), organizationId,
                "Examination created: " + exam.getName() + " (" + exam.getAcademicYear() + ")");
        return response;
    }

    public ExaminationResponse getExaminationById(String organizationId, String examId) {
        AccessPolicy.assertOrg(organizationId);
        Examination exam = loadExam(organizationId, examId);
        return ExaminationResponse.from(exam);
    }

    public Page<ExaminationResponse> getExaminationsByBranch(String organizationId, String branchId, Pageable pageable) {
        AccessPolicy.assertOrg(organizationId);
        AccessPolicy.assertBranch(branchId);
        return examinationRepository.findByOrganizationIdAndBranchId(organizationId, branchId, pageable)
                .map(ExaminationResponse::from);
    }

    public Page<ExaminationResponse> getExaminationsByBranchAndYear(String organizationId, String branchId, String academicYear, Pageable pageable) {
        AccessPolicy.assertOrg(organizationId);
        AccessPolicy.assertBranch(branchId);
        return examinationRepository.findByOrganizationIdAndBranchIdAndAcademicYear(organizationId, branchId, academicYear, pageable)
                .map(ExaminationResponse::from);
    }

    public ExaminationResponse updateExamination(String organizationId, String examId, UpdateExaminationRequest request) {
        AccessPolicy.assertOrg(organizationId);
        Examination exam = loadExam(organizationId, examId);

        if (request.getName() != null) exam.setName(request.getName());
        if (request.getAcademicYear() != null && !request.getAcademicYear().equals(exam.getAcademicYear())) {
            AcademicYears.validate(request.getAcademicYear());
            if (examSubjectRepository.countByExaminationId(examId) > 0) {
                throw new IllegalStateException("Academic year cannot be changed once the exam has subjects or results");
            }
            exam.setAcademicYear(request.getAcademicYear());
            exam.setAcademicYearId(catalogResolver.year(organizationId, null, request.getAcademicYear()).id());
        }
        if (request.getExamType() != null) exam.setExamType(request.getExamType());
        if (request.getStartDate() != null) exam.setStartDate(request.getStartDate());
        if (request.getEndDate() != null) exam.setEndDate(request.getEndDate());
        if (request.getDescription() != null) exam.setDescription(request.getDescription());
        if (request.getStatus() != null) exam.setStatus(request.getStatus());

        ExaminationResponse response = ExaminationResponse.from(examinationRepository.save(exam));
        auditLogService.log(Action.UPDATE, "Examination", examId, exam.getName(), organizationId, "Examination updated");
        return response;
    }

    public void deleteExamination(String organizationId, String examId) {
        AccessPolicy.assertOrg(organizationId);
        Examination exam = loadExam(organizationId, examId);
        examResultRepository.deleteByExaminationId(examId);
        examSubjectRepository.deleteByExaminationId(examId);
        examinationRepository.delete(exam);
        auditLogService.log(Action.DELETE, "Examination", examId, exam.getName(), organizationId,
                "Examination deleted with subjects and results");
    }

    // ===================== Exam Subject CRUD =====================

    public ExamSubjectResponse createExamSubject(String organizationId, String examId, CreateExamSubjectRequest request) {
        AccessPolicy.assertOrg(organizationId);
        Examination exam = loadExam(organizationId, examId);

        academicClassRepository.findByIdAndOrganizationIdAndBranchId(request.getClassId(), organizationId, exam.getBranchId())
                .orElseThrow(() -> new ResourceNotFoundException("Class not found: " + request.getClassId()));

        var subject = catalogResolver.subject(organizationId, request.getSubjectId(), request.getSubjectName(), true);
        request.setSubjectName(subject.name());

        if (examSubjectRepository.existsByExaminationIdAndSubjectNameAndClassId(examId, request.getSubjectName(), request.getClassId())) {
            throw new DuplicateResourceException("Subject '" + request.getSubjectName() + "' already exists for this exam and class");
        }

        if (request.getPassingMarks() > request.getMaxMarks()) {
            throw new IllegalArgumentException("Passing marks cannot exceed max marks");
        }

        ExamSubject es = new ExamSubject();
        es.setExaminationId(examId);
        es.setOrganizationId(organizationId);
        es.setBranchId(exam.getBranchId());
        es.setClassId(request.getClassId());
        es.setSubjectName(request.getSubjectName());
        es.setSubjectId(subject.id());
        es.setSubjectCode(request.getSubjectCode());
        es.setMaxMarks(request.getMaxMarks());
        es.setPassingMarks(request.getPassingMarks());
        es.setExamDate(request.getExamDate());
        es.setStatus(ExamSubjectStatus.SCHEDULED);

        ExamSubjectResponse response = ExamSubjectResponse.from(examSubjectRepository.save(es));
        auditLogService.log(Action.CREATE, "ExamSubject", response.getId(), es.getSubjectName(), organizationId,
                "Exam subject created: " + es.getSubjectName());
        return response;
    }

    public Page<ExamSubjectResponse> getExamSubjects(String organizationId, String examId, Pageable pageable) {
        AccessPolicy.assertOrg(organizationId);
        loadExam(organizationId, examId);
        return examSubjectRepository.findByExaminationId(examId, pageable).map(ExamSubjectResponse::from);
    }

    public ExamSubjectResponse updateExamSubject(String organizationId, String examId, String subjectId, UpdateExamSubjectRequest request) {
        AccessPolicy.assertOrg(organizationId);
        loadExam(organizationId, examId);
        ExamSubject es = examSubjectRepository.findByIdAndExaminationId(subjectId, examId)
                .orElseThrow(() -> new ResourceNotFoundException("Exam subject not found: " + subjectId));

        if (request.getSubjectName() != null) {
            es.setSubjectName(request.getSubjectName());
            es.setSubjectId(catalogResolver.subject(organizationId, null, request.getSubjectName(), false).id());
        }
        if (request.getSubjectCode() != null) es.setSubjectCode(request.getSubjectCode());
        if (request.getMaxMarks() != null) es.setMaxMarks(request.getMaxMarks());
        if (request.getPassingMarks() != null) es.setPassingMarks(request.getPassingMarks());
        if (request.getExamDate() != null) es.setExamDate(request.getExamDate());
        if (request.getStatus() != null) es.setStatus(request.getStatus());

        // Validate passingMarks <= maxMarks after all updates applied
        if (es.getPassingMarks() > es.getMaxMarks()) {
            throw new IllegalArgumentException("Passing marks (" + es.getPassingMarks() + ") cannot exceed max marks (" + es.getMaxMarks() + ")");
        }

        boolean thresholdsChanged = request.getMaxMarks() != null || request.getPassingMarks() != null;
        ExamSubjectResponse response = ExamSubjectResponse.from(examSubjectRepository.save(es));
        if (thresholdsChanged) {
            int recomputed = recomputeResults(es);
            if (recomputed > 0) {
                auditLogService.log(Action.UPDATE, "ExamResult", subjectId, es.getSubjectName(), organizationId,
                        "Recomputed grade/status of " + recomputed + " results after max/passing marks changed");
            }
        }
        auditLogService.log(Action.UPDATE, "ExamSubject", subjectId, es.getSubjectName(), organizationId, "Exam subject updated");
        return response;
    }

    public void deleteExamSubject(String organizationId, String examId, String subjectId) {
        AccessPolicy.assertOrg(organizationId);
        loadExam(organizationId, examId);
        ExamSubject es = examSubjectRepository.findByIdAndExaminationId(subjectId, examId)
                .orElseThrow(() -> new ResourceNotFoundException("Exam subject not found: " + subjectId));
        examResultRepository.deleteByExamSubjectId(subjectId);
        examSubjectRepository.delete(es);
        auditLogService.log(Action.DELETE, "ExamSubject", subjectId, es.getSubjectName(), organizationId, "Exam subject deleted with results");
    }

    // ===================== Exam Result CRUD =====================

    public ExamResultResponse createExamResult(String organizationId, String examId, String subjectId, CreateExamResultRequest request) {
        AccessPolicy.assertOrg(organizationId);
        Examination exam = loadExam(organizationId, examId);
        ExamSubject es = examSubjectRepository.findByIdAndExaminationId(subjectId, examId)
                .orElseThrow(() -> new ResourceNotFoundException("Exam subject not found: " + subjectId));

        studentRepository.findByIdAndOrganizationId(request.getStudentId(), organizationId)
                .filter(st -> es.getBranchId() != null && es.getBranchId().equals(st.getBranchId()))
                .orElseThrow(() -> new ResourceNotFoundException("Student not found: " + request.getStudentId()));

        // The student must be enrolled, for the EXAM's academic year (not whatever is active today), in the subject's class
        StudentEnrollment yearEnrollment = enrollmentRepository
                .findByOrganizationIdAndStudentIdAndAcademicYear(organizationId, request.getStudentId(), exam.getAcademicYear())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Student has no enrollment for academic year " + exam.getAcademicYear()));
        if (!yearEnrollment.getClassId().equals(es.getClassId())) {
            throw new IllegalArgumentException("Student was enrolled in class " + yearEnrollment.getClassId()
                    + " for " + exam.getAcademicYear() + ", but the exam subject belongs to class " + es.getClassId());
        }

        if (examResultRepository.existsByExamSubjectIdAndStudentId(subjectId, request.getStudentId())) {
            throw new DuplicateResourceException("Result already exists for this student and subject");
        }

        if (request.getMarksObtained() > es.getMaxMarks()) {
            throw new IllegalArgumentException("Marks obtained cannot exceed max marks (" + es.getMaxMarks() + ")");
        }

        String resolvedClassId = yearEnrollment.getClassId();

        ExamResult result = new ExamResult();
        result.setExaminationId(examId);
        result.setExamSubjectId(subjectId);
        result.setOrganizationId(organizationId);
        result.setBranchId(es.getBranchId());
        result.setClassId(resolvedClassId);
        result.setStudentId(request.getStudentId());
        result.setMarksObtained(request.getMarksObtained());
        result.setGrade(calculateGrade(request.getMarksObtained(), es.getMaxMarks()));
        result.setRemarks(request.getRemarks());
        result.setStatus(request.getMarksObtained() >= es.getPassingMarks()
                ? ExamResult.ResultStatus.PASS : ExamResult.ResultStatus.FAIL);

        ExamResultResponse response = ExamResultResponse.from(examResultRepository.save(result));
        auditLogService.log(Action.CREATE, "ExamResult", response.getId(), null, organizationId,
                "Result recorded: student=" + request.getStudentId() + " marks=" + request.getMarksObtained());
        return response;
    }

    public Page<ExamResultResponse> getResultsBySubject(String organizationId, String examId, String subjectId, Pageable pageable) {
        AccessPolicy.assertOrg(organizationId);
        loadExam(organizationId, examId);
        examSubjectRepository.findByIdAndExaminationId(subjectId, examId)
                .orElseThrow(() -> new ResourceNotFoundException("Exam subject not found: " + subjectId));
        return examResultRepository.findByExamSubjectId(subjectId, pageable).map(ExamResultResponse::from);
    }

    public Page<ExamResultResponse> getResultsByClass(String organizationId, String examId, String classId, Pageable pageable) {
        AccessPolicy.assertOrg(organizationId);
        loadExam(organizationId, examId);
        return examResultRepository.findByExaminationIdAndClassId(examId, classId, pageable).map(ExamResultResponse::from);
    }

    public java.util.List<ExamResultResponse> getStudentResults(String organizationId, String examId, String studentId) {
        AccessPolicy.assertOrg(organizationId);
        loadExam(organizationId, examId);
        return examResultRepository.findByExaminationIdAndStudentId(examId, studentId, org.springframework.data.domain.PageRequest.of(0, 200))
                .getContent().stream().map(ExamResultResponse::from).toList();
    }

    public ExamResultResponse updateExamResult(String organizationId, String examId, String resultId, UpdateExamResultRequest request) {
        AccessPolicy.assertOrg(organizationId);
        loadExam(organizationId, examId);
        ExamResult result = examResultRepository.findByIdAndOrganizationId(resultId, organizationId)
                .filter(r -> examId.equals(r.getExaminationId()))
                .orElseThrow(() -> new ResourceNotFoundException("Exam result not found: " + resultId));

        if (request.getMarksObtained() != null) {
            ExamSubject es = examSubjectRepository.findByIdAndExaminationId(result.getExamSubjectId(), examId)
                    .orElseThrow(() -> new ResourceNotFoundException("Exam subject not found"));
            if (request.getMarksObtained() > es.getMaxMarks()) {
                throw new IllegalArgumentException("Marks obtained cannot exceed max marks (" + es.getMaxMarks() + ")");
            }
            result.setMarksObtained(request.getMarksObtained());
            result.setGrade(calculateGrade(request.getMarksObtained(), es.getMaxMarks()));
            result.setStatus(request.getMarksObtained() >= es.getPassingMarks()
                    ? ExamResult.ResultStatus.PASS : ExamResult.ResultStatus.FAIL);
        }
        if (request.getRemarks() != null) result.setRemarks(request.getRemarks());
        if (request.getStatus() != null) result.setStatus(request.getStatus());

        ExamResultResponse response = ExamResultResponse.from(examResultRepository.save(result));
        auditLogService.log(Action.UPDATE, "ExamResult", resultId, null, organizationId, "Exam result updated");
        return response;
    }

    public void deleteExamResult(String organizationId, String examId, String resultId) {
        AccessPolicy.assertOrg(organizationId);
        loadExam(organizationId, examId);
        ExamResult result = examResultRepository.findByIdAndOrganizationId(resultId, organizationId)
                .filter(r -> examId.equals(r.getExaminationId()))
                .orElseThrow(() -> new ResourceNotFoundException("Exam result not found: " + resultId));
        examResultRepository.delete(result);
        auditLogService.log(Action.DELETE, "ExamResult", resultId, null, organizationId,
                "Exam result deleted: student=" + result.getStudentId());
    }

    // ===================== Helpers =====================

    /** Re-derives grade and PASS/FAIL for results of a subject; ABSENT and WITHHELD are left alone. */
    private int recomputeResults(ExamSubject es) {
        java.util.List<ExamResult> existing = examResultRepository.findByExamSubjectId(es.getId());
        java.util.List<ExamResult> changed = new java.util.ArrayList<>();
        for (ExamResult r : existing) {
            if (r.getMarksObtained() == null
                    || r.getStatus() == ExamResult.ResultStatus.ABSENT
                    || r.getStatus() == ExamResult.ResultStatus.WITHHELD) {
                continue;
            }
            r.setGrade(calculateGrade(r.getMarksObtained(), es.getMaxMarks()));
            r.setStatus(r.getMarksObtained() >= es.getPassingMarks()
                    ? ExamResult.ResultStatus.PASS : ExamResult.ResultStatus.FAIL);
            changed.add(r);
        }
        if (!changed.isEmpty()) {
            examResultRepository.saveAll(changed);
        }
        return changed.size();
    }

    private String calculateGrade(double marks, double maxMarks) {
        if (maxMarks <= 0) {
            return "F";
        }
        double percentage = (marks / maxMarks) * 100;
        if (percentage >= 90) return "A+";
        if (percentage >= 80) return "A";
        if (percentage >= 70) return "B+";
        if (percentage >= 60) return "B";
        if (percentage >= 50) return "C";
        if (percentage >= 40) return "D";
        return "F";
    }


    /** Loads the exam inside the organization and enforces branch access for branch-scoped callers. */
    private Examination loadExam(String organizationId, String examId) {
        Examination exam = examinationRepository.findByIdAndOrganizationId(examId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Examination not found: " + examId));
        AccessPolicy.assertBranch(exam.getBranchId());
        return exam;
    }

    private void validateBranchExists(String organizationId, String branchId) {
        branchRepository.findByIdAndOrganizationId(branchId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found: " + branchId));
    }
}
