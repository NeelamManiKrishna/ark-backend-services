package com.app.ark_backend_services;

import com.app.ark_backend_services.migration.MigrationRunner;
import com.app.ark_backend_services.model.*;
import com.app.ark_backend_services.repository.*;
import com.app.ark_backend_services.util.ArkIdGenerator;
import com.jayway.jsonpath.JsonPath;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Catalog ids, class progression rules, gender values and copied-branchId consistency. */
@AutoConfigureMockMvc
class OpenItemsAcceptanceTest extends AbstractMongoIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired MongoTemplate mongo;
    @Autowired MigrationRunner migrations;
    @Autowired PasswordEncoder encoder;
    @Autowired UserRepository users;
    @Autowired OrganizationRepository orgs;
    @Autowired BranchRepository branches;
    @Autowired AcademicClassRepository classes;
    @Autowired StudentRepository students;
    @Autowired FacultyRepository faculty;
    @Autowired StudentEnrollmentRepository enrollments;
    @Autowired ExaminationRepository exams;
    @Autowired ExamSubjectRepository examSubjects;
    @Autowired ExamResultRepository results;
    @Autowired FacultyAssignmentRepository assignments;
    @Autowired ClassProgressionRepository progressions;
    @Autowired AcademicYearRepository years;
    @Autowired SubjectRepository subjectCatalog;

    Organization org;
    Branch branch, other;
    AcademicYear year;
    Subject math;
    String token, base;

    @BeforeEach
    void seed() throws Exception {
        results.deleteAll(); examSubjects.deleteAll(); exams.deleteAll(); enrollments.deleteAll(); assignments.deleteAll();
        progressions.deleteAll(); classes.deleteAll(); students.deleteAll(); faculty.deleteAll(); users.deleteAll();
        branches.deleteAll(); orgs.deleteAll(); years.deleteAll(); subjectCatalog.deleteAll();
        mongo.getCollection("migrations").deleteMany(new Document());

        org = new Organization();
        org.setArkId(ArkIdGenerator.generateOrgId());
        org.setName("Org Open");
        org.setStatus(Organization.OrganizationStatus.ACTIVE);
        org = orgs.save(org);
        branch = branch("Main");
        other = branch("Other");

        User u = new User();
        u.setFullName("OA");
        u.setEmail("oa@open.test");
        u.setPassword(encoder.encode("Password@123"));
        u.setRole(User.Role.ORG_ADMIN);
        u.setOrganizationId(org.getId());
        u.setStatus(User.UserStatus.ACTIVE);
        users.save(u);

        year = new AcademicYear();
        year.setOrganizationId(org.getId());
        year.setLabel("2025-2026");
        year = years.save(year);
        math = new Subject();
        math.setOrganizationId(org.getId());
        math.setCode("MATH");
        math.setName("Mathematics");
        math = subjectCatalog.save(math);

        MvcResult res = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"oa@open.test\",\"password\":\"Password@123\"}")).andReturn();
        token = JsonPath.read(res.getResponse().getContentAsString(), "$.accessToken");
        base = "/api/v1/organizations/" + org.getId();
    }

    // ---- catalog ids (additive) ----

    @Test
    void recordsCanBeCreatedByCatalogIdAndTextStillWorks() throws Exception {
        // class by id only: the label is filled in and the id is returned
        String classBody = "{\"name\":\"Grade 1\",\"section\":\"A\",\"academicYearId\":\"" + year.getId() + "\"}";
        MvcResult created = mvc.perform(post(base + "/branches/" + branch.getId() + "/classes").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(classBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.academicYear").value("2025-2026"))
                .andExpect(jsonPath("$.academicYearId").value(year.getId())).andReturn();
        String classId = JsonPath.read(created.getResponse().getContentAsString(), "$.id");

        // class by text: the id is looked up in the catalog
        mvc.perform(post(base + "/branches/" + branch.getId() + "/classes").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Grade 2\",\"section\":\"A\",\"academicYear\":\"2025-2026\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.academicYearId").value(year.getId()));
        // a year that is not in the catalog still works, without an id
        mvc.perform(post(base + "/branches/" + branch.getId() + "/classes").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Grade 3\",\"section\":\"A\",\"academicYear\":\"2030-2031\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.academicYearId").doesNotExist());
        // disagreeing id and text, unknown id, no year at all
        mvc.perform(post(base + "/branches/" + branch.getId() + "/classes").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"X\",\"section\":\"A\",\"academicYear\":\"2024-2025\",\"academicYearId\":\"" + year.getId() + "\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post(base + "/branches/" + branch.getId() + "/classes").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"X\",\"section\":\"A\",\"academicYearId\":\"nope\"}"))
                .andExpect(status().isNotFound());
        mvc.perform(post(base + "/branches/" + branch.getId() + "/classes").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"X\",\"section\":\"A\"}"))
                .andExpect(status().isBadRequest());

        // exam, subject and assignment by ids
        MvcResult exam = mvc.perform(post(base + "/branches/" + branch.getId() + "/examinations").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Final\",\"examType\":\"FINAL\",\"academicYearId\":\"" + year.getId() + "\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.academicYear").value("2025-2026"))
                .andExpect(jsonPath("$.academicYearId").value(year.getId())).andReturn();
        String examId = JsonPath.read(exam.getResponse().getContentAsString(), "$.id");
        mvc.perform(post(base + "/examinations/" + examId + "/subjects").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"classId\":\"" + classId + "\",\"subjectId\":\"" + math.getId() + "\",\"maxMarks\":100,\"passingMarks\":35}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.subjectName").value("Mathematics"))
                .andExpect(jsonPath("$.subjectId").value(math.getId()));

        Faculty f = new Faculty();
        f.setArkId(ArkIdGenerator.generateFacultyId());
        f.setOrganizationId(org.getId());
        f.setBranchId(branch.getId());
        f.setFirstName("Fay");
        f.setLastName("Teacher");
        f = faculty.save(f);
        mvc.perform(post(base + "/branches/" + branch.getId() + "/faculty-assignments").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"facultyId\":\"" + f.getId() + "\",\"classId\":\"" + classId + "\",\"academicYearId\":\"" + year.getId()
                                + "\",\"subjectId\":\"" + math.getId() + "\",\"assignmentType\":\"SUBJECT_TEACHER\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.subjectName").value("Mathematics"))
                .andExpect(jsonPath("$.academicYear").value("2025-2026"))
                .andExpect(jsonPath("$.branchId").value(branch.getId()));
        // the copied branchId equals the class's branch
        assertThat(assignments.findAll()).allMatch(a -> a.getBranchId().equals(classes.findById(a.getClassId()).orElseThrow().getBranchId()));
    }

    @Test
    void backfillMigrationLinksExistingRecordsToTheCatalog() {
        AcademicClass legacy = clazz("Legacy", "2025-2026", branch);
        assertThat(legacy.getAcademicYearId()).isNull();
        migrations.execute(false);
        assertThat(classes.findById(legacy.getId()).orElseThrow().getAcademicYearId()).isEqualTo(year.getId());
    }

    // ---- class progression ----

    @Test
    void progressionNeedsGapFreeOrdersAndTerminalLast() throws Exception {
        String url = base + "/branches/" + branch.getId() + "/class-progression";
        // gap
        mvc.perform(put(url).header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                .content("{\"sequence\":[{\"className\":\"A\",\"displayOrder\":1,\"isTerminal\":false},{\"className\":\"B\",\"displayOrder\":3,\"isTerminal\":true}]}"))
                .andExpect(status().isBadRequest());
        // terminal not last
        mvc.perform(put(url).header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                .content("{\"sequence\":[{\"className\":\"A\",\"displayOrder\":1,\"isTerminal\":true},{\"className\":\"B\",\"displayOrder\":2,\"isTerminal\":false}]}"))
                .andExpect(status().isBadRequest());
        // valid; "Grade 2" has no class yet, which is reported but allowed (promotion creates it)
        clazz("Grade 1", "2025-2026", branch);
        mvc.perform(put(url).header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                .content("{\"sequence\":[{\"className\":\"Grade 1\",\"displayOrder\":1,\"isTerminal\":false},{\"className\":\"Grade 2\",\"displayOrder\":2,\"isTerminal\":true}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.classesNotCreatedYet.length()").value(1))
                .andExpect(jsonPath("$.classesNotCreatedYet[0]").value("Grade 2"));
    }

    // ---- gender ----

    @Test
    void genderIsValidatedAndStoredNormalized() throws Exception {
        String url = base + "/students";
        mvc.perform(post(url).header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"branchId\":\"" + branch.getId() + "\",\"firstName\":\"G\",\"lastName\":\"One\",\"gender\":\"female\",\"govtIdType\":\"PAN\",\"govtIdNumber\":\"G1\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.gender").value("FEMALE"));
        mvc.perform(post(url).header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"branchId\":\"" + branch.getId() + "\",\"firstName\":\"G\",\"lastName\":\"Two\",\"gender\":\"unknown\",\"govtIdType\":\"PAN\",\"govtIdNumber\":\"G2\"}"))
                .andExpect(status().isBadRequest());
        // bulk import reports the bad row and keeps the good one
        String csv = "firstName,lastName,branchId,govtIdType,govtIdNumber,gender\n"
                + "A,Good," + branch.getId() + ",PAN,X1,Male\n"
                + "B,Bad," + branch.getId() + ",PAN,X2,robot\n";
        mvc.perform(multipart(base + "/bulk-import/students").file(new MockMultipartFile("file", "s.csv", "text/csv", csv.getBytes()))
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.successCount").value(1)).andExpect(jsonPath("$.errors[0].row").value(3));
        assertThat(students.findAll().stream().filter(s -> "Good".equals(s.getLastName())).findFirst().orElseThrow().getGender()).isEqualTo("MALE");
    }

    @Test
    void genderMigrationNormalizesAndReportsUnmappableValues() {
        Student s = student("Legacy", "Female");
        Student odd = student("Odd", "n/a");
        migrations.execute(false);
        assertThat(students.findById(s.getId()).orElseThrow().getGender()).isEqualTo("FEMALE");
        assertThat(students.findById(odd.getId()).orElseThrow().getGender()).isEqualTo("n/a");
    }

    // ---- copied branchId ----

    @Test
    void migrationRepairsCopiedBranchIds() {
        AcademicClass c = clazz("Grade 1", "2025-2026", branch);
        FacultyAssignment a = new FacultyAssignment();
        a.setArkId(ArkIdGenerator.generateAssignmentId());
        a.setOrganizationId(org.getId());
        a.setBranchId(other.getId()); // wrong on purpose
        a.setFacultyId("f1");
        a.setClassId(c.getId());
        a.setSubjectName("Math");
        a.setAcademicYear("2025-2026");
        a.setAssignmentType(FacultyAssignment.AssignmentType.SUBJECT_TEACHER);
        a.setStatus(FacultyAssignment.AssignmentStatus.ACTIVE);
        a = assignments.save(a);

        String dry = String.join("\n", migrations.execute(true));
        assertThat(dry).contains("1 faculty assignment(s) would be corrected");
        assertThat(assignments.findById(a.getId()).orElseThrow().getBranchId()).isEqualTo(other.getId());
        migrations.execute(false);
        assertThat(assignments.findById(a.getId()).orElseThrow().getBranchId()).isEqualTo(branch.getId());
    }

    // ---- helpers ----

    private Branch branch(String name) {
        Branch b = new Branch();
        b.setArkId(ArkIdGenerator.generateBranchId());
        b.setOrganizationId(org.getId());
        b.setName(name);
        b.setStatus(Branch.BranchStatus.ACTIVE);
        return branches.save(b);
    }

    private AcademicClass clazz(String name, String yearLabel, Branch b) {
        AcademicClass c = new AcademicClass();
        c.setOrganizationId(org.getId());
        c.setBranchId(b.getId());
        c.setName(name);
        c.setSection("A");
        c.setAcademicYear(yearLabel);
        c.setStatus(AcademicClass.ClassStatus.ACTIVE);
        return classes.save(c);
    }

    private Student student(String first, String gender) {
        Student s = new Student();
        s.setArkId(ArkIdGenerator.generateStudentId());
        s.setOrganizationId(org.getId());
        s.setBranchId(branch.getId());
        s.setFirstName(first);
        s.setLastName("T");
        s.setGender(gender);
        s.setStatus(Student.StudentStatus.ACTIVE);
        return students.save(s);
    }
}
