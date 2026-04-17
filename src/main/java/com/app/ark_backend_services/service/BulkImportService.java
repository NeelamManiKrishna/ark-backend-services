package com.app.ark_backend_services.service;

import com.app.ark_backend_services.dto.BulkImportResponse;
import com.app.ark_backend_services.dto.BulkImportResponse.RowError;
import com.app.ark_backend_services.exception.ResourceNotFoundException;
import com.app.ark_backend_services.model.*;
import com.app.ark_backend_services.model.AuditLog.Action;
import com.app.ark_backend_services.model.Student.GovtIdType;
import com.app.ark_backend_services.model.Student.StudentStatus;
import com.app.ark_backend_services.model.StudentEnrollment.EnrollmentStatus;
import com.app.ark_backend_services.repository.*;
import com.app.ark_backend_services.security.CurrentUser;
import com.app.ark_backend_services.util.ArkIdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BulkImportService {

    private static final int BATCH_SIZE = 100;
    private static final int MAX_ROWS = 10_000;

    private final StudentRepository studentRepository;
    private final FacultyRepository facultyRepository;
    private final AcademicClassRepository academicClassRepository;
    private final BranchRepository branchRepository;
    private final OrganizationRepository organizationRepository;
    private final StudentEnrollmentRepository enrollmentRepository;
    private final AuditLogService auditLogService;

    // ======================== STUDENTS ========================

    public BulkImportResponse importStudents(String organizationId, MultipartFile file) {
        validateAccess(organizationId);
        List<CSVRecord> records = parseFile(file);

        // Pre-cache valid branch and class IDs
        Set<String> validBranchIds = branchRepository.findByOrganizationId(organizationId).stream()
                .map(Branch::getId).collect(Collectors.toSet());
        Set<String> validClassIds = new HashSet<>();
        for (String branchId : validBranchIds) {
            academicClassRepository.findByOrganizationIdAndBranchId(organizationId, branchId)
                    .forEach(c -> validClassIds.add(c.getId()));
        }

        List<RowError> errors = new ArrayList<>();
        // Pair each student with its optional enrollment (null if no enrollment needed)
        List<Student> batch = new ArrayList<>();
        List<StudentEnrollment> enrollmentBatch = new ArrayList<>(); // same size as batch, 1:1 mapping
        int success = 0;

        for (int i = 0; i < records.size(); i++) {
            int rowNum = i + 2; // +2 for 1-based + header row
            CSVRecord rec = records.get(i);
            try {
                String firstName = requireField(rec, "firstName", rowNum);
                String lastName = requireField(rec, "lastName", rowNum);
                String branchId = requireField(rec, "branchId", rowNum);
                String govtIdTypeStr = requireField(rec, "govtIdType", rowNum);
                String govtIdNumber = requireField(rec, "govtIdNumber", rowNum);

                if (!validBranchIds.contains(branchId)) {
                    throw new IllegalArgumentException("Branch not found: " + branchId);
                }

                GovtIdType govtIdType = parseEnum(GovtIdType.class, govtIdTypeStr, "govtIdType");

                Student student = new Student();
                student.setArkId(ArkIdGenerator.generateStudentId());
                student.setOrganizationId(organizationId);
                student.setBranchId(branchId);
                student.setFirstName(firstName);
                student.setLastName(lastName);
                student.setEmail(getOptional(rec, "email"));
                student.setPhone(getOptional(rec, "phone"));
                student.setDateOfBirth(parseDate(getOptional(rec, "dateOfBirth")));
                student.setGender(getOptional(rec, "gender"));
                student.setAddress(getOptional(rec, "address"));
                student.setCity(getOptional(rec, "city"));
                student.setState(getOptional(rec, "state"));
                student.setZipCode(getOptional(rec, "zipCode"));
                student.setGuardianName(getOptional(rec, "guardianName"));
                student.setGuardianPhone(getOptional(rec, "guardianPhone"));
                student.setGuardianEmail(getOptional(rec, "guardianEmail"));
                student.setGovtIdType(govtIdType);
                student.setGovtIdNumber(govtIdNumber);
                student.setRollNumber(getOptional(rec, "rollNumber"));
                student.setEnrollmentDate(parseDate(getOptional(rec, "enrollmentDate")));
                student.setStatus(StudentStatus.ACTIVE);

                batch.add(student);

                // Prepare enrollment if classId + academicYear provided
                String classId = getOptional(rec, "classId");
                String academicYear = getOptional(rec, "academicYear");
                if (classId != null && academicYear != null) {
                    if (!validClassIds.contains(classId)) {
                        throw new IllegalArgumentException("Class not found: " + classId);
                    }
                    StudentEnrollment enrollment = new StudentEnrollment();
                    enrollment.setArkId(ArkIdGenerator.generateEnrollmentId());
                    enrollment.setOrganizationId(organizationId);
                    enrollment.setBranchId(branchId);
                    enrollment.setClassId(classId);
                    enrollment.setAcademicYear(academicYear);
                    enrollment.setEnrolledAt(student.getEnrollmentDate() != null ? student.getEnrollmentDate() : LocalDate.now());
                    enrollment.setStatus(EnrollmentStatus.ACTIVE);
                    enrollmentBatch.add(enrollment);
                } else {
                    enrollmentBatch.add(null); // no enrollment for this student
                }

                success++;

                if (batch.size() >= BATCH_SIZE) {
                    flushStudentBatch(batch, enrollmentBatch);
                }
            } catch (Exception e) {
                errors.add(new RowError(rowNum, e.getMessage()));
            }
        }

        // Flush remaining
        if (!batch.isEmpty()) {
            flushStudentBatch(batch, enrollmentBatch);
        }

        auditLogService.log(Action.CREATE, "Student", null, null, organizationId,
                "Bulk import: " + success + "/" + records.size() + " students created");
        log.info("Bulk import students for org {}: {}/{} success", organizationId, success, records.size());

        return BulkImportResponse.builder()
                .entityType("Student")
                .totalRows(records.size())
                .successCount(success)
                .failureCount(errors.size())
                .errors(errors)
                .build();
    }

    private void flushStudentBatch(List<Student> batch, List<StudentEnrollment> enrollmentBatch) {
        List<Student> saved = studentRepository.saveAll(batch);

        // batch and enrollmentBatch are 1:1 aligned — link enrollments to saved students
        List<StudentEnrollment> enrollmentsToSave = new ArrayList<>();
        for (int i = 0; i < saved.size(); i++) {
            StudentEnrollment enrollment = enrollmentBatch.get(i);
            if (enrollment != null) {
                enrollment.setStudentId(saved.get(i).getId());
                enrollmentsToSave.add(enrollment);
            }
        }

        if (!enrollmentsToSave.isEmpty()) {
            enrollmentRepository.saveAll(enrollmentsToSave);
        }

        batch.clear();
        enrollmentBatch.clear();
    }

    // ======================== FACULTY ========================

    public BulkImportResponse importFaculty(String organizationId, MultipartFile file) {
        validateAccess(organizationId);
        List<CSVRecord> records = parseFile(file);

        Set<String> validBranchIds = branchRepository.findByOrganizationId(organizationId).stream()
                .map(Branch::getId).collect(Collectors.toSet());

        List<RowError> errors = new ArrayList<>();
        List<Faculty> batch = new ArrayList<>();
        int success = 0;

        for (int i = 0; i < records.size(); i++) {
            int rowNum = i + 2;
            CSVRecord rec = records.get(i);
            try {
                String firstName = requireField(rec, "firstName", rowNum);
                String lastName = requireField(rec, "lastName", rowNum);
                String branchId = requireField(rec, "branchId", rowNum);
                String govtIdTypeStr = requireField(rec, "govtIdType", rowNum);
                String govtIdNumber = requireField(rec, "govtIdNumber", rowNum);

                if (!validBranchIds.contains(branchId)) {
                    throw new IllegalArgumentException("Branch not found: " + branchId);
                }

                Faculty.GovtIdType govtIdType = parseEnum(Faculty.GovtIdType.class, govtIdTypeStr, "govtIdType");

                Faculty faculty = new Faculty();
                faculty.setArkId(ArkIdGenerator.generateFacultyId());
                faculty.setOrganizationId(organizationId);
                faculty.setBranchId(branchId);
                faculty.setFirstName(firstName);
                faculty.setLastName(lastName);
                faculty.setEmail(getOptional(rec, "email"));
                faculty.setPhone(getOptional(rec, "phone"));
                faculty.setDateOfBirth(parseDate(getOptional(rec, "dateOfBirth")));
                faculty.setGender(getOptional(rec, "gender"));
                faculty.setAddress(getOptional(rec, "address"));
                faculty.setCity(getOptional(rec, "city"));
                faculty.setState(getOptional(rec, "state"));
                faculty.setZipCode(getOptional(rec, "zipCode"));
                faculty.setDepartment(getOptional(rec, "department"));
                faculty.setDesignation(getOptional(rec, "designation"));
                faculty.setQualifications(parsePipeList(getOptional(rec, "qualifications")));
                faculty.setSpecializations(parsePipeList(getOptional(rec, "specializations")));
                faculty.setJoiningDate(parseDate(getOptional(rec, "joiningDate")));
                faculty.setGovtIdType(govtIdType);
                faculty.setGovtIdNumber(govtIdNumber);
                faculty.setEmployeeId(getOptional(rec, "employeeId"));
                faculty.setStatus(Faculty.FacultyStatus.ACTIVE);

                batch.add(faculty);
                success++;

                if (batch.size() >= BATCH_SIZE) {
                    facultyRepository.saveAll(batch);
                    batch.clear();
                }
            } catch (Exception e) {
                errors.add(new RowError(rowNum, e.getMessage()));
            }
        }

        if (!batch.isEmpty()) {
            facultyRepository.saveAll(batch);
        }

        auditLogService.log(Action.CREATE, "Faculty", null, null, organizationId,
                "Bulk import: " + success + "/" + records.size() + " faculty created");
        log.info("Bulk import faculty for org {}: {}/{} success", organizationId, success, records.size());

        return BulkImportResponse.builder()
                .entityType("Faculty")
                .totalRows(records.size())
                .successCount(success)
                .failureCount(errors.size())
                .errors(errors)
                .build();
    }

    // ======================== ACADEMIC CLASSES ========================

    public BulkImportResponse importAcademicClasses(String organizationId, MultipartFile file) {
        validateAccess(organizationId);
        List<CSVRecord> records = parseFile(file);

        Set<String> validBranchIds = branchRepository.findByOrganizationId(organizationId).stream()
                .map(Branch::getId).collect(Collectors.toSet());

        // Pre-cache existing class keys to skip duplicates
        Set<String> existingKeys = new HashSet<>();
        for (String branchId : validBranchIds) {
            academicClassRepository.findByOrganizationIdAndBranchId(organizationId, branchId)
                    .forEach(c -> existingKeys.add(classKey(organizationId, c.getBranchId(), c.getName(), c.getSection(), c.getAcademicYear())));
        }

        List<RowError> errors = new ArrayList<>();
        List<AcademicClass> batch = new ArrayList<>();
        int success = 0;

        for (int i = 0; i < records.size(); i++) {
            int rowNum = i + 2;
            CSVRecord rec = records.get(i);
            try {
                String name = requireField(rec, "name", rowNum);
                String academicYear = requireField(rec, "academicYear", rowNum);
                String branchId = requireField(rec, "branchId", rowNum);
                String section = getOptional(rec, "section");

                if (!validBranchIds.contains(branchId)) {
                    throw new IllegalArgumentException("Branch not found: " + branchId);
                }

                String key = classKey(organizationId, branchId, name, section, academicYear);
                if (existingKeys.contains(key)) {
                    throw new IllegalArgumentException("Class already exists: " + name + " " + (section != null ? section : "") + " " + academicYear);
                }

                AcademicClass ac = new AcademicClass();
                ac.setOrganizationId(organizationId);
                ac.setBranchId(branchId);
                ac.setName(name);
                ac.setSection(section);
                ac.setAcademicYear(academicYear);
                ac.setCapacity(parseInteger(getOptional(rec, "capacity")));
                ac.setDescription(getOptional(rec, "description"));
                ac.setStatus(AcademicClass.ClassStatus.ACTIVE);

                batch.add(ac);
                existingKeys.add(key); // prevent duplicates within same CSV
                success++;

                if (batch.size() >= BATCH_SIZE) {
                    academicClassRepository.saveAll(batch);
                    batch.clear();
                }
            } catch (Exception e) {
                errors.add(new RowError(rowNum, e.getMessage()));
            }
        }

        if (!batch.isEmpty()) {
            academicClassRepository.saveAll(batch);
        }

        auditLogService.log(Action.CREATE, "AcademicClass", null, null, organizationId,
                "Bulk import: " + success + "/" + records.size() + " classes created");
        log.info("Bulk import classes for org {}: {}/{} success", organizationId, success, records.size());

        return BulkImportResponse.builder()
                .entityType("AcademicClass")
                .totalRows(records.size())
                .successCount(success)
                .failureCount(errors.size())
                .errors(errors)
                .build();
    }

    // ======================== BRANCHES ========================

    public BulkImportResponse importBranches(String organizationId, MultipartFile file) {
        validateAccess(organizationId);
        List<CSVRecord> records = parseFile(file);

        // Pre-cache existing branch names
        Set<String> existingNames = new HashSet<>();
        branchRepository.findByOrganizationId(organizationId)
                .forEach(b -> existingNames.add(b.getName().toLowerCase()));

        List<RowError> errors = new ArrayList<>();
        List<Branch> batch = new ArrayList<>();
        int success = 0;

        for (int i = 0; i < records.size(); i++) {
            int rowNum = i + 2;
            CSVRecord rec = records.get(i);
            try {
                String name = requireField(rec, "name", rowNum);

                if (existingNames.contains(name.toLowerCase())) {
                    throw new IllegalArgumentException("Branch already exists: " + name);
                }

                Branch branch = new Branch();
                branch.setArkId(ArkIdGenerator.generateBranchId());
                branch.setOrganizationId(organizationId);
                branch.setName(name);
                branch.setAddress(getOptional(rec, "address"));
                branch.setCity(getOptional(rec, "city"));
                branch.setState(getOptional(rec, "state"));
                branch.setZipCode(getOptional(rec, "zipCode"));
                branch.setContactEmail(getOptional(rec, "contactEmail"));
                branch.setContactPhone(getOptional(rec, "contactPhone"));
                branch.setStatus(Branch.BranchStatus.ACTIVE);

                batch.add(branch);
                existingNames.add(name.toLowerCase()); // prevent duplicates within same CSV
                success++;

                if (batch.size() >= BATCH_SIZE) {
                    branchRepository.saveAll(batch);
                    batch.clear();
                }
            } catch (Exception e) {
                errors.add(new RowError(rowNum, e.getMessage()));
            }
        }

        if (!batch.isEmpty()) {
            branchRepository.saveAll(batch);
        }

        auditLogService.log(Action.CREATE, "Branch", null, null, organizationId,
                "Bulk import: " + success + "/" + records.size() + " branches created");
        log.info("Bulk import branches for org {}: {}/{} success", organizationId, success, records.size());

        return BulkImportResponse.builder()
                .entityType("Branch")
                .totalRows(records.size())
                .successCount(success)
                .failureCount(errors.size())
                .errors(errors)
                .build();
    }

    // ======================== SAMPLE CSV ========================

    public String getSampleCsv(String entityType) {
        return switch (entityType.toLowerCase()) {
            case "students" -> """
                    firstName,lastName,email,phone,dateOfBirth,gender,address,city,state,zipCode,guardianName,guardianPhone,guardianEmail,govtIdType,govtIdNumber,rollNumber,branchId,classId,academicYear,enrollmentDate
                    John,Doe,john.doe@example.com,+1-555-0001,2010-05-15,Male,123 Main St,Springfield,IL,62704,Jane Doe,+1-555-0002,jane.doe@example.com,AADHAAR,1234-5678-9012,R001,<branchId>,<classId>,2025-2026,2025-06-15""";
            case "faculty" -> """
                    firstName,lastName,email,phone,dateOfBirth,gender,address,city,state,zipCode,department,designation,qualifications,specializations,joiningDate,govtIdType,govtIdNumber,employeeId,branchId
                    Jane,Smith,jane.smith@example.com,+1-555-0010,1985-03-20,Female,456 Oak Ave,Springfield,IL,62704,Science,Senior Teacher,M.Sc Physics|B.Ed,Thermodynamics|Optics,2020-08-01,PAN,ABCDE1234F,EMP-001,<branchId>""";
            case "academic-classes" -> """
                    name,section,academicYear,capacity,description,branchId
                    Grade 1,A,2025-2026,40,Grade 1 Section A,<branchId>""";
            case "branches" -> """
                    name,address,city,state,zipCode,contactEmail,contactPhone
                    South Campus,789 South Blvd,Springfield,IL,62710,south@school.edu,+1-555-0300""";
            default -> throw new IllegalArgumentException("Unknown entity type: " + entityType + ". Valid types: students, faculty, academic-classes, branches");
        };
    }

    // ======================== HELPERS ========================

    private void validateAccess(String organizationId) {
        if (!CurrentUser.belongsToOrg(organizationId)) {
            throw new AccessDeniedException("You do not have access to this organization");
        }
        if (!organizationRepository.existsById(organizationId)) {
            throw new ResourceNotFoundException("Organization not found: " + organizationId);
        }
    }

    private List<CSVRecord> parseFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("CSV file is required and must not be empty");
        }

        try (Reader reader = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8);
             CSVParser parser = CSVFormat.DEFAULT.builder()
                     .setHeader()
                     .setSkipHeaderRecord(true)
                     .setTrim(true)
                     .setIgnoreEmptyLines(true)
                     .build()
                     .parse(reader)) {

            List<CSVRecord> records = parser.getRecords();
            if (records.isEmpty()) {
                throw new IllegalArgumentException("CSV file contains no data rows");
            }
            if (records.size() > MAX_ROWS) {
                throw new IllegalArgumentException("CSV exceeds maximum of " + MAX_ROWS + " rows. Got: " + records.size());
            }
            return records;
        } catch (IllegalArgumentException e) {
            throw e; // re-throw our own validation errors
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse CSV file: " + e.getMessage());
        }
    }

    private String requireField(CSVRecord rec, String column, int rowNum) {
        if (!rec.isMapped(column)) {
            throw new IllegalArgumentException("Missing required column: " + column);
        }
        String value = rec.get(column);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Row " + rowNum + ": " + column + " is required");
        }
        return value.trim();
    }

    private String getOptional(CSVRecord rec, String column) {
        if (!rec.isMapped(column)) return null;
        String value = rec.get(column);
        return (value == null || value.isBlank()) ? null : value.trim();
    }

    private LocalDate parseDate(String value) {
        if (value == null) return null;
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid date format: " + value + " (expected yyyy-MM-dd)");
        }
    }

    private Integer parseInteger(String value) {
        if (value == null) return null;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid number: " + value);
        }
    }

    private <T extends Enum<T>> T parseEnum(Class<T> enumClass, String value, String fieldName) {
        try {
            return Enum.valueOf(enumClass, value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid " + fieldName + ": " + value + ". Valid values: " + Arrays.toString(enumClass.getEnumConstants()));
        }
    }

    private List<String> parsePipeList(String value) {
        if (value == null) return null;
        return Arrays.stream(value.split("\\|"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    private String classKey(String orgId, String branchId, String name, String section, String year) {
        return orgId + "|" + branchId + "|" + name + "|" + (section != null ? section : "") + "|" + year;
    }
}
