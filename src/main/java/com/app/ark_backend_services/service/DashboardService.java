package com.app.ark_backend_services.service;

import com.app.ark_backend_services.dto.BranchDashboardResponse;
import com.app.ark_backend_services.dto.OrgDashboardResponse;
import com.app.ark_backend_services.dto.PlatformDashboardResponse;
import com.app.ark_backend_services.exception.ResourceNotFoundException;
import com.app.ark_backend_services.model.*;
import com.app.ark_backend_services.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final OrganizationRepository organizationRepository;
    private final BranchRepository branchRepository;
    private final AcademicClassRepository academicClassRepository;
    private final StudentRepository studentRepository;
    private final FacultyRepository facultyRepository;
    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;
    private final StudentEnrollmentRepository enrollmentRepository;
    private final FacultyAssignmentRepository assignmentRepository;

    public PlatformDashboardResponse getPlatformDashboard() {
        List<Organization> orgs = organizationRepository.findAll();

        // Summary counts
        long totalOrgs = orgs.size();
        long activeOrgs = orgs.stream().filter(o -> o.getStatus() == Organization.OrganizationStatus.ACTIVE).count();
        long totalBranches = branchRepository.count();
        long totalStudents = studentRepository.count();
        long totalFaculty = facultyRepository.count();
        long totalUsers = userRepository.count();

        // Orgs by status (pie chart)
        Map<String, Long> orgsByStatus = new LinkedHashMap<>();
        for (Organization.OrganizationStatus status : Organization.OrganizationStatus.values()) {
            long count = orgs.stream().filter(o -> o.getStatus() == status).count();
            if (count > 0) orgsByStatus.put(status.name(), count);
        }

        // Per-org metrics (bar charts)
        List<PlatformDashboardResponse.OrgMetric> studentsPerOrg = orgs.stream()
                .map(o -> PlatformDashboardResponse.OrgMetric.builder()
                        .organizationId(o.getId())
                        .organizationName(o.getName())
                        .count(studentRepository.countByOrganizationId(o.getId()))
                        .build())
                .collect(Collectors.toList());

        List<PlatformDashboardResponse.OrgMetric> facultyPerOrg = orgs.stream()
                .map(o -> PlatformDashboardResponse.OrgMetric.builder()
                        .organizationId(o.getId())
                        .organizationName(o.getName())
                        .count(facultyRepository.countByOrganizationId(o.getId()))
                        .build())
                .collect(Collectors.toList());

        List<PlatformDashboardResponse.OrgMetric> branchesPerOrg = orgs.stream()
                .map(o -> PlatformDashboardResponse.OrgMetric.builder()
                        .organizationId(o.getId())
                        .organizationName(o.getName())
                        .count(branchRepository.countByOrganizationId(o.getId()))
                        .build())
                .collect(Collectors.toList());

        // Users by role (pie chart)
        Map<String, Long> usersByRole = new LinkedHashMap<>();
        for (User.Role role : User.Role.values()) {
            long count = userRepository.countByRole(role);
            if (count > 0) usersByRole.put(role.name(), count);
        }

        // Audit action counts
        Map<String, Long> auditCounts = new LinkedHashMap<>();
        for (AuditLog.Action action : AuditLog.Action.values()) {
            long count = auditLogRepository.countByAction(action);
            if (count > 0) auditCounts.put(action.name(), count);
        }

        // Recent activity
        List<PlatformDashboardResponse.RecentActivity> recentActivity = auditLogRepository.findTop10ByOrderByTimestampDesc()
                .stream()
                .map(log -> PlatformDashboardResponse.RecentActivity.builder()
                        .action(log.getAction().name())
                        .entityType(log.getEntityType())
                        .entityName(log.getEntityName())
                        .performedByEmail(log.getPerformedByEmail())
                        .timestamp(log.getTimestamp() != null ? log.getTimestamp().toString() : null)
                        .build())
                .collect(Collectors.toList());

        return PlatformDashboardResponse.builder()
                .totalOrganizations(totalOrgs)
                .activeOrganizations(activeOrgs)
                .totalBranches(totalBranches)
                .totalStudents(totalStudents)
                .totalFaculty(totalFaculty)
                .totalUsers(totalUsers)
                .organizationsByStatus(orgsByStatus)
                .studentsPerOrganization(studentsPerOrg)
                .facultyPerOrganization(facultyPerOrg)
                .branchesPerOrganization(branchesPerOrg)
                .usersByRole(usersByRole)
                .auditActionCounts(auditCounts)
                .recentActivity(recentActivity)
                .build();
    }

    public OrgDashboardResponse getOrgDashboard(String organizationId) {
        Organization org = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found: " + organizationId));

        List<Branch> branches = branchRepository.findByOrganizationId(organizationId);

        long totalBranches = branches.size();
        long totalClasses = academicClassRepository.countByOrganizationId(organizationId);
        long totalStudents = studentRepository.countByOrganizationId(organizationId);
        long totalFaculty = facultyRepository.countByOrganizationId(organizationId);
        long totalUsers = userRepository.countByOrganizationId(organizationId);
        long totalEnrollments = enrollmentRepository.countByOrganizationId(organizationId);
        long totalFacultyAssignments = assignmentRepository.countByOrganizationId(organizationId);

        // Students by status (pie chart)
        Map<String, Long> studentsByStatus = new LinkedHashMap<>();
        for (Student.StudentStatus status : Student.StudentStatus.values()) {
            long count = studentRepository.countByOrganizationIdAndStatus(organizationId, status);
            if (count > 0) studentsByStatus.put(status.name(), count);
        }

        // Students by gender (pie chart)
        Map<String, Long> studentsByGender = new LinkedHashMap<>();
        for (String gender : List.of("Male", "Female", "Other")) {
            long count = studentRepository.countByOrganizationIdAndGender(organizationId, gender);
            if (count > 0) studentsByGender.put(gender, count);
        }

        // Faculty by status (pie chart)
        Map<String, Long> facultyByStatus = new LinkedHashMap<>();
        for (Faculty.FacultyStatus status : Faculty.FacultyStatus.values()) {
            long count = facultyRepository.countByOrganizationIdAndStatus(organizationId, status);
            if (count > 0) facultyByStatus.put(status.name(), count);
        }

        // Per-branch metrics (bar charts)
        List<OrgDashboardResponse.BranchMetric> studentsPerBranch = branches.stream()
                .map(b -> OrgDashboardResponse.BranchMetric.builder()
                        .branchId(b.getId())
                        .branchName(b.getName())
                        .count(studentRepository.countByOrganizationIdAndBranchId(organizationId, b.getId()))
                        .build())
                .collect(Collectors.toList());

        List<OrgDashboardResponse.BranchMetric> facultyPerBranch = branches.stream()
                .map(b -> OrgDashboardResponse.BranchMetric.builder()
                        .branchId(b.getId())
                        .branchName(b.getName())
                        .count(facultyRepository.countByOrganizationIdAndBranchId(organizationId, b.getId()))
                        .build())
                .collect(Collectors.toList());

        List<OrgDashboardResponse.BranchMetric> classesPerBranch = branches.stream()
                .map(b -> OrgDashboardResponse.BranchMetric.builder()
                        .branchId(b.getId())
                        .branchName(b.getName())
                        .count(academicClassRepository.countByOrganizationIdAndBranchId(organizationId, b.getId()))
                        .build())
                .collect(Collectors.toList());

        // Users by role within org (pie chart)
        Map<String, Long> usersByRole = new LinkedHashMap<>();
        for (User.Role role : User.Role.values()) {
            long count = userRepository.countByOrganizationIdAndRole(organizationId, role);
            if (count > 0) usersByRole.put(role.name(), count);
        }

        // Audit action counts for this org
        Map<String, Long> auditCounts = new LinkedHashMap<>();
        for (AuditLog.Action action : AuditLog.Action.values()) {
            long count = auditLogRepository.countByOrganizationIdAndAction(organizationId, action);
            if (count > 0) auditCounts.put(action.name(), count);
        }

        // Recent activity for this org
        List<OrgDashboardResponse.RecentActivity> recentActivity = auditLogRepository
                .findTop10ByOrganizationIdOrderByTimestampDesc(organizationId)
                .stream()
                .map(log -> OrgDashboardResponse.RecentActivity.builder()
                        .action(log.getAction().name())
                        .entityType(log.getEntityType())
                        .entityName(log.getEntityName())
                        .performedByEmail(log.getPerformedByEmail())
                        .timestamp(log.getTimestamp() != null ? log.getTimestamp().toString() : null)
                        .build())
                .collect(Collectors.toList());

        return OrgDashboardResponse.builder()
                .organizationId(organizationId)
                .organizationName(org.getName())
                .totalBranches(totalBranches)
                .totalClasses(totalClasses)
                .totalStudents(totalStudents)
                .totalFaculty(totalFaculty)
                .totalUsers(totalUsers)
                .totalEnrollments(totalEnrollments)
                .totalFacultyAssignments(totalFacultyAssignments)
                .studentsByStatus(studentsByStatus)
                .studentsByGender(studentsByGender)
                .facultyByStatus(facultyByStatus)
                .studentsPerBranch(studentsPerBranch)
                .facultyPerBranch(facultyPerBranch)
                .classesPerBranch(classesPerBranch)
                .usersByRole(usersByRole)
                .auditActionCounts(auditCounts)
                .recentActivity(recentActivity)
                .build();
    }

    public BranchDashboardResponse getBranchDashboard(String organizationId, String branchId) {
        Organization org = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found: " + organizationId));

        Branch branch = branchRepository.findByIdAndOrganizationId(branchId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found: " + branchId));

        List<AcademicClass> classes = academicClassRepository.findByOrganizationIdAndBranchId(organizationId, branchId);

        long totalClasses = classes.size();
        long totalStudents = studentRepository.countByOrganizationIdAndBranchId(organizationId, branchId);
        long totalFaculty = facultyRepository.countByOrganizationIdAndBranchId(organizationId, branchId);
        long totalEnrollments = enrollmentRepository.countByOrganizationIdAndBranchId(organizationId, branchId);
        long totalFacultyAssignments = assignmentRepository.countByOrganizationIdAndBranchId(organizationId, branchId);

        // Students by status (pie chart)
        Map<String, Long> studentsByStatus = new LinkedHashMap<>();
        for (Student.StudentStatus status : Student.StudentStatus.values()) {
            long count = studentRepository.countByOrganizationIdAndBranchIdAndStatus(organizationId, branchId, status);
            if (count > 0) studentsByStatus.put(status.name(), count);
        }

        // Students by gender (pie chart)
        Map<String, Long> studentsByGender = new LinkedHashMap<>();
        for (String gender : List.of("Male", "Female", "Other")) {
            long count = studentRepository.countByOrganizationIdAndBranchIdAndGender(organizationId, branchId, gender);
            if (count > 0) studentsByGender.put(gender, count);
        }

        // Faculty by status (pie chart)
        Map<String, Long> facultyByStatus = new LinkedHashMap<>();
        for (Faculty.FacultyStatus status : Faculty.FacultyStatus.values()) {
            long count = facultyRepository.countByOrganizationIdAndBranchIdAndStatus(organizationId, branchId, status);
            if (count > 0) facultyByStatus.put(status.name(), count);
        }

        // Faculty by department (pie chart)
        Map<String, Long> facultyByDepartment = new LinkedHashMap<>();
        List<Faculty> branchFaculty = facultyRepository.findByOrganizationIdAndBranchId(organizationId, branchId);
        branchFaculty.stream()
                .filter(f -> f.getDepartment() != null)
                .collect(Collectors.groupingBy(Faculty::getDepartment, Collectors.counting()))
                .forEach(facultyByDepartment::put);

        // Students per class (bar chart)
        List<BranchDashboardResponse.ClassMetric> studentsPerClass = classes.stream()
                .map(c -> BranchDashboardResponse.ClassMetric.builder()
                        .classId(c.getId())
                        .className(c.getName())
                        .section(c.getSection())
                        .count(enrollmentRepository.countByClassIdAndStatus(c.getId(), StudentEnrollment.EnrollmentStatus.ACTIVE))
                        .build())
                .collect(Collectors.toList());

        // Recent activity (use org-level since audit logs don't have branchId)
        List<BranchDashboardResponse.RecentActivity> recentActivity = auditLogRepository
                .findTop10ByOrganizationIdOrderByTimestampDesc(organizationId)
                .stream()
                .map(log -> BranchDashboardResponse.RecentActivity.builder()
                        .action(log.getAction().name())
                        .entityType(log.getEntityType())
                        .entityName(log.getEntityName())
                        .performedByEmail(log.getPerformedByEmail())
                        .timestamp(log.getTimestamp() != null ? log.getTimestamp().toString() : null)
                        .build())
                .collect(Collectors.toList());

        return BranchDashboardResponse.builder()
                .organizationId(organizationId)
                .organizationName(org.getName())
                .branchId(branchId)
                .branchName(branch.getName())
                .totalClasses(totalClasses)
                .totalStudents(totalStudents)
                .totalFaculty(totalFaculty)
                .totalEnrollments(totalEnrollments)
                .totalFacultyAssignments(totalFacultyAssignments)
                .studentsByStatus(studentsByStatus)
                .studentsByGender(studentsByGender)
                .facultyByStatus(facultyByStatus)
                .facultyByDepartment(facultyByDepartment)
                .studentsPerClass(studentsPerClass)
                .recentActivity(recentActivity)
                .build();
    }
}