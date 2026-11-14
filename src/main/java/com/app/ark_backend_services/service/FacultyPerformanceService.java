package com.app.ark_backend_services.service;

import com.app.ark_backend_services.dto.*;
import com.app.ark_backend_services.dto.FacultyPerformanceResponse.*;
import com.app.ark_backend_services.exception.ResourceNotFoundException;
import com.app.ark_backend_services.model.*;
import com.app.ark_backend_services.model.ExamResult.ResultStatus;
import com.app.ark_backend_services.model.FacultyAssignment.AssignmentStatus;
import com.app.ark_backend_services.model.FacultyAssignment.AssignmentType;
import com.app.ark_backend_services.repository.*;
import com.app.ark_backend_services.security.AccessPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FacultyPerformanceService {

    private final FacultyRepository facultyRepository;
    private final FacultyAssignmentRepository assignmentRepository;
    private final AcademicClassRepository academicClassRepository;
    private final ExaminationRepository examinationRepository;
    private final ExamSubjectRepository examSubjectRepository;
    private final ExamResultRepository examResultRepository;

    public FacultyPerformanceResponse getOverallPerformance(String organizationId, String facultyId, String academicYear) {
        AccessPolicy.assertOrg(organizationId);

        Faculty faculty = facultyRepository.findByIdAndOrganizationId(facultyId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Faculty not found: " + facultyId));
        AccessPolicy.assertBranch(faculty.getBranchId());

        List<FacultyAssignment> assignments = assignmentRepository.findByOrganizationIdAndFacultyIdAndStatus(organizationId, facultyId, AssignmentStatus.ACTIVE);
        if (academicYear != null) {
            assignments = assignments.stream()
                    .filter(a -> academicYear.equals(a.getAcademicYear()))
                    .toList();
        }

        // Group assignments by subject
        Map<String, List<FacultyAssignment>> bySubject = assignments.stream()
                .filter(a -> a.getSubjectName() != null && !a.getSubjectName().isEmpty())
                .collect(Collectors.groupingBy(FacultyAssignment::getSubjectName));

        List<SubjectPerformance> subjectPerformances = new ArrayList<>();
        int totalStudents = 0;
        int totalPassed = 0;
        double totalMarksSum = 0;
        int totalResultCount = 0;
        Map<String, Integer> overallGrades = new LinkedHashMap<>();
        Set<String> uniqueClasses = new HashSet<>();
        assignments.forEach(a -> uniqueClasses.add(a.getClassId()));

        for (Map.Entry<String, List<FacultyAssignment>> entry : bySubject.entrySet()) {
            String subjectName = entry.getKey();
            List<FacultyAssignment> subjectAssignments = entry.getValue();

            List<ClassPerformance> classPerformances = new ArrayList<>();
            int subjectStudents = 0;
            int subjectPassed = 0;
            double subjectMarksSum = 0;
            int subjectResultCount = 0;

            for (FacultyAssignment assignment : subjectAssignments) {
                uniqueClasses.add(assignment.getClassId());
                AcademicClass ac = academicClassRepository.findByIdAndOrganizationId(assignment.getClassId(), organizationId).orElse(null);
                String className = ac != null ? ac.getName() + " " + ac.getSection() : assignment.getClassId();

                List<ExamResult> results = getResultsForClassSubject(organizationId, assignment.getClassId(), subjectName, assignment.getAcademicYear());

                if (results.isEmpty()) {
                    classPerformances.add(ClassPerformance.builder()
                            .classId(assignment.getClassId())
                            .className(className)
                            .studentsCount(0)
                            .averageMarks(0)
                            .passPercentage(0)
                            .gradeDistribution(Collections.emptyMap())
                            .build());
                    continue;
                }

                double avg = results.stream().mapToDouble(ExamResult::getMarksObtained).average().orElse(0);
                long passed = results.stream().filter(r -> r.getStatus() == ResultStatus.PASS).count();
                double passPercent = results.isEmpty() ? 0 : (passed * 100.0 / results.size());
                Map<String, Integer> grades = countGrades(results);

                classPerformances.add(ClassPerformance.builder()
                        .classId(assignment.getClassId())
                        .className(className)
                        .studentsCount(distinctStudents(results))
                        .averageMarks(round2(avg))
                        .passPercentage(round2(passPercent))
                        .gradeDistribution(grades)
                        .build());

                subjectStudents += distinctStudents(results);
                subjectPassed += passed;
                subjectMarksSum += results.stream().mapToDouble(ExamResult::getMarksObtained).sum();
                subjectResultCount += results.size();
                mergeGrades(overallGrades, grades);
            }

            double subjectAvg = subjectResultCount > 0 ? subjectMarksSum / subjectResultCount : 0;
            double subjectPass = subjectStudents > 0 ? (subjectPassed * 100.0 / subjectStudents) : 0;

            subjectPerformances.add(SubjectPerformance.builder()
                    .subjectName(subjectName)
                    .classesCount(subjectAssignments.size())
                    .classes(classPerformances)
                    .totalStudents(subjectStudents)
                    .averageMarks(round2(subjectAvg))
                    .passPercentage(round2(subjectPass))
                    .build());

            totalStudents += subjectStudents;
            totalPassed += subjectPassed;
            totalMarksSum += subjectMarksSum;
            totalResultCount += subjectResultCount;
        }

        double overallAvg = totalResultCount > 0 ? totalMarksSum / totalResultCount : 0;
        double overallPass = totalStudents > 0 ? (totalPassed * 100.0 / totalStudents) : 0;

        // Class teacher info
        ClassTeacherInfo classTeacherInfo = null;
        Optional<FacultyAssignment> classTeacherAssignment = assignments.stream()
                .filter(a -> a.getAssignmentType() == AssignmentType.CLASS_TEACHER || a.getAssignmentType() == AssignmentType.BOTH)
                .findFirst();
        if (classTeacherAssignment.isPresent()) {
            FacultyAssignment cta = classTeacherAssignment.get();
            AcademicClass ac = academicClassRepository.findByIdAndOrganizationId(cta.getClassId(), organizationId).orElse(null);
            if (ac != null) {
                List<ExamResult> allClassResults = getAllResultsForClass(organizationId, cta.getClassId(), cta.getAcademicYear());
                double classAvg = allClassResults.stream().mapToDouble(ExamResult::getMarksObtained).average().orElse(0);
                long classPassed = allClassResults.stream().filter(r -> r.getStatus() == ResultStatus.PASS).count();
                double classPassPercent = allClassResults.isEmpty() ? 0 : (classPassed * 100.0 / allClassResults.size());

                classTeacherInfo = ClassTeacherInfo.builder()
                        .classId(ac.getId())
                        .className(ac.getName() + " " + ac.getSection())
                        .studentsCount((int) allClassResults.stream().map(ExamResult::getStudentId).distinct().count())
                        .classOverallAverage(round2(classAvg))
                        .classPassPercentage(round2(classPassPercent))
                        .build();
            }
        }

        Set<String> uniqueSubjects = bySubject.keySet();

        return FacultyPerformanceResponse.builder()
                .facultyId(facultyId)
                .facultyName(faculty.getFirstName() + " " + faculty.getLastName())
                .employeeId(faculty.getEmployeeId())
                .academicYear(academicYear)
                .summary(PerformanceSummary.builder()
                        .totalClassesAssigned(uniqueClasses.size())
                        .totalSubjectsTaught(uniqueSubjects.size())
                        .totalStudentsTaught(totalStudents)
                        .overallAverageMarks(round2(overallAvg))
                        .overallPassPercentage(round2(overallPass))
                        .overallGradeDistribution(overallGrades)
                        .build())
                .subjectWise(subjectPerformances)
                .classTeacherOf(classTeacherInfo)
                .build();
    }

    public FacultyClassPerformanceResponse getClassPerformance(String organizationId, String facultyId, String classId, String academicYear) {
        AccessPolicy.assertOrg(organizationId);

        Faculty faculty = facultyRepository.findByIdAndOrganizationId(facultyId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Faculty not found: " + facultyId));
        AccessPolicy.assertBranch(faculty.getBranchId());

        AcademicClass ac = academicClassRepository.findByIdAndOrganizationId(classId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found: " + classId));

        List<FacultyAssignment> assignments = assignmentRepository
                .findByOrganizationIdAndClassIdAndAcademicYear(organizationId, classId, academicYear);
        FacultyAssignment assignment = assignments.stream()
                .filter(a -> a.getFacultyId().equals(facultyId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Faculty not assigned to this class for " + academicYear));

        String subjectName = assignment.getSubjectName();
        List<ExamResult> results = getResultsForClassSubject(organizationId, classId, subjectName, academicYear);

        double avg = results.stream().mapToDouble(ExamResult::getMarksObtained).average().orElse(0);
        double highest = results.stream().mapToDouble(ExamResult::getMarksObtained).max().orElse(0);
        double lowest = results.stream().mapToDouble(ExamResult::getMarksObtained).min().orElse(0);
        long passed = results.stream().filter(r -> r.getStatus() == ResultStatus.PASS).count();
        double passPercent = results.isEmpty() ? 0 : (passed * 100.0 / results.size());

        // Exam-wise breakdown
        List<Examination> exams = examinationRepository.findByOrganizationIdAndBranchIdAndAcademicYear(
                organizationId, ac.getBranchId(), academicYear);

        List<FacultyClassPerformanceResponse.ExamBreakdown> examBreakdowns = new ArrayList<>();
        for (Examination exam : exams) {
            List<ExamSubject> subjects = examSubjectRepository.findByExaminationIdAndClassId(exam.getId(), classId);
            ExamSubject matchingSubject = subjects.stream()
                    .filter(s -> s.getSubjectName().equals(subjectName))
                    .findFirst()
                    .orElse(null);

            if (matchingSubject == null) continue;

            List<ExamResult> examResults = examResultRepository.findByExamSubjectId(matchingSubject.getId());
            if (examResults.isEmpty()) continue;

            long examAbsent = examResults.stream().filter(r -> r.getStatus() == ResultStatus.ABSENT).count();
            // Averages and pass rates only cover students who appeared and have marks
            List<ExamResult> appeared = examResults.stream()
                    .filter(r -> r.getMarksObtained() != null
                            && r.getStatus() != ResultStatus.ABSENT && r.getStatus() != ResultStatus.WITHHELD)
                    .toList();
            double examAvg = appeared.stream().mapToDouble(ExamResult::getMarksObtained).average().orElse(0);
            long examPassed = appeared.stream().filter(r -> r.getStatus() == ResultStatus.PASS).count();
            double examPassPercent = appeared.isEmpty() ? 0 : (examPassed * 100.0 / appeared.size());

            examBreakdowns.add(FacultyClassPerformanceResponse.ExamBreakdown.builder()
                    .examinationId(exam.getId())
                    .examinationName(exam.getName())
                    .examType(exam.getExamType().name())
                    .averageMarks(round2(examAvg))
                    .passPercentage(round2(examPassPercent))
                    .studentsAppeared(appeared.size())
                    .studentsAbsent((int) examAbsent)
                    .build());
        }

        return FacultyClassPerformanceResponse.builder()
                .facultyId(facultyId)
                .facultyName(faculty.getFirstName() + " " + faculty.getLastName())
                .classId(classId)
                .className(ac.getName() + " " + ac.getSection())
                .academicYear(academicYear)
                .subjectName(subjectName)
                .assignmentType(assignment.getAssignmentType().name())
                .studentsCount(distinctStudents(results))
                .averageMarks(round2(avg))
                .highestMarks(highest)
                .lowestMarks(lowest)
                .passPercentage(round2(passPercent))
                .gradeDistribution(countGrades(results))
                .examWise(examBreakdowns)
                .build();
    }

    public FacultySubjectPerformanceResponse getSubjectPerformance(String organizationId, String facultyId, String subjectName, String academicYear) {
        AccessPolicy.assertOrg(organizationId);

        Faculty faculty = facultyRepository.findByIdAndOrganizationId(facultyId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Faculty not found: " + facultyId));
        AccessPolicy.assertBranch(faculty.getBranchId());

        List<FacultyAssignment> assignments = assignmentRepository.findByOrganizationIdAndFacultyIdAndStatus(organizationId, facultyId, AssignmentStatus.ACTIVE)
                .stream()
                .filter(a -> subjectName.equals(a.getSubjectName()))
                .filter(a -> academicYear == null || academicYear.equals(a.getAcademicYear()))
                .toList();

        List<FacultySubjectPerformanceResponse.ClassBreakdown> breakdowns = new ArrayList<>();
        int totalStudents = 0;
        int totalPassed = 0;
        double totalMarksSum = 0;
        int totalResultCount = 0;

        for (FacultyAssignment assignment : assignments) {
            AcademicClass ac = academicClassRepository.findByIdAndOrganizationId(assignment.getClassId(), organizationId).orElse(null);
            String className = ac != null ? ac.getName() + " " + ac.getSection() : assignment.getClassId();

            List<ExamResult> results = getResultsForClassSubject(organizationId, assignment.getClassId(), subjectName, assignment.getAcademicYear());

            double avg = results.stream().mapToDouble(ExamResult::getMarksObtained).average().orElse(0);
            long passed = results.stream().filter(r -> r.getStatus() == ResultStatus.PASS).count();
            double passPercent = results.isEmpty() ? 0 : (passed * 100.0 / results.size());

            breakdowns.add(FacultySubjectPerformanceResponse.ClassBreakdown.builder()
                    .classId(assignment.getClassId())
                    .className(className)
                    .studentsCount(distinctStudents(results))
                    .averageMarks(round2(avg))
                    .passPercentage(round2(passPercent))
                    .build());

            totalStudents += results.size();
            totalPassed += passed;
            totalMarksSum += results.stream().mapToDouble(ExamResult::getMarksObtained).sum();
            totalResultCount += results.size();
        }

        double overallAvg = totalResultCount > 0 ? totalMarksSum / totalResultCount : 0;
        double overallPass = totalStudents > 0 ? (totalPassed * 100.0 / totalStudents) : 0;

        return FacultySubjectPerformanceResponse.builder()
                .facultyId(facultyId)
                .facultyName(faculty.getFirstName() + " " + faculty.getLastName())
                .subjectName(subjectName)
                .academicYear(academicYear)
                .totalClasses(assignments.size())
                .totalStudents(totalStudents)
                .overallAverage(round2(overallAvg))
                .overallPassPercentage(round2(overallPass))
                .classBreakdown(breakdowns)
                .build();
    }

    // ---- Helpers ----

    private List<ExamResult> getResultsForClassSubject(String organizationId, String classId, String subjectName, String academicYear) {
        return resultsForClass(organizationId, classId, subjectName, academicYear);
    }

    private List<ExamResult> getAllResultsForClass(String organizationId, String classId, String academicYear) {
        return resultsForClass(organizationId, classId, null, academicYear);
    }

    /**
     * Results that count towards averages and pass rates: marks present and student not ABSENT/WITHHELD.
     * Subjects are collected first and their results fetched with a single IN query.
     */
    private List<ExamResult> resultsForClass(String organizationId, String classId, String subjectName, String academicYear) {
        String classBranchId = academicClassRepository.findByIdAndOrganizationId(classId, organizationId)
                .map(AcademicClass::getBranchId)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found: " + classId));
        List<Examination> exams = examinationRepository.findByOrganizationIdAndBranchIdAndAcademicYear(
                organizationId, classBranchId, academicYear);

        List<String> subjectIds = new ArrayList<>();
        for (Examination exam : exams) {
            for (ExamSubject subject : examSubjectRepository.findByExaminationIdAndClassId(exam.getId(), classId)) {
                if (subjectName == null || subjectName.equals(subject.getSubjectName())) {
                    subjectIds.add(subject.getId());
                }
            }
        }
        if (subjectIds.isEmpty()) {
            return List.of();
        }
        return examResultRepository.findByExamSubjectIdIn(subjectIds).stream()
                .filter(r -> r.getMarksObtained() != null
                        && r.getStatus() != ResultStatus.ABSENT
                        && r.getStatus() != ResultStatus.WITHHELD)
                .toList();
    }

    private static int distinctStudents(List<ExamResult> results) {
        return (int) results.stream().map(ExamResult::getStudentId).distinct().count();
    }

    private Map<String, Integer> countGrades(List<ExamResult> results) {
        Map<String, Integer> grades = new LinkedHashMap<>();
        for (String g : List.of("A+", "A", "B+", "B", "C", "D", "F")) {
            grades.put(g, 0);
        }
        for (ExamResult r : results) {
            if (r.getGrade() != null) {
                grades.merge(r.getGrade(), 1, Integer::sum);
            }
        }
        return grades;
    }

    private void mergeGrades(Map<String, Integer> target, Map<String, Integer> source) {
        source.forEach((grade, count) -> target.merge(grade, count, Integer::sum));
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

}
