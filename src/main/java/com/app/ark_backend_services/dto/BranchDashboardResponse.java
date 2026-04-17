package com.app.ark_backend_services.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class BranchDashboardResponse {

    private String organizationId;
    private String organizationName;
    private String branchId;
    private String branchName;

    // Summary cards
    private long totalClasses;
    private long totalStudents;
    private long totalFaculty;
    private long totalEnrollments;
    private long totalFacultyAssignments;

    // Pie chart: students by status
    private Map<String, Long> studentsByStatus;

    // Pie chart: students by gender
    private Map<String, Long> studentsByGender;

    // Pie chart: faculty by status
    private Map<String, Long> facultyByStatus;

    // Pie chart: faculty by department
    private Map<String, Long> facultyByDepartment;

    // Bar chart: students per class
    private List<ClassMetric> studentsPerClass;

    // Recent activity for this branch
    private List<RecentActivity> recentActivity;

    @Data
    @Builder
    public static class ClassMetric {
        private String classId;
        private String className;
        private String section;
        private long count;
    }

    @Data
    @Builder
    public static class RecentActivity {
        private String action;
        private String entityType;
        private String entityName;
        private String performedByEmail;
        private String timestamp;
    }
}