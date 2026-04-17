package com.app.ark_backend_services.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class OrgDashboardResponse {

    private String organizationId;
    private String organizationName;

    // Summary cards
    private long totalBranches;
    private long totalClasses;
    private long totalStudents;
    private long totalFaculty;
    private long totalUsers;
    private long totalEnrollments;
    private long totalFacultyAssignments;

    // Pie chart: students by status
    private Map<String, Long> studentsByStatus;

    // Pie chart: students by gender
    private Map<String, Long> studentsByGender;

    // Pie chart: faculty by status
    private Map<String, Long> facultyByStatus;

    // Bar chart: students per branch
    private List<BranchMetric> studentsPerBranch;

    // Bar chart: faculty per branch
    private List<BranchMetric> facultyPerBranch;

    // Bar chart: classes per branch
    private List<BranchMetric> classesPerBranch;

    // Pie chart: users by role (within org)
    private Map<String, Long> usersByRole;

    // Activity: audit action counts for this org
    private Map<String, Long> auditActionCounts;

    // Recent activity for this org
    private List<RecentActivity> recentActivity;

    @Data
    @Builder
    public static class BranchMetric {
        private String branchId;
        private String branchName;
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
