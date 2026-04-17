package com.app.ark_backend_services.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class FacultyClassPerformanceResponse {

    private String facultyId;
    private String facultyName;
    private String classId;
    private String className;
    private String academicYear;
    private String subjectName;
    private String assignmentType;
    private int studentsCount;
    private double averageMarks;
    private double highestMarks;
    private double lowestMarks;
    private double passPercentage;
    private Map<String, Integer> gradeDistribution;
    private List<ExamBreakdown> examWise;

    @Data
    @Builder
    public static class ExamBreakdown {
        private String examinationId;
        private String examinationName;
        private String examType;
        private double averageMarks;
        private double passPercentage;
        private int studentsAppeared;
        private int studentsAbsent;
    }
}
