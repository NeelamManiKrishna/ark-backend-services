package com.app.ark_backend_services;

import com.app.ark_backend_services.model.*;
import com.app.ark_backend_services.repository.*;
import com.app.ark_backend_services.util.ArkIdGenerator;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Phase 2 acceptance tests: one per data bug fixed. */
@AutoConfigureMockMvc
class DataBugsAcceptanceTest extends AbstractMongoIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired PasswordEncoder encoder;
    @Autowired UserRepository users;
    @Autowired OrganizationRepository orgs;
    @Autowired BranchRepository branches;
    @Autowired AcademicClassRepository classes;
    @Autowired StudentRepository students;
    @Autowired StudentEnrollmentRepository enrollments;
    @Autowired ClassProgressionRepository progressions;
    @Autowired ExaminationRepository exams;
    @Autowired ExamSubjectRepository subjects;
    @Autowired ExamResultRepository results;
    @Autowired AuditLogRepository audit;
    @Autowired RefreshSessionRepository sessions;

    Organization org;
    Branch branch;
    User orgAdmin;
    AcademicClass grade1, grade2;
    Student s1, s2;
    String token;

    @BeforeEach
    void seed() throws Exception {
        results.deleteAll(); subjects.deleteAll(); exams.deleteAll(); enrollments.deleteAll();
        progressions.deleteAll(); classes.deleteAll(); students.deleteAll(); users.deleteAll();
        branches.deleteAll(); orgs.deleteAll(); sessions.deleteAll(); audit.deleteAll();

        org = new Organization();
        org.setArkId(ArkIdGenerator.generateOrgId());
        org.setName("Org P2");
        org.setStatus(Organization.OrganizationStatus.ACTIVE);
        org = orgs.save(org);

        branch = new Branch();
        branch.setArkId(ArkIdGenerator.generateBranchId());
        branch.setOrganizationId(org.getId());
        branch.setName("Main");
        branch.setStatus(Branch.BranchStatus.ACTIVE);
        branch = branches.save(branch);

        orgAdmin = new User();
        orgAdmin.setFullName("OA");
        orgAdmin.setEmail("oa@p2.test");
        orgAdmin.setPassword(encoder.encode("Password@123"));
        orgAdmin.setRole(User.Role.ORG_ADMIN);
        orgAdmin.setOrganizationId(org.getId());
        orgAdmin.setStatus(User.UserStatus.ACTIVE);
        users.save(orgAdmin);

        grade1 = clazz("Grade 1", "A", "2025-2026");
        s1 = student("One");
        s2 = student("Two");
        enroll(s1, grade1, "2025-2026");
        enroll(s2, grade1, "2025-2026");

        MvcResult res = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"oa@p2.test\",\"password\":\"Password@123\"}")).andReturn();
        token = JsonPath.read(res.getResponse().getContentAsString(), "$.accessToken");
    }

    // ---- bulk import ----

    @Test
    void badCsvRowNeverChangesOtherRows() throws Exception {
        String csv = "firstName,lastName,branchId,govtIdType,govtIdNumber,classId,academicYear\n"
                + "Good,One," + branch.getId() + ",PAN,P1," + grade1.getId() + ",2025-2026\n"
                + "Bad,Class," + branch.getId() + ",PAN,P2,no-such-class,2025-2026\n"
                + "Bad,Year," + branch.getId() + ",PAN,P3," + grade1.getId() + ",2025\n"
                + "Good,Two," + branch.getId() + ",PAN,P4," + grade1.getId() + ",2025-2026\n";
        long before = students.count();
        mvc.perform(multipart("/api/v1/organizations/" + org.getId() + "/bulk-import/students")
                        .file(new MockMultipartFile("file", "s.csv", "text/csv", csv.getBytes()))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.successCount").value(2))
                .andExpect(jsonPath("$.failureCount").value(2))
                .andExpect(jsonPath("$.errors[0].row").value(3))
                .andExpect(jsonPath("$.errors[1].row").value(4));
        assertThat(students.count() - before).isEqualTo(2);
        // every imported student has exactly its own enrollment
        for (Student st : students.findAll()) {
            if (st.getFirstName().equals("Good")) {
                assertThat(enrollments.findByOrganizationIdAndStudentIdOrderByAcademicYearDesc(org.getId(), st.getId()))
                        .hasSize(1);
            }
        }
    }

    // ---- promotion ----

    private void configureProgression() throws Exception {
        mvc.perform(put(base() + "/class-progression").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sequence\":[{\"className\":\"Grade 1\",\"displayOrder\":1,\"isTerminal\":false},"
                                + "{\"className\":\"Grade 2\",\"displayOrder\":2,\"isTerminal\":true}]}"))
                .andExpect(status().isOk());
    }

    @Test
    void promotionRunsOnceAndHeldBackStudentsRepeatTheClass() throws Exception {
        configureProgression();
        String body = "{\"sourceClassId\":\"" + grade1.getId() + "\",\"targetAcademicYear\":\"2026-2027\","
                + "\"studentOverrides\":[{\"studentId\":\"" + s2.getId() + "\",\"action\":\"HOLD_BACK\",\"reason\":\"failed\"}]}";
        mvc.perform(post(base() + "/promotions/execute").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.promoted").value(1))
                .andExpect(jsonPath("$.summary.heldBack").value(1));

        List<StudentEnrollment> s1e = enrollments.findByOrganizationIdAndStudentIdOrderByAcademicYearDesc(org.getId(), s1.getId());
        List<StudentEnrollment> s2e = enrollments.findByOrganizationIdAndStudentIdOrderByAcademicYearDesc(org.getId(), s2.getId());
        assertThat(s1e).hasSize(2);
        assertThat(s2e).hasSize(2);
        StudentEnrollment s2old = s2e.get(1);
        assertThat(s2old.getExitReason()).isEqualTo(StudentEnrollment.ExitReason.HELD_BACK);
        StudentEnrollment s2new = s2e.get(0);
        assertThat(s2new.getStatus()).isEqualTo(StudentEnrollment.EnrollmentStatus.ACTIVE);
        assertThat(classes.findById(s2new.getClassId()).orElseThrow().getName()).isEqualTo("Grade 1");
        assertThat(classes.findById(s2new.getClassId()).orElseThrow().getAcademicYear()).isEqualTo("2026-2027");
        assertThat(classes.findById(s1e.get(0).getClassId()).orElseThrow().getName()).isEqualTo("Grade 2");

        // second run must not duplicate anything
        mvc.perform(post(base() + "/promotions/execute").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
        assertThat(enrollments.count()).isEqualTo(4);
        // per-student changes and the auto-created classes are audited
        assertThat(audit.findAll()).anyMatch(l -> "AcademicClass".equals(l.getEntityType()) && l.getAction() == AuditLog.Action.CREATE);
        assertThat(audit.findAll().stream().filter(l -> "StudentEnrollment".equals(l.getEntityType())).count()).isGreaterThanOrEqualTo(2);
    }

    @Test
    void failingStudentWithNoOverrideIsHeldBack() throws Exception {
        configureProgression();
        Examination exam = new Examination();
        exam.setArkId(ArkIdGenerator.generateExamId());
        exam.setOrganizationId(org.getId());
        exam.setBranchId(branch.getId());
        exam.setName("Finals");
        exam.setAcademicYear("2025-2026");
        exam.setExamType(Examination.ExamType.FINAL);
        exam.setStatus(Examination.ExamStatus.COMPLETED);
        exam = exams.save(exam);

        ExamSubject math = new ExamSubject();
        math.setExaminationId(exam.getId());
        math.setOrganizationId(org.getId());
        math.setBranchId(branch.getId());
        math.setClassId(grade1.getId());
        math.setSubjectName("Mathematics");
        math.setMaxMarks(100.0);
        math.setPassingMarks(35.0);
        math = subjects.save(math);

        saveResult(exam, math, s1, 90.0, ExamResult.ResultStatus.PASS);
        saveResult(exam, math, s2, 20.0, ExamResult.ResultStatus.FAIL);

        mvc.perform(post(base() + "/promotions/execute").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sourceClassId\":\"" + grade1.getId() + "\",\"targetAcademicYear\":\"2026-2027\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.promoted").value(1))
                .andExpect(jsonPath("$.summary.heldBack").value(1));

        List<StudentEnrollment> s2e = enrollments.findByOrganizationIdAndStudentIdOrderByAcademicYearDesc(org.getId(), s2.getId());
        assertThat(s2e.get(1).getExitReason()).isEqualTo(StudentEnrollment.ExitReason.HELD_BACK);
        assertThat(classes.findById(s2e.get(0).getClassId()).orElseThrow().getName()).isEqualTo("Grade 1");
    }

    private void saveResult(Examination exam, ExamSubject subject, Student st, double marks, ExamResult.ResultStatus status) {
        ExamResult r = new ExamResult();
        r.setExaminationId(exam.getId());
        r.setExamSubjectId(subject.getId());
        r.setOrganizationId(org.getId());
        r.setBranchId(branch.getId());
        r.setClassId(grade1.getId());
        r.setStudentId(st.getId());
        r.setMarksObtained(marks);
        r.setStatus(status);
        results.save(r);
    }

    @Test
    void facultyImportRejectsBadEmailAndDuplicateEmployeeId() throws Exception {
        String csv = "firstName,lastName,email,branchId,govtIdType,govtIdNumber,employeeId\n"
                + "Good,One,good@x.test," + branch.getId() + ",PAN,F1,E1\n"
                + "Bad,Email,not-an-email," + branch.getId() + ",PAN,F2,E2\n"
                + "Dup,Emp,dup@x.test," + branch.getId() + ",PAN,F3,E1\n";
        mvc.perform(multipart("/api/v1/organizations/" + org.getId() + "/bulk-import/faculty")
                        .file(new MockMultipartFile("file", "f.csv", "text/csv", csv.getBytes()))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.successCount").value(1))
                .andExpect(jsonPath("$.failureCount").value(2))
                .andExpect(jsonPath("$.errors[0].row").value(3))
                .andExpect(jsonPath("$.errors[1].row").value(4));
    }

    @Test
    void promotionInputIsValidated() throws Exception {
        configureProgression();
        String url = base() + "/promotions/execute";
        // bad year format
        mvc.perform(post(url).header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                .content("{\"sourceClassId\":\"" + grade1.getId() + "\",\"targetAcademicYear\":\"2027\"}"))
                .andExpect(status().isUnprocessableEntity());
        // not later than the source year
        mvc.perform(post(url).header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                .content("{\"sourceClassId\":\"" + grade1.getId() + "\",\"targetAcademicYear\":\"2025-2026\"}"))
                .andExpect(status().isBadRequest());
        // unknown action
        mvc.perform(post(url).header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                .content("{\"sourceClassId\":\"" + grade1.getId() + "\",\"targetAcademicYear\":\"2026-2027\","
                        + "\"studentOverrides\":[{\"studentId\":\"" + s1.getId() + "\",\"action\":\"FLY\"}]}"))
                .andExpect(status().isBadRequest());
        // GRADUATE in a non-terminal class
        mvc.perform(post(url).header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                .content("{\"sourceClassId\":\"" + grade1.getId() + "\",\"targetAcademicYear\":\"2026-2027\","
                        + "\"studentOverrides\":[{\"studentId\":\"" + s1.getId() + "\",\"action\":\"GRADUATE\"}]}"))
                .andExpect(status().isBadRequest());
        // PROMOTE in the terminal class
        grade2 = clazz("Grade 2", "A", "2025-2026");
        Student s3 = student("Three");
        enroll(s3, grade2, "2025-2026");
        mvc.perform(post(url).header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                .content("{\"sourceClassId\":\"" + grade2.getId() + "\",\"targetAcademicYear\":\"2026-2027\","
                        + "\"studentOverrides\":[{\"studentId\":\"" + s3.getId() + "\",\"action\":\"PROMOTE\"}]}"))
                .andExpect(status().isBadRequest());
        assertThat(enrollments.count()).isEqualTo(3); // nothing changed
    }

    @Test
    void progressionNeedsUniqueOrdersAndExactlyOneTerminal() throws Exception {
        mvc.perform(put(base() + "/class-progression").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sequence\":[{\"className\":\"A\",\"displayOrder\":1,\"isTerminal\":true},"
                                + "{\"className\":\"B\",\"displayOrder\":2,\"isTerminal\":true}]}"))
                .andExpect(status().isBadRequest());
        mvc.perform(put(base() + "/class-progression").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sequence\":[{\"className\":\"A\",\"displayOrder\":1,\"isTerminal\":false},"
                                + "{\"className\":\"B\",\"displayOrder\":1,\"isTerminal\":true}]}"))
                .andExpect(status().isBadRequest());
        configureProgression();
        assertThat(audit.findAll()).anyMatch(l -> "ClassProgression".equals(l.getEntityType()) && l.getAction() == AuditLog.Action.CREATE);
        mvc.perform(delete(base() + "/class-progression").header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }

    // ---- exams ----

    @Test
    void marksAreValidatedAgainstTheExamYearEnrollmentAndRecomputedOnThresholdChange() throws Exception {
        // s1's 2025-2026 enrollment is closed (promoted) and a 2026-2027 one is active elsewhere
        StudentEnrollment old = enrollments.findByOrganizationIdAndStudentIdAndAcademicYear(org.getId(), s1.getId(), "2025-2026").orElseThrow();
        old.setStatus(StudentEnrollment.EnrollmentStatus.COMPLETED);
        enrollments.save(old);
        AcademicClass other = clazz("Grade 2", "A", "2026-2027");
        enroll(s1, other, "2026-2027");

        Examination exam = new Examination();
        exam.setArkId(ArkIdGenerator.generateExamId());
        exam.setOrganizationId(org.getId());
        exam.setBranchId(branch.getId());
        exam.setName("Final");
        exam.setAcademicYear("2025-2026");
        exam.setStatus(Examination.ExamStatus.SCHEDULED);
        exam = exams.save(exam);
        ExamSubject subj = new ExamSubject();
        subj.setExaminationId(exam.getId());
        subj.setOrganizationId(org.getId());
        subj.setBranchId(branch.getId());
        subj.setClassId(grade1.getId());
        subj.setSubjectName("Math");
        subj.setMaxMarks(100.0);
        subj.setPassingMarks(40.0);
        subj = subjects.save(subj);

        String resultsUrl = base().replace("/branches/" + branch.getId(), "") + "/examinations/" + exam.getId()
                + "/subjects/" + subj.getId() + "/results";
        // entered against the exam's year, not the student's current active enrollment
        mvc.perform(post(resultsUrl).header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentId\":\"" + s1.getId() + "\",\"marksObtained\":45}"))
                .andExpect(status().isCreated());
        // a student with no enrollment for that year is rejected
        Student stranger = student("Stranger");
        mvc.perform(post(resultsUrl).header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentId\":\"" + stranger.getId() + "\",\"marksObtained\":45}"))
                .andExpect(status().isBadRequest());

        // raising the passing mark flips the stored PASS to FAIL
        mvc.perform(put(base().replace("/branches/" + branch.getId(), "") + "/examinations/" + exam.getId()
                        + "/subjects/" + subj.getId()).header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"passingMarks\":60}"))
                .andExpect(status().isOk());
        assertThat(results.findAll()).hasSize(1).allMatch(r -> r.getStatus() == ExamResult.ResultStatus.FAIL);
    }

    // ---- classes, enrollments, students ----

    @Test
    void classWithHistoryCannotBeDeleted() throws Exception {
        for (StudentEnrollment e : enrollments.findAll()) {
            e.setStatus(StudentEnrollment.EnrollmentStatus.COMPLETED);
            enrollments.save(e);
        }
        mvc.perform(delete(base() + "/classes/" + grade1.getId()).header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict());
        assertThat(classes.findById(grade1.getId())).isPresent();
    }

    @Test
    void closingForGoodUpdatesTheStudentAndReopenUndoesIt() throws Exception {
        StudentEnrollment e = enrollments.findByOrganizationIdAndStudentIdAndAcademicYear(org.getId(), s1.getId(), "2025-2026").orElseThrow();
        String url = base() + "/enrollments/" + e.getId();
        mvc.perform(put(url + "/close").header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                .content("{\"exitReason\":\"GRADUATED\"}")).andExpect(status().isOk());
        assertThat(students.findById(s1.getId()).orElseThrow().getStatus()).isEqualTo(Student.StudentStatus.GRADUATED);
        mvc.perform(put(url + "/reopen").header("Authorization", "Bearer " + token)).andExpect(status().isOk());
        assertThat(students.findById(s1.getId()).orElseThrow().getStatus()).isEqualTo(Student.StudentStatus.ACTIVE);
        assertThat(enrollments.findById(e.getId()).orElseThrow().getStatus()).isEqualTo(StudentEnrollment.EnrollmentStatus.ACTIVE);
    }

    @Test
    void studentWithActiveEnrollmentCannotChangeBranch() throws Exception {
        Branch other = new Branch();
        other.setArkId(ArkIdGenerator.generateBranchId());
        other.setOrganizationId(org.getId());
        other.setName("Other");
        other.setStatus(Branch.BranchStatus.ACTIVE);
        other = branches.save(other);
        mvc.perform(put("/api/v1/organizations/" + org.getId() + "/students/" + s1.getId())
                        .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"branchId\":\"" + other.getId() + "\"}"))
                .andExpect(status().isConflict());
        mvc.perform(put("/api/v1/organizations/" + org.getId() + "/students/" + s1.getId())
                        .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"branchId\":\"nope\"}"))
                .andExpect(status().isNotFound());
    }

    // ---- audit ----

    @Test
    void updatesRecordBeforeAfterAndFiltersCombine() throws Exception {
        mvc.perform(put("/api/v1/organizations/" + org.getId() + "/students/" + s1.getId())
                .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                .content("{\"firstName\":\"Renamed\"}")).andExpect(status().isOk());
        String url = "/api/v1/organizations/" + org.getId() + "/audit-logs";
        mvc.perform(get(url + "?action=UPDATE&entityType=Student&userId=" + orgAdmin.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].changedFields[0]").value("firstName"))
                .andExpect(jsonPath("$.content[0].before.firstName").value("One"))
                .andExpect(jsonPath("$.content[0].after.firstName").value("Renamed"));
        // govt id never leaks into the audit snapshot
        assertThat(audit.findAll().stream().filter(l -> l.getAfter() != null))
                .allMatch(l -> !l.getAfter().containsKey("govtIdNumber") && !l.getAfter().containsKey("password"));
        mvc.perform(get(url + "?action=UPDATE&entityType=Branch").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.content.length()").value(0));
    }

    // ---- helpers ----

    private String base() {
        return "/api/v1/organizations/" + org.getId() + "/branches/" + branch.getId();
    }

    private AcademicClass clazz(String name, String section, String year) {
        AcademicClass c = new AcademicClass();
        c.setOrganizationId(org.getId());
        c.setBranchId(branch.getId());
        c.setName(name);
        c.setSection(section);
        c.setAcademicYear(year);
        c.setCapacity(30);
        c.setStatus(AcademicClass.ClassStatus.ACTIVE);
        return classes.save(c);
    }

    private Student student(String first) {
        Student s = new Student();
        s.setArkId(ArkIdGenerator.generateStudentId());
        s.setOrganizationId(org.getId());
        s.setBranchId(branch.getId());
        s.setFirstName(first);
        s.setLastName("Test");
        s.setGovtIdNumber("SECRET-" + first);
        s.setStatus(Student.StudentStatus.ACTIVE);
        return students.save(s);
    }

    private StudentEnrollment enroll(Student s, AcademicClass c, String year) {
        StudentEnrollment e = new StudentEnrollment();
        e.setArkId(ArkIdGenerator.generateEnrollmentId());
        e.setOrganizationId(org.getId());
        e.setBranchId(branch.getId());
        e.setStudentId(s.getId());
        e.setClassId(c.getId());
        e.setAcademicYear(year);
        e.setEnrolledAt(LocalDate.now());
        e.setStatus(StudentEnrollment.EnrollmentStatus.ACTIVE);
        return enrollments.save(e);
    }
}
