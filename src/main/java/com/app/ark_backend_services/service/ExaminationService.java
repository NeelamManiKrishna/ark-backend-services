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
import com.app.ark_backend_services.security.CurrentUser;
import com.app.ark_backend_services.util.ArkIdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    // ===================== Examination CRUD =====================

    public ExaminationResponse createExamination(String organizationId, String branchId, CreateExaminationRequest request) {
        validateOrgAccess(organizationId);
        validateBranchExists(organizationId, branchId);

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
        validateOrgAccess(organizationId);
        Examination exam = examinationRepository.findByIdAndOrganizationId(examId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Examination not found: " + examId));
        return ExaminationResponse.from(exam);
    }

    public Page<ExaminationResponse> getExaminationsByBranch(String organizationId, String branchId, Pageable pageable) {
        validateOrgAccess(organizationId);
        return examinationRepository.findByOrganizationIdAndBranchId(organizationId, branchId, pageable)
                .map(ExaminationResponse::from);
    }

    public Page<ExaminationResponse> getExaminationsByBranchAndYear(String organizationId, String branchId, String academicYear, Pageable pageable) {
        validateOrgAccess(organizationId);
        return examinationRepository.findByOrganizationIdAndBranchIdAndAcademicYear(organizationId, branchId, academicYear, pageable)
                .map(ExaminationResponse::from);
    }

    public ExaminationResponse updateExamination(String organizationId, String examId, UpdateExaminationRequest request) {
        validateOrgAccess(organizationId);
        Examination exam = examinationRepository.findByIdAndOrganizationId(examId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Examination not found: " + examId));

        if (request.getName() != null) exam.setName(request.getName());
        if (request.getAcademicYear() != null) exam.setAcademicYear(request.getAcademicYear());
        if (request.getExamType() != null) exam.setExamType(request.getExamType());
        if (request.getStartDate() != null) exam.setStartDate(request.getStartDate());
        if (request.getEndDate() != null) exam.setEndDate(request.getEndDate());
        if (request.getDescription() != null) exam.setDescription(request.getDescription());
        if (request.getStatus() != null) exam.setStatus(request.getStatus());

        ExaminationResponse response = ExaminationResponse.from(examinationRepository.save(exam));
        auditLogService.log(Action.UPDATE, "Examination", examId, exam.getName(), organizationId, "Examination updated");
        return response;
    }

    @Transactional
    public void deleteExamination(String organizationId, String examId) {
        validateOrgAccess(organizationId);
        Examination exam = examinationRepository.findByIdAndOrganizationId(examId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Examination not found: " + examId));
        examResultRepository.deleteByExaminationId(examId);
        examSubjectRepository.deleteByExaminationId(examId);
        auditLogService.log(Action.DELETE, "Examination", examId, exam.getName(), organizationId,
                "Examination deleted with subjects and results");
        examinationRepository.delete(exam);
    }

    // ===================== Exam Subject CRUD =====================

    public ExamSubjectResponse createExamSubject(String organizationId, String examId, CreateExamSubjectRequest request) {
        validateOrgAccess(organizationId);
        Examination exam = examinationRepository.findByIdAndOrganizationId(examId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Examination not found: " + examId));

        academicClassRepository.findByIdAndOrganizationIdAndBranchId(request.getClassId(), organizationId, exam.getBranchId())
                .orElseThrow(() -> new ResourceNotFoundException("Class not found: " + request.getClassId()));

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
        validateOrgAccess(organizationId);
        examinationRepository.findByIdAndOrganizationId(examId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Examination not found: " + examId));
        return examSubjectRepository.findByExaminationId(examId, pageable).map(ExamSubjectResponse::from);
    }

    public ExamSubjectResponse updateExamSubject(String organizationId, String examId, String subjectId, UpdateExamSubjectRequest request) {
        validateOrgAccess(organizationId);
        examinationRepository.findByIdAndOrganizationId(examId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Examination not found: " + examId));
        ExamSubject es = examSubjectRepository.findByIdAndExaminationId(subjectId, examId)
                .orElseThrow(() -> new ResourceNotFoundException("Exam subject not found: " + subjectId));

        if (request.getSubjectName() != null) es.setSubjectName(request.getSubjectName());
        if (request.getSubjectCode() != null) es.setSubjectCode(request.getSubjectCode());
        if (request.getMaxMarks() != null) es.setMaxMarks(request.getMaxMarks());
        if (request.getPassingMarks() != null) es.setPassingMarks(request.getPassingMarks());
        if (request.getExamDate() != null) es.setExamDate(request.getExamDate());
        if (request.getStatus() != null) es.setStatus(request.getStatus());

        // Validate passingMarks <= maxMarks after all updates applied
        if (es.getPassingMarks() > es.getMaxMarks()) {
            throw new IllegalArgumentException("Passing marks (" + es.getPassingMarks() + ") cannot exceed max marks (" + es.getMaxMarks() + ")");
        }

        ExamSubjectResponse response = ExamSubjectResponse.from(examSubjectRepository.save(es));
        auditLogService.log(Action.UPDATE, "ExamSubject", subjectId, es.getSubjectName(), organizationId, "Exam subject updated");
        return response;
    }

    @Transactional
    public void deleteExamSubject(String organizationId, String examId, String subjectId) {
        validateOrgAccess(organizationId);
        examinationRepository.findByIdAndOrganizationId(examId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Examination not found: " + examId));
        ExamSubject es = examSubjectRepository.findByIdAndExaminationId(subjectId, examId)
                .orElseThrow(() -> new ResourceNotFoundException("Exam subject not found: " + subjectId));
        examResultRepository.deleteByExamSubjectId(subjectId);
        auditLogService.log(Action.DELETE, "ExamSubject", subjectId, es.getSubjectName(), organizationId, "Exam subject deleted with results");
        examSubjectRepository.delete(es);
    }

    // ===================== Exam Result CRUD =====================

    public ExamResultResponse createExamResult(String organizationId, String examId, String subjectId, CreateExamResultRequest request) {
        validateOrgAccess(organizationId);
        examinationRepository.findByIdAndOrganizationId(examId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Examination not found: " + examId));
        ExamSubject es = examSubjectRepository.findByIdAndExaminationId(subjectId, examId)
                .orElseThrow(() -> new ResourceNotFoundException("Exam subject not found: " + subjectId));

        studentRepository.findByIdAndOrganizationId(request.getStudentId(), organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found: " + request.getStudentId()));

        // Validate student's active enrollment matches the exam subject's class
        java.util.Optional<StudentEnrollment> activeEnrollment = enrollmentRepository
                .findByStudentIdAndStatus(request.getStudentId(), StudentEnrollment.EnrollmentStatus.ACTIVE);
        if (activeEnrollment.isPresent() && !activeEnrollment.get().getClassId().equals(es.getClassId())) {
            throw new IllegalArgumentException("Student's active enrollment is in class "
                    + activeEnrollment.get().getClassId() + ", but exam subject belongs to class " + es.getClassId());
        }

        if (examResultRepository.existsByExamSubjectIdAndStudentId(subjectId, request.getStudentId())) {
            throw new DuplicateResourceException("Result already exists for this student and subject");
        }

        if (request.getMarksObtained() > es.getMaxMarks()) {
            throw new IllegalArgumentException("Marks obtained cannot exceed max marks (" + es.getMaxMarks() + ")");
        }

        // Use enrollment's classId if available, otherwise fall back to exam subject's classId
        String resolvedClassId = activeEnrollment.map(StudentEnrollment::getClassId).orElse(es.getClassId());

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
        validateOrgAccess(organizationId);
        examinationRepository.findByIdAndOrganizationId(examId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Examination not found: " + examId));
        return examResultRepository.findByExaminationId(examId, pageable).map(ExamResultResponse::from);
    }

    public Page<ExamResultResponse> getResultsByClass(String organizationId, String examId, String classId, Pageable pageable) {
        validateOrgAccess(organizationId);
        examinationRepository.findByIdAndOrganizationId(examId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Examination not found: " + examId));
        return examResultRepository.findByExaminationIdAndClassId(examId, classId, pageable).map(ExamResultResponse::from);
    }

    public java.util.List<ExamResultResponse> getStudentResults(String organizationId, String examId, String studentId) {
        validateOrgAccess(organizationId);
        examinationRepository.findByIdAndOrganizationId(examId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Examination not found: " + examId));
        return examResultRepository.findByExaminationIdAndStudentId(examId, studentId)
                .stream().map(ExamResultResponse::from).toList();
    }

    public ExamResultResponse updateExamResult(String organizationId, String examId, String resultId, UpdateExamResultRequest request) {
        validateOrgAccess(organizationId);
        examinationRepository.findByIdAndOrganizationId(examId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Examination not found: " + examId));
        ExamResult result = examResultRepository.findByIdAndOrganizationId(resultId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Exam result not found: " + resultId));

        if (request.getMarksObtained() != null) {
            ExamSubject es = examSubjectRepository.findById(result.getExamSubjectId())
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

    // ===================== Helpers =====================

    private String calculateGrade(double marks, double maxMarks) {
        double percentage = (marks / maxMarks) * 100;
        if (percentage >= 90) return "A+";
        if (percentage >= 80) return "A";
        if (percentage >= 70) return "B+";
        if (percentage >= 60) return "B";
        if (percentage >= 50) return "C";
        if (percentage >= 40) return "D";
        return "F";
    }

    private void validateOrgAccess(String organizationId) {
        if (!CurrentUser.belongsToOrg(organizationId)) {
            throw new AccessDeniedException("You do not have access to this organization");
        }
    }

    private void validateBranchExists(String organizationId, String branchId) {
        branchRepository.findByIdAndOrganizationId(branchId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found: " + branchId));
    }
}
