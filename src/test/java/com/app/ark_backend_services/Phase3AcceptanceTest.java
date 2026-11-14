package com.app.ark_backend_services;

import com.app.ark_backend_services.migration.MigrationRunner;
import com.app.ark_backend_services.model.*;
import com.app.ark_backend_services.repository.*;
import com.app.ark_backend_services.util.ArkIdGenerator;
import com.jayway.jsonpath.JsonPath;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Phase 3 acceptance tests: search, catalogs, reports, indexes and migrations. */
@AutoConfigureMockMvc
class Phase3AcceptanceTest extends AbstractMongoIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired MongoTemplate mongo;
    @Autowired MigrationRunner migrations;
    @Autowired PasswordEncoder encoder;
    @Autowired UserRepository users;
    @Autowired OrganizationRepository orgs;
    @Autowired BranchRepository branches;
    @Autowired AcademicClassRepository classes;
    @Autowired StudentRepository students;
    @Autowired StudentEnrollmentRepository enrollments;
    @Autowired ExaminationRepository exams;
    @Autowired ExamSubjectRepository subjects;
    @Autowired ExamResultRepository results;
    @Autowired AcademicYearRepository years;
    @Autowired SubjectRepository subjectCatalog;

    Organization org;
    Branch b1, b2;
    AcademicClass c1, c2;
    String orgAdminToken, adminB1Token;

    @BeforeEach
    void seed() throws Exception {
        results.deleteAll(); subjects.deleteAll(); exams.deleteAll(); enrollments.deleteAll(); classes.deleteAll();
        students.deleteAll(); users.deleteAll(); branches.deleteAll(); orgs.deleteAll(); years.deleteAll();
        subjectCatalog.deleteAll();
        mongo.getCollection("migrations").deleteMany(new Document());
        mongo.getCollection("faculty").deleteMany(new Document());

        org = new Organization();
        org.setArkId(ArkIdGenerator.generateOrgId());
        org.setName("Org P3");
        org.setStatus(Organization.OrganizationStatus.ACTIVE);
        org = orgs.save(org);
        b1 = branch("North");
        b2 = branch("South");
        user("oa@p3.test", User.Role.ORG_ADMIN, null);
        user("admin@p3.test", User.Role.ADMIN, b1);
        c1 = clazz(b1, "Grade 1");
        c2 = clazz(b2, "Grade 1");
        orgAdminToken = login("oa@p3.test");
        adminB1Token = login("admin@p3.test");
    }

    // ---- indexes ----

    @Test
    void declaredIndexesExist() {
        List<String> studentIdx = mongo.indexOps("students").getIndexInfo().stream().map(i -> i.getName()).toList();
        assertThat(studentIdx).contains("org_branch_idx", "org_status_idx");
        List<String> userIdx = mongo.indexOps("users").getIndexInfo().stream().map(i -> i.getName()).toList();
        assertThat(userIdx).contains("email_unique_idx");
        assertThat(mongo.indexOps("users").getIndexInfo()).anyMatch(i -> i.getName().equals("email_unique_idx") && i.isUnique());
    }

    // ---- q search ----

    @Test
    void searchMatchesPrefixesAndStaysInsideTheCallersBranch() throws Exception {
        student(b1, "Alice", "Smith", "A1");
        student(b1, "Alan", "Jones", "A2");
        student(b2, "Alicia", "Other", "A3");
        String url = "/api/v1/organizations/" + org.getId() + "/students?q=ali";

        mvc.perform(get(url).header("Authorization", "Bearer " + orgAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2)); // Alice + Alicia (case-insensitive prefix)
        // the branch admin cannot see the other branch even through search
        mvc.perform(get(url).header("Authorization", "Bearer " + adminB1Token))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].firstName").value("Alice"));
        // by roll number / ARK id prefix
        mvc.perform(get("/api/v1/organizations/" + org.getId() + "/students?q=A2").header("Authorization", "Bearer " + orgAdminToken))
                .andExpect(jsonPath("$.content.length()").value(1));
        // regex characters are literal, never an injection
        mvc.perform(get("/api/v1/organizations/" + org.getId() + "/students?q=.*").header("Authorization", "Bearer " + orgAdminToken))
                .andExpect(jsonPath("$.content.length()").value(0));
        // other resources
        mvc.perform(get("/api/v1/organizations/" + org.getId() + "/branches?q=nor").header("Authorization", "Bearer " + orgAdminToken))
                .andExpect(jsonPath("$.content.length()").value(1));
        mvc.perform(get("/api/v1/organizations/" + org.getId() + "/users?q=admin").header("Authorization", "Bearer " + adminB1Token))
                .andExpect(jsonPath("$.content.length()").value(1));
        mvc.perform(get("/api/v1/organizations/" + org.getId() + "/branches/" + b1.getId() + "/classes?q=gra")
                .header("Authorization", "Bearer " + adminB1Token)).andExpect(jsonPath("$.content.length()").value(1));
    }

    // ---- catalogs ----

    @Test
    void academicYearAndSubjectCatalogs() throws Exception {
        String y = "/api/v1/organizations/" + org.getId() + "/academic-years";
        mvc.perform(post(y).header("Authorization", "Bearer " + orgAdminToken).contentType(MediaType.APPLICATION_JSON)
                .content("{\"label\":\"2025\"}")).andExpect(status().isUnprocessableEntity());
        mvc.perform(post(y).header("Authorization", "Bearer " + orgAdminToken).contentType(MediaType.APPLICATION_JSON)
                .content("{\"label\":\"2025-2027\"}")).andExpect(status().isBadRequest());
        mvc.perform(post(y).header("Authorization", "Bearer " + orgAdminToken).contentType(MediaType.APPLICATION_JSON)
                .content("{\"label\":\"2025-2026\",\"current\":true}")).andExpect(status().isCreated());
        mvc.perform(post(y).header("Authorization", "Bearer " + orgAdminToken).contentType(MediaType.APPLICATION_JSON)
                .content("{\"label\":\"2025-2026\"}")).andExpect(status().isConflict());
        mvc.perform(post(y).header("Authorization", "Bearer " + orgAdminToken).contentType(MediaType.APPLICATION_JSON)
                .content("{\"label\":\"2026-2027\",\"current\":true}")).andExpect(status().isCreated());
        assertThat(years.findAll().stream().filter(AcademicYear::isCurrent).map(AcademicYear::getLabel)).containsExactly("2026-2027");
        // ADMIN may read but not write
        mvc.perform(get(y).header("Authorization", "Bearer " + adminB1Token)).andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2));
        mvc.perform(post(y).header("Authorization", "Bearer " + adminB1Token).contentType(MediaType.APPLICATION_JSON)
                .content("{\"label\":\"2027-2028\"}")).andExpect(status().isForbidden());
        // a year used by a class cannot be deleted
        String id1 = years.findAll().stream().filter(a -> a.getLabel().equals("2025-2026")).findFirst().orElseThrow().getId();
        mvc.perform(delete(y + "/" + id1).header("Authorization", "Bearer " + orgAdminToken)).andExpect(status().isConflict());

        String s = "/api/v1/organizations/" + org.getId() + "/subjects";
        mvc.perform(post(s).header("Authorization", "Bearer " + orgAdminToken).contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"math\",\"name\":\"Mathematics\"}")).andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("MATH"));
        mvc.perform(post(s).header("Authorization", "Bearer " + orgAdminToken).contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"MATH\",\"name\":\"Again\"}")).andExpect(status().isConflict());
        mvc.perform(get(s + "?q=mat").header("Authorization", "Bearer " + adminB1Token))
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    // ---- reports ----

    @Test
    void enrollmentAndPerformanceReportsWithCsv() throws Exception {
        Student s1 = student(b1, "R1", "T", "R1"), s2 = student(b1, "R2", "T", "R2"), s3 = student(b2, "R3", "T", "R3");
        enroll(s1, c1, b1, StudentEnrollment.EnrollmentStatus.ACTIVE);
        enroll(s2, c1, b1, StudentEnrollment.EnrollmentStatus.COMPLETED);
        enroll(s3, c2, b2, StudentEnrollment.EnrollmentStatus.ACTIVE);

        String base = "/api/v1/organizations/" + org.getId() + "/reports/enrollment?academicYear=2025-2026";
        mvc.perform(get(base).header("Authorization", "Bearer " + orgAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rows.length()").value(3)) // two classes + totals
                .andExpect(jsonPath("$.rows[2].branch").value("TOTAL"))
                .andExpect(jsonPath("$.rows[2].total").value(3))
                .andExpect(jsonPath("$.rows[2].active").value(2));
        // branch admin only gets their own branch (and totals)
        mvc.perform(get(base).header("Authorization", "Bearer " + adminB1Token))
                .andExpect(jsonPath("$.rows[1].total").value(2));
        mvc.perform(get(base + "&branchId=" + b2.getId()).header("Authorization", "Bearer " + adminB1Token))
                .andExpect(status().isForbidden());
        MvcResult csv = mvc.perform(get(base + "&format=csv").header("Authorization", "Bearer " + orgAdminToken))
                .andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith("text/csv")).andReturn();
        assertThat(csv.getResponse().getContentAsString()).startsWith("branch,class,section,total,active,completed")
                .contains("TOTAL");

        // performance: 2 PASS (40, 50), 1 FAIL (10), 1 ABSENT
        Examination exam = new Examination();
        exam.setArkId(ArkIdGenerator.generateExamId());
        exam.setOrganizationId(org.getId());
        exam.setBranchId(b1.getId());
        exam.setName("Final");
        exam.setAcademicYear("2025-2026");
        exam.setStatus(Examination.ExamStatus.COMPLETED);
        exam = exams.save(exam);
        ExamSubject es = new ExamSubject();
        es.setExaminationId(exam.getId());
        es.setOrganizationId(org.getId());
        es.setBranchId(b1.getId());
        es.setClassId(c1.getId());
        es.setSubjectName("Math");
        es.setMaxMarks(100.0);
        es.setPassingMarks(35.0);
        es = subjects.save(es);
        result(exam, es, student(b1, "P1", "T", "P1"), 40.0, ExamResult.ResultStatus.PASS);
        result(exam, es, student(b1, "P2", "T", "P2"), 50.0, ExamResult.ResultStatus.PASS);
        result(exam, es, student(b1, "P3", "T", "P3"), 10.0, ExamResult.ResultStatus.FAIL);
        result(exam, es, student(b1, "P4", "T", "P4"), null, ExamResult.ResultStatus.ABSENT);

        mvc.perform(get("/api/v1/organizations/" + org.getId() + "/reports/performance?academicYear=2025-2026")
                        .header("Authorization", "Bearer " + orgAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rows.length()").value(1))
                .andExpect(jsonPath("$.rows[0].appeared").value(3))
                .andExpect(jsonPath("$.rows[0].absent").value(1))
                .andExpect(jsonPath("$.rows[0].passed").value(2))
                .andExpect(jsonPath("$.rows[0].passPercentage").value(66.67))
                .andExpect(jsonPath("$.rows[0].average").value(33.33))
                .andExpect(jsonPath("$.rows[0].highest").value(50.0))
                .andExpect(jsonPath("$.rows[0].lowest").value(10.0));
        mvc.perform(get("/api/v1/organizations/" + org.getId() + "/reports/performance?academicYear=bad")
                .header("Authorization", "Bearer " + orgAdminToken)).andExpect(status().isBadRequest());
    }

    // ---- migrations ----

    @Test
    void migrationsDryRunChangesNothingAndRealRunRunsOnce() {
        Student plain = student(b1, "Plain", "T", "M1");
        // simulate legacy data: plaintext govt id and the old classId field
        mongo.getCollection("students").updateOne(new Document("_id", new org.bson.types.ObjectId(plain.getId())),
                new Document("$set", new Document("govtIdNumber", "PLAINTEXT-123").append("classId", "legacy")));

        List<String> dry = migrations.execute(true);
        assertThat(String.join("\n", dry)).contains("DRY RUN").contains("would be encrypted");
        Document untouched = rawStudent(plain.getId());
        assertThat(untouched.getString("govtIdNumber")).isEqualTo("PLAINTEXT-123");
        assertThat(mongo.getCollection("migrations").countDocuments()).isZero();

        migrations.execute(false);
        Document migrated = rawStudent(plain.getId());
        assertThat(migrated.getString("govtIdNumber")).startsWith("enc:v1:");
        assertThat(migrated.containsKey("classId")).isFalse();
        assertThat(mongo.getCollection("migrations").countDocuments()).isEqualTo(6);
        // the app still reads the original value
        assertThat(students.findById(plain.getId()).orElseThrow().getGovtIdNumber()).isEqualTo("PLAINTEXT-123");

        // second run: everything already applied
        assertThat(String.join("\n", migrations.execute(false))).contains("already applied").doesNotContain("encrypted");
    }

    @Test
    void duplicateEmailsFailTheCheckAndAreNotRecorded() {
        user("Dup@p3.test", User.Role.ORG_ADMIN, null);
        user("dup@p3.test", User.Role.ORG_ADMIN, null); // differs only by case, allowed by the exact-match index
        String report = String.join("\n", migrations.execute(false));
        assertThat(report).contains("duplicate email (case-insensitive): dup@p3.test").contains("FAILED");
        assertThat(mongo.getCollection("migrations").find(new Document("_id", "002-check-user-email-duplicates")).first()).isNull();
    }

    // ---- helpers ----

    private Document rawStudent(String id) {
        return mongo.getCollection("students").find(new Document("_id", new org.bson.types.ObjectId(id))).first();
    }

    private Branch branch(String name) {
        Branch b = new Branch();
        b.setArkId(ArkIdGenerator.generateBranchId());
        b.setOrganizationId(org.getId());
        b.setName(name);
        b.setStatus(Branch.BranchStatus.ACTIVE);
        return branches.save(b);
    }

    private User user(String email, User.Role role, Branch b) {
        User u = new User();
        u.setFullName(email);
        u.setEmail(email);
        u.setPassword(encoder.encode("Password@123"));
        u.setRole(role);
        u.setOrganizationId(org.getId());
        u.setBranchId(b == null ? null : b.getId());
        u.setStatus(User.UserStatus.ACTIVE);
        return users.save(u);
    }

    private AcademicClass clazz(Branch b, String name) {
        AcademicClass c = new AcademicClass();
        c.setOrganizationId(org.getId());
        c.setBranchId(b.getId());
        c.setName(name);
        c.setSection("A");
        c.setAcademicYear("2025-2026");
        c.setStatus(AcademicClass.ClassStatus.ACTIVE);
        return classes.save(c);
    }

    private Student student(Branch b, String first, String last, String roll) {
        Student s = new Student();
        s.setArkId(ArkIdGenerator.generateStudentId());
        s.setOrganizationId(org.getId());
        s.setBranchId(b.getId());
        s.setFirstName(first);
        s.setLastName(last);
        s.setRollNumber(roll);
        s.setGovtIdNumber("ID-" + roll);
        s.setStatus(Student.StudentStatus.ACTIVE);
        return students.save(s);
    }

    private void enroll(Student s, AcademicClass c, Branch b, StudentEnrollment.EnrollmentStatus st) {
        StudentEnrollment e = new StudentEnrollment();
        e.setArkId(ArkIdGenerator.generateEnrollmentId());
        e.setOrganizationId(org.getId());
        e.setBranchId(b.getId());
        e.setStudentId(s.getId());
        e.setClassId(c.getId());
        e.setAcademicYear("2025-2026");
        e.setEnrolledAt(LocalDate.now());
        e.setStatus(st);
        enrollments.save(e);
    }

    private void result(Examination e, ExamSubject es, Student s, Double marks, ExamResult.ResultStatus st) {
        ExamResult r = new ExamResult();
        r.setExaminationId(e.getId());
        r.setExamSubjectId(es.getId());
        r.setOrganizationId(org.getId());
        r.setBranchId(e.getBranchId());
        r.setClassId(es.getClassId());
        r.setStudentId(s.getId());
        r.setMarksObtained(marks);
        r.setStatus(st);
        results.save(r);
    }

    private String login(String email) throws Exception {
        MvcResult res = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"Password@123\"}")).andExpect(status().isOk()).andReturn();
        return JsonPath.read(res.getResponse().getContentAsString(), "$.accessToken");
    }
}
