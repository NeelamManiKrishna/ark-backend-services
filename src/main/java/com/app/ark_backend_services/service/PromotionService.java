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
import com.app.ark_backend_services.security.AccessPolicy;
import com.app.ark_backend_services.util.AcademicYears;
import com.app.ark_backend_services.util.ArkIdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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
    private final CatalogResolver catalogResolver;

    public PromotionPreviewResponse preview(String organizationId, String branchId, String sourceClassId, String targetAcademicYear) {
        AccessPolicy.assertOrg(organizationId);
        AccessPolicy.assertBranch(branchId);

        // Get source class
        AcademicClass sourceClass = academicClassRepository.findByIdAndOrganizationIdAndBranchId(sourceClassId, organizationId, branchId)
                .orElseThrow(() -> new ResourceNotFoundException("Source class not found: " + sourceClassId));

        AcademicYears.assertLater(targetAcademicYear, sourceClass.getAcademicYear());
        if (sourceClass.getStatus() == ClassStatus.COMPLETED) {
            throw new IllegalStateException("Source class has already been promoted (status COMPLETED)");
        }

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

        // Batch-fetch every result of those exams once; index by student and subject (no queries inside the loops)
        Map<String, Map<String, ExamResult>> resultsByStudent = new HashMap<>();
        if (!finalExams.isEmpty()) {
            for (ExamResult r : examResultRepository.findByExaminationIdIn(finalExams.stream().map(Examination::getId).toList())) {
                resultsByStudent.computeIfAbsent(r.getStudentId(), k -> new HashMap<>()).put(r.getExamSubjectId(), r);
            }
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
                    Optional<ExamResult> result = Optional.ofNullable(
                            resultsByStudent.getOrDefault(student.getId(), Map.of()).get(subject.getId()));

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
        response.setNoFinalExam(finalExams.isEmpty());
        response.setCandidates(candidates);

        return response;
    }

    public PromotionExecuteResponse execute(String organizationId, String branchId, PromotionExecuteRequest request) {
        AccessPolicy.assertOrg(organizationId);
        AccessPolicy.assertBranch(branchId);
        var targetYear = catalogResolver.year(organizationId, request.getTargetAcademicYearId(), request.getTargetAcademicYear());
        request.setTargetAcademicYear(targetYear.label());

        // Get source class
        AcademicClass sourceClass = academicClassRepository.findByIdAndOrganizationIdAndBranchId(request.getSourceClassId(), organizationId, branchId)
                .orElseThrow(() -> new ResourceNotFoundException("Source class not found: " + request.getSourceClassId()));

        AcademicYears.assertLater(request.getTargetAcademicYear(), sourceClass.getAcademicYear());
        if (sourceClass.getStatus() == ClassStatus.COMPLETED) {
            throw new IllegalStateException("Source class has already been promoted (status COMPLETED)");
        }

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
        AcademicClass targetClass = isTerminal ? null
                : findOrCreateClass(organizationId, branchId, sourceClass, targetClassName, targetSection, request.getTargetAcademicYear());

        // Build override map
        Map<String, PromotionExecuteRequest.StudentOverride> overrideMap = new HashMap<>();
        if (request.getStudentOverrides() != null) {
            for (PromotionExecuteRequest.StudentOverride override : request.getStudentOverrides()) {
                if (overrideMap.put(override.getStudentId(), override) != null) {
                    throw new IllegalArgumentException("Duplicate override for student " + override.getStudentId());
                }
                if (override.getAction() == PromotionExecuteRequest.Action.PROMOTE && isTerminal) {
                    throw new IllegalArgumentException("Cannot PROMOTE a student from the terminal class; use GRADUATE");
                }
                if (override.getAction() == PromotionExecuteRequest.Action.GRADUATE && !isTerminal) {
                    throw new IllegalArgumentException("Cannot GRADUATE a student from a non-terminal class");
                }
            }
        }

        // Get active enrollments
        List<StudentEnrollment> enrollments = enrollmentRepository
                .findByOrganizationIdAndClassIdAndStatus(organizationId, request.getSourceClassId(), EnrollmentStatus.ACTIVE);

        Set<String> rosterIds = enrollments.stream().map(StudentEnrollment::getStudentId).collect(Collectors.toSet());
        for (String overrideStudentId : overrideMap.keySet()) {
            if (!rosterIds.contains(overrideStudentId)) {
                throw new IllegalArgumentException("Override refers to a student who is not actively enrolled in the source class: " + overrideStudentId);
            }
        }

        // Batch-fetch all students (eliminates N+1 queries)
        List<String> execStudentIds = enrollments.stream().map(StudentEnrollment::getStudentId).toList();
        Map<String, Student> execStudentMap = studentRepository.findAllById(execStudentIds).stream()
                .collect(Collectors.toMap(Student::getId, s -> s));

        // Without an explicit override, follow the same recommendation the preview shows
        // (HOLD_BACK for students who failed a FINAL exam subject).
        Map<String, String> recommendations = preview(organizationId, branchId, request.getSourceClassId(), request.getTargetAcademicYear())
                .getCandidates().stream()
                .collect(Collectors.toMap(PromotionCandidate::getStudentId, PromotionCandidate::getRecommendation, (a, b) -> a));

        int promoted = 0, graduated = 0, heldBack = 0, skipped = 0;
        AcademicClass heldBackClass = null;
        List<PromotionRecordDto> records = new ArrayList<>();

        for (StudentEnrollment enrollment : enrollments) {
            Student student = execStudentMap.get(enrollment.getStudentId());
            if (student == null || student.getStatus() != Student.StudentStatus.ACTIVE) continue;

            // Determine action
            String action;
            String reason = null;
            PromotionExecuteRequest.StudentOverride override = overrideMap.get(student.getId());
            if (override != null) {
                action = override.getAction().name();
                reason = override.getReason();
            } else if (isTerminal) {
                action = "GRADUATE";
            } else {
                action = "HOLD_BACK".equals(recommendations.get(student.getId())) ? "HOLD_BACK" : "PROMOTE";
                if ("HOLD_BACK".equals(action)) {
                    reason = "Held back automatically: failed a FINAL exam subject";
                }
            }

            // Process based on action
            PromotionRecordDto record = new PromotionRecordDto();
            record.setStudentId(student.getId());
            record.setStudentArkId(student.getArkId());
            record.setStudentName(student.getFirstName() + " " + student.getLastName());
            record.setReason(reason);

            // Idempotency: a student who already has an enrollment in the target year is left alone
            if (!"GRADUATE".equals(action) && enrollmentRepository.existsByOrganizationIdAndStudentIdAndAcademicYear(
                    organizationId, student.getId(), request.getTargetAcademicYear())) {
                record.setPromotionType("SKIPPED");
                record.setReason("Already enrolled for " + request.getTargetAcademicYear());
                skipped++;
                records.add(record);
                continue;
            }

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
                    newEnrollment.setAcademicYearId(targetYear.id());
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
                    // Repeat the same class level next year: close the old enrollment as HELD_BACK and
                    // enrol the student in the same class (name + section) for the target year.
                    if (heldBackClass == null) {
                        heldBackClass = findOrCreateClass(organizationId, branchId, sourceClass,
                                sourceClass.getName(), sourceClass.getSection(), request.getTargetAcademicYear());
                    }
                    enrollment.setStatus(EnrollmentStatus.COMPLETED);
                    enrollment.setExitReason(ExitReason.HELD_BACK);
                    enrollment.setExitedAt(LocalDate.now());
                    enrollmentRepository.save(enrollment);

                    StudentEnrollment repeat = new StudentEnrollment();
                    repeat.setArkId(ArkIdGenerator.generateEnrollmentId());
                    repeat.setOrganizationId(organizationId);
                    repeat.setBranchId(branchId);
                    repeat.setStudentId(student.getId());
                    repeat.setClassId(heldBackClass.getId());
                    repeat.setAcademicYear(request.getTargetAcademicYear());
                    repeat.setAcademicYearId(targetYear.id());
                    repeat.setEnrolledAt(LocalDate.now());
                    repeat.setStatus(EnrollmentStatus.ACTIVE);
                    enrollmentRepository.save(repeat);

                    record.setPromotionType("HELD_BACK");
                    record.setTargetClassId(heldBackClass.getId());
                    heldBack++;
                }
            }

            auditLogService.log(Action.UPDATE, "StudentEnrollment", enrollment.getId(),
                    record.getStudentName(), organizationId,
                    "Promotion " + record.getPromotionType() + " from class " + sourceClass.getName()
                            + " " + sourceClass.getSection() + (record.getReason() != null ? " (" + record.getReason() + ")" : ""));

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
        summary.setSkipped(skipped);
        response.setSummary(summary);
        response.setRecords(records);

        auditLogService.log(Action.CREATE, "Promotion", sourceClass.getId(),
                "Promotion from " + sourceClass.getName(), organizationId,
                "Promoted: " + promoted + ", Graduated: " + graduated + ", Held back: " + heldBack);

        return response;
    }


    /** Finds the class for the target year or creates it (and audits the creation). */
    private AcademicClass findOrCreateClass(String organizationId, String branchId, AcademicClass template,
                                            String name, String section, String academicYear) {
        return academicClassRepository.findByOrganizationIdAndBranchIdAndNameAndSectionAndAcademicYear(
                organizationId, branchId, name, section, academicYear
        ).orElseGet(() -> {
            AcademicClass newClass = new AcademicClass();
            newClass.setOrganizationId(organizationId);
            newClass.setBranchId(branchId);
            newClass.setName(name);
            newClass.setSection(section);
            newClass.setAcademicYear(academicYear);
            newClass.setAcademicYearId(catalogResolver.year(organizationId, null, academicYear).id());
            newClass.setCapacity(template.getCapacity());
            newClass.setStatus(ClassStatus.ACTIVE);
            AcademicClass saved = academicClassRepository.save(newClass);
            auditLogService.log(Action.CREATE, "AcademicClass", saved.getId(), name + " " + section, organizationId,
                    "Class auto-created by promotion for " + academicYear);
            return saved;
        });
    }
}
