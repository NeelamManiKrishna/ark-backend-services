package com.app.ark_backend_services.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class PlatformDashboardResponse {

    // Summary cards
    private long totalOrganizations;
    private long activeOrganizations;
    private long totalBranches;
    private long totalStudents;
    private long totalFaculty;
    private long totalUsers;

    // Pie chart: organizations by status
    private Map<String, Long> organizationsByStatus;

    // Bar chart: students per organization
    private List<OrgMetric> studentsPerOrganization;

    // Bar chart: faculty per organization
    private List<OrgMetric> facultyPerOrganization;

    // Bar chart: branches per organization
    private List<OrgMetric> branchesPerOrganization;

    // Pie chart: users by role (platform-wide)
    private Map<String, Long> usersByRole;

    // Activity: audit action counts
    private Map<String, Long> auditActionCounts;

    // Recent activity
    private List<RecentActivity> recentActivity;

    @Data
    @Builder
    public static class OrgMetric {
        private String organizationId;
        private String organizationName;
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
