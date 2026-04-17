package com.app.ark_backend_services.service;

import com.app.ark_backend_services.dto.*;
import com.app.ark_backend_services.dto.PromotionPreviewResponse.*;
import com.app.ark_backend_services.dto.PromotionExecuteResponse.*;
import com.app.ark_backend_services.exception.ResourceNotFoundException;
import com.app.ark_backend_services.model.*;
import com.app.ark_backend_services.model.AcademicClass.ClassStatus;
import com.app.ark_backend_services.model.AuditLog.Action;
import com.app.ark_backend_services.model.Examination.ExamType;
import com.app.ark_backend_services.model.ExamResult.ResultStatus;
import com.app.ark_backend_services.model.StudentEnrollment.EnrollmentStatus;
import com.app.ark_backend_services.model.StudentEnrollment.ExitReason;
import com.app.ark_backend_services.repository.*;
import com.app.ark_backend_services.security.CurrentUser;
import com.app.ark_backend_services.util.ArkIdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PromotionService {

    private final ClassProgressionRepository progressionRepository;
    private final AcademicClassRepository academicClassRepository;
    private final StudentEnrollmentRepository enrollmentRepository;
    private final StudentRepository studentRepository;
    private final ExaminationRepository examinationRepository;
    private final ExamSubjectRepository examSubjectRepository;
    private final ExamResultRepository examResultRepository;
    private final AuditLogService auditLogService;

    public PromotionPreviewResponse preview(String organizationId, String branchId, String sourceClassId, String targetAcademicYear) {
        validateOrgAccess(organizationId);

        // Get source class
        AcademicClass sourceClass = academicClassRepository.findByIdAndOrganizationIdAndBranchId(sourceClassId, organizationId, branchId)
                .orElseThrow(() -> new ResourceNotFoundException("Source class not found: " + sourceClassId));

        // Get class progression
        ClassProgression progression = progressionRepository.findByOrganizationIdAndBranchId(organizationId, branchId)
                .orElseThrow(() -> new IllegalStateException("Class progression not configured for branch: " + branchId));

        // Find current class in progression
        ClassProgression.ClassLevel currentLevel = progression.getSequence().stream()
                .filter(l -> l.getClassName().equals(sourceClass.getName()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Source class '" + sourceClass.getName() + "' not found in progression sequence"));

        boolean isTerminal = Boolean.TRUE.equals(currentLevel.getIsTerminal());

        // Determine target class name
        String targetClassName;
        if (isTerminal) {
            targetClassName = sourceClass.getName() + " (Graduation)";
        } else {
            targetClassName = progression.getSequence().stream()
                    .filter(l -> l.getDisplayOrder() == currentLevel.getDisplayOrder() + 1)
                    .findFirst()
                    .map(ClassProgression.ClassLevel::getClassName)
                    .orElseThrow(() -> new IllegalStateException("No next class defined after '" + sourceClass.getName() + "' in progression"));
        }

        // Get active enrollments for the source class
        List<StudentEnrollment> enrollments = enrollmentRepository
                .findByOrganizationIdAndClassIdAndStatus(organizationId, sourceClassId, EnrollmentStatus.ACTIVE);

        // Find FINAL exams for this branch and academic year
        List<Examination> finalExams = examinationRepository
                .findByOrganizationIdAndBranchIdAndAcademicYear(organizationId, branchId, sourceClass.getAcademicYear())
                .stream()
                .filter(e -> e.getExamType() == ExamType.FINAL)
                .toList();

        // Batch-fetch all students for these enrollments (eliminates N+1 queries)
        List<String> studentIds = enrollments.stream().map(StudentEnrollment::getStudentId).toList();
        Map<String, Student> studentMap = studentRepository.findAllById(studentIds).stream()
                .collect(Collectors.toMap(Student::getId, s -> s));

        // Batch-fetch all exam subjects for final exams in this class (eliminates N*M queries)
        Map<String, List<ExamSubject>> subjectsByExamId = new HashMap<>();
        for (Examination exam : finalExams) {
            subjectsByExamId.put(exam.getId(), examSubjectRepository.findByExaminationIdAndClassId(exam.getId(), sourceClassId));
        }

        // Build candidates
        List<PromotionCandidate> candidates = new ArrayList<>();
        int recommendPromote = 0, recommendHoldBack = 0, recommendGraduate = 0, noExamData = 0;

        for (StudentEnrollment enrollment : enrollments) {
            Student student = studentMap.get(enrollment.getStudentId());
            if (student == null || student.getStatus() != Student.StudentStatus.ACTIVE) continue;

            PromotionCandidate candidate = new PromotionCandidate();
            candidate.setStudentId(student.getId());
            candidate.setStudentArkId(student.getArkId());
            candidate.setFirstName(student.getFirstName());
            candidate.setLastName(student.getLastName());
            candidate.setRollNumber(student.getRollNumber());

            // Check exam results for FINAL exams
            List<String> failedSubjects = new ArrayList<>();
            boolean hasExamData = false;
            int totalSubjects = 0;
            int passedSubjects = 0;

            for (Examination exam : finalExams) {
                List<ExamSubject> subjects = subjectsByExamId.getOrDefault(exam.getId(), List.of());
                for (ExamSubject subject : subjects) {
                    totalSubjects++;
                    List<ExamResult> results = examResultRepository.findByExaminationIdAndStudentId(exam.getId(), student.getId());
                    Optional<ExamResult> result = results.stream()
                            .filter(r -> r.getExamSubjectId().equals(subject.getId()))
                            .findFirst();

                    if (result.isPresent()) {
                        hasExamData = true;
                        if (result.get().getStatus() == ResultStatus.PASS) {
                            passedSubjects++;
                        } else {
                            failedSubjects.add(subject.getSubjectName());
                        }
                    }
                }
            }

            candidate.setHasExamData(hasExamData);
            candidate.setFailedSubjects(failedSubjects);
            candidate.setHasFailingResults(!failedSubjects.isEmpty());

            if (isTerminal) {
                candidate.setRecommendation("GRADUATE");
                recommendGraduate++;
                candidate.setExamSummary(hasExamData
                        ? "Passed " + passedSubjects + "/" + totalSubjects + " subjects"
                        : "No exam data available");
            } else if (!hasExamData) {
                candidate.setRecommendation("PROMOTE");
                candidate.setExamSummary("No exam data available");
                recommendPromote++;
                noExamData++;
            } else if (failedSubjects.isEmpty()) {
                candidate.setRecommendation("PROMOTE");
                candidate.setExamSummary("Passed " + passedSubjects + "/" + totalSubjects + " subjects");
                recommendPromote++;
            } else {
                candidate.setRecommendation("HOLD_BACK");
                candidate.setExamSummary("Passed " + passedSubjects + "/" + totalSubjects + " subjects (Failed: " + String.join(", ", failedSubjects) + ")");
                recommendHoldBack++;
            }

            candidates.add(candidate);
        }

        // Build response
        PromotionPreviewResponse response = new PromotionPreviewResponse();
        SourceClassInfo sourceInfo = new SourceClassInfo();
        sourceInfo.setId(sourceClass.getId());
        sourceInfo.setName(sourceClass.getName());
        sourceInfo.setSection(sourceClass.getSection());
        sourceInfo.setAcademicYear(sourceClass.getAcademicYear());
        sourceInfo.setBranchId(sourceClass.getBranchId());
        sourceInfo.setStatus(sourceClass.getStatus().name());
        response.setSourceClass(sourceInfo);
        response.setTargetClassName(targetClassName);
        response.setTargetAcademicYear(targetAcademicYear);
        response.setTerminalClass(isTerminal);
        response.setTotalEligible(candidates.size());
        response.setTotalRecommendedPromote(recommendPromote);
        response.setTotalRecommendedHoldBack(recommendHoldBack);
        response.setTotalRecommendedGraduate(recommendGraduate);
        response.setTotalNoExamData(noExamData);
        response.setCandidates(candidates);

        return response;
    }

    @Transactional
    public PromotionExecuteResponse execute(String organizationId, String branchId, PromotionExecuteRequest request) {
        validateOrgAccess(organizationId);

        // Get source class
        AcademicClass sourceClass = academicClassRepository.findByIdAndOrganizationIdAndBranchId(request.getSourceClassId(), organizationId, branchId)
                .orElseThrow(() -> new ResourceNotFoundException("Source class not found: " + request.getSourceClassId()));

        // Get class progression
        ClassProgression progression = progressionRepository.findByOrganizationIdAndBranchId(organizationId, branchId)
                .orElseThrow(() -> new IllegalStateException("Class progression not configured for branch: " + branchId));

        ClassProgression.ClassLevel currentLevel = progression.getSequence().stream()
                .filter(l -> l.getClassName().equals(sourceClass.getName()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Source class '" + sourceClass.getName() + "' not found in progression sequence"));

        boolean isTerminal = Boolean.TRUE.equals(currentLevel.getIsTerminal());

        // Resolve target class name
        String targetClassName = isTerminal ? null : progression.getSequence().stream()
                .filter(l -> l.getDisplayOrder() == currentLevel.getDisplayOrder() + 1)
                .findFirst()
                .map(ClassProgression.ClassLevel::getClassName)
                .orElseThrow(() -> new IllegalStateException("No next class defined after '" + sourceClass.getName() + "'"));

        // Resolve target section
        String targetSection = request.getTargetSection() != null ? request.getTargetSection() : sourceClass.getSection();

        // Find or create target class (only if not terminal)
        AcademicClass targetClass = null;
        if (!isTerminal) {
            targetClass = academicClassRepository.findByOrganizationIdAndBranchIdAndNameAndSectionAndAcademicYear(
                    organizationId, branchId, targetClassName, targetSection, request.getTargetAcademicYear()
            ).orElseGet(() -> {
                AcademicClass newClass = new AcademicClass();
                newClass.setOrganizationId(organizationId);
                newClass.setBranchId(branchId);
                newClass.setName(targetClassName);
                newClass.setSection(targetSection);
                newClass.setAcademicYear(request.getTargetAcademicYear());
                newClass.setCapacity(sourceClass.getCapacity());
                newClass.setStatus(ClassStatus.ACTIVE);
                return academicClassRepository.save(newClass);
            });
        }

        // Build override map
        Map<String, PromotionExecuteRequest.StudentOverride> overrideMap = new HashMap<>();
        if (request.getStudentOverrides() != null) {
            for (PromotionExecuteRequest.StudentOverride override : request.getStudentOverrides()) {
                overrideMap.put(override.getStudentId(), override);
            }
        }

        // Get active enrollments
        List<StudentEnrollment> enrollments = enrollmentRepository
                .findByOrganizationIdAndClassIdAndStatus(organizationId, request.getSourceClassId(), EnrollmentStatus.ACTIVE);

        // Batch-fetch all students (eliminates N+1 queries)
        List<String> execStudentIds = enrollments.stream().map(StudentEnrollment::getStudentId).toList();
        Map<String, Student> execStudentMap = studentRepository.findAllById(execStudentIds).stream()
                .collect(Collectors.toMap(Student::getId, s -> s));

        int promoted = 0, graduated = 0, heldBack = 0;
        List<PromotionRecordDto> records = new ArrayList<>();

        for (StudentEnrollment enrollment : enrollments) {
            Student student = execStudentMap.get(enrollment.getStudentId());
            if (student == null || student.getStatus() != Student.StudentStatus.ACTIVE) continue;

            // Determine action
            String action;
            String reason = null;
            PromotionExecuteRequest.StudentOverride override = overrideMap.get(student.getId());
            if (override != null) {
                action = override.getAction();
                reason = override.getReason();
            } else if (isTerminal) {
                action = "GRADUATE";
            } else {
                action = "PROMOTE";
            }

            // Process based on action
            PromotionRecordDto record = new PromotionRecordDto();
            record.setStudentId(student.getId());
            record.setStudentArkId(student.getArkId());
            record.setStudentName(student.getFirstName() + " " + student.getLastName());
            record.setReason(reason);

            switch (action) {
                case "PROMOTE" -> {
                    // Close current enrollment
                    enrollment.setStatus(EnrollmentStatus.COMPLETED);
                    enrollment.setExitReason(ExitReason.PROMOTED);
                    enrollment.setExitedAt(LocalDate.now());
                    enrollmentRepository.save(enrollment);

                    // Create new enrollment in target class
                    StudentEnrollment newEnrollment = new StudentEnrollment();
                    newEnrollment.setArkId(ArkIdGenerator.generateEnrollmentId());
                    newEnrollment.setOrganizationId(organizationId);
                    newEnrollment.setBranchId(branchId);
                    newEnrollment.setStudentId(student.getId());
                    newEnrollment.setClassId(targetClass.getId());
                    newEnrollment.setAcademicYear(request.getTargetAcademicYear());
                    newEnrollment.setEnrolledAt(LocalDate.now());
                    newEnrollment.setStatus(EnrollmentStatus.ACTIVE);
                    enrollmentRepository.save(newEnrollment);

                    record.setPromotionType("PROMOTED");
                    record.setTargetClassId(targetClass.getId());
                    promoted++;
                }
                case "GRADUATE" -> {
                    // Close current enrollment
                    enrollment.setStatus(EnrollmentStatus.COMPLETED);
                    enrollment.setExitReason(ExitReason.GRADUATED);
                    enrollment.setExitedAt(LocalDate.now());
                    enrollmentRepository.save(enrollment);

                    student.setStatus(Student.StudentStatus.GRADUATED);
                    studentRepository.save(student);
                    record.setPromotionType("GRADUATED");
                    graduated++;
                }
                case "HOLD_BACK" -> {
                    // Student stays in current class — enrollment remains ACTIVE, no changes
                    record.setPromotionType("HELD_BACK");
                    heldBack++;
                }
            }

            records.add(record);
        }

        // Mark source class as COMPLETED
        sourceClass.setStatus(ClassStatus.COMPLETED);
        academicClassRepository.save(sourceClass);

        // Build response
        PromotionExecuteResponse response = new PromotionExecuteResponse();
        response.setSourceClassId(sourceClass.getId());
        response.setSourceClassName(sourceClass.getName() + " " + sourceClass.getSection());
        response.setTargetClassId(targetClass != null ? targetClass.getId() : null);
        response.setTargetClassName(targetClass != null ? targetClass.getName() + " " + targetClass.getSection() : "Graduated");
        response.setSourceAcademicYear(sourceClass.getAcademicYear());
        response.setTargetAcademicYear(request.getTargetAcademicYear());

        PromotionSummary summary = new PromotionSummary();
        summary.setTotalProcessed(records.size());
        summary.setPromoted(promoted);
        summary.setGraduated(graduated);
        summary.setHeldBack(heldBack);
        response.setSummary(summary);
        response.setRecords(records);

        auditLogService.log(Action.CREATE, "Promotion", sourceClass.getId(),
                "Promotion from " + sourceClass.getName(), organizationId,
                "Promoted: " + promoted + ", Graduated: " + graduated + ", Held back: " + heldBack);

        return response;
    }

    private void validateOrgAccess(String organizationId) {
        if (!CurrentUser.belongsToOrg(organizationId)) {
            throw new AccessDeniedException("You do not have access to this organization");
        }
    }
}
