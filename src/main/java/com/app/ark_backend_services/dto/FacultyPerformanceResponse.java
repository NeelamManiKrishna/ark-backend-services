package com.app.ark_backend_services.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class FacultyPerformanceResponse {

    private String facultyId;
    private String facultyName;
    private String employeeId;
    private String academicYear;
    private PerformanceSummary summary;
    private List<SubjectPerformance> subjectWise;
    private ClassTeacherInfo classTeacherOf;

    @Data
    @Builder
    public static class PerformanceSummary {
        private int totalClassesAssigned;
        private int totalSubjectsTaught;
        private int totalStudentsTaught;
        private double overallAverageMarks;
        private double overallPassPercentage;
        private Map<String, Integer> overallGradeDistribution;
    }

    @Data
    @Builder
    public static class SubjectPerformance {
        private String subjectName;
        private int classesCount;
        private List<ClassPerformance> classes;
        private int totalStudents;
        private double averageMarks;
        private double passPercentage;
    }

    @Data
    @Builder
    public static class ClassPerformance {
        private String classId;
        private String className;
        private int studentsCount;
        private double averageMarks;
        private double passPercentage;
        private Map<String, Integer> gradeDistribution;
    }

    @Data
    @Builder
    public static class ClassTeacherInfo {
        private String classId;
        private String className;
        private int studentsCount;
        private double classOverallAverage;
        private double classPassPercentage;
    }
}
