package com.app.ark_backend_services.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class FacultySubjectPerformanceResponse {

    private String facultyId;
    private String facultyName;
    private String subjectName;
    private String academicYear;
    private int totalClasses;
    private int totalStudents;
    private double overallAverage;
    private double overallPassPercentage;
    private List<ClassBreakdown> classBreakdown;

    @Data
    @Builder
    public static class ClassBreakdown {
        private String classId;
        private String className;
        private int studentsCount;
        private double averageMarks;
        private double passPercentage;
    }
}
