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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Phase 0/1 acceptance tests: auth contract, tenant and branch isolation, role rules, path-id fixes. */
@AutoConfigureMockMvc
class SecurityAcceptanceTest extends AbstractMongoIntegrationTest {

    static final String PASSWORD = "Password@123";

    @Autowired MockMvc mvc;
    @Autowired PasswordEncoder encoder;
    @Autowired UserRepository users;
    @Autowired OrganizationRepository orgs;
    @Autowired BranchRepository branches;
    @Autowired StudentRepository students;
    @Autowired ExaminationRepository exams;
    @Autowired ExamSubjectRepository subjects;
    @Autowired ExamResultRepository results;
    @Autowired RefreshSessionRepository sessions;
    @Autowired AuditLogRepository audit;

    Organization orgA, orgB;
    Branch a1, a2, b1;
    Student sA1, sA2, sB1;
    Examination e1, e2;
    ExamSubject subj1, subj2;
    ExamResult r1, r2;
    User superAdmin, orgAdminA, adminA1, userA1;

    @BeforeEach
    void seed() {
        results.deleteAll();
        subjects.deleteAll();
        exams.deleteAll();
        students.deleteAll();
        users.deleteAll();
        branches.deleteAll();
        orgs.deleteAll();
        sessions.deleteAll();
        audit.deleteAll();

        orgA = org("Org A");
        orgB = org("Org B");
        a1 = branch(orgA, "A1");
        a2 = branch(orgA, "A2");
        b1 = branch(orgB, "B1");

        superAdmin = user("super@ark.test", User.Role.SUPER_ADMIN, null, null);
        orgAdminA = user("orgadmin@a.test", User.Role.ORG_ADMIN, orgA, null);
        adminA1 = user("admin@a1.test", User.Role.ADMIN, orgA, a1);
        userA1 = user("user@a1.test", User.Role.USER, orgA, a1);

        sA1 = student(orgA, a1, "Ann", "ABCD123456");
        sA2 = student(orgA, a2, "Amy", "ZZZZ999999");
        sB1 = student(orgB, b1, "Bob", "BBBB000000");

        e1 = exam(orgA, a1, "Midterm");
        e2 = exam(orgA, a1, "Final");
        subj1 = subject(e1, "Math");
        subj2 = subject(e1, "Science");
        r1 = result(e1, subj1, sA1, 40);
        r2 = result(e1, subj2, sA1, 30);
    }

    // ---------------- auth contract ----------------

    @Test
    void registerEndpointIsGone() throws Exception {
        int status = mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"x@y.z\",\"password\":\"Password@123\",\"role\":\"ADMIN\"}"))
                .andReturn().getResponse().getStatus();
        assertThat(status).isIn(401, 404, 405);
        assertThat(users.findByEmail("x@y.z")).isEmpty();
    }

    @Test
    void wrongPasswordAndUnknownEmailGiveSame401() throws Exception {
        mvc.perform(login("admin@a1.test", "nope"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.message").value("Invalid email or password"))
                .andExpect(jsonPath("$.path").value("/api/v1/auth/login"))
                .andExpect(jsonPath("$.timestamp").exists());
        mvc.perform(login("ghost@nowhere.test", "nope"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
        // unknown emails are audited too
        assertThat(audit.findAll()).anyMatch(l -> l.getAction() == AuditLog.Action.LOGIN_FAILED
                && "ghost@nowhere.test".equals(l.getEntityName()));
    }

    @Test
    void repeatedFailuresLockTheAccount() throws Exception {
        for (int i = 0; i < 5; i++) {
            mvc.perform(login("user@a1.test", "wrong")).andExpect(status().isUnauthorized());
        }
        mvc.perform(login("user@a1.test", PASSWORD))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("ACCOUNT_LOCKED"));
    }

    @Test
    void loginReturnsContractShapeAndMeReturnsScope() throws Exception {
        MvcResult res = mvc.perform(login("admin@a1.test", PASSWORD))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andExpect(jsonPath("$.expiresIn").value(1800))
                .andExpect(jsonPath("$.user.role").value("ADMIN"))
                .andExpect(jsonPath("$.user.branchId").value(a1.getId()))
                .andReturn();
        String access = JsonPath.read(res.getResponse().getContentAsString(), "$.accessToken");
        mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + access))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scope.role").value("ADMIN"))
                .andExpect(jsonPath("$.scope.organizationId").value(orgA.getId()))
                .andExpect(jsonPath("$.scope.branchIds[0]").value(a1.getId()))
                .andExpect(jsonPath("$.scope.allBranches").value(false));
    }

    @Test
    void suspendedOrgCannotLoginAndExistingTokensStopWorking() throws Exception {
        String token = accessToken("orgadmin@a.test");
        orgA.setStatus(Organization.OrganizationStatus.SUSPENDED);
        orgs.save(orgA);

        mvc.perform(login("orgadmin@a.test", PASSWORD))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("ORG_SUSPENDED"));
        mvc.perform(get("/api/v1/organizations/" + orgA.getId()).header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("ORG_SUSPENDED"));
    }

    @Test
    void refreshRotatesAndReuseRevokesTheSession() throws Exception {
        MvcResult login = mvc.perform(login("admin@a1.test", PASSWORD)).andExpect(status().isOk()).andReturn();
        String refresh1 = JsonPath.read(login.getResponse().getContentAsString(), "$.refreshToken");

        MvcResult rotated = mvc.perform(refresh(refresh1)).andExpect(status().isOk()).andReturn();
        String refresh2 = JsonPath.read(rotated.getResponse().getContentAsString(), "$.refreshToken");
        assertThat(refresh2).isNotEqualTo(refresh1);

        // reusing the already-rotated token is an attack signal: 401 and the whole device session dies
        mvc.perform(refresh(refresh1)).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("TOKEN_REVOKED"));
        mvc.perform(refresh(refresh2)).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("TOKEN_REVOKED"));
    }

    @Test
    void logoutMakesRefreshTokenFail() throws Exception {
        MvcResult login = mvc.perform(login("admin@a1.test", PASSWORD)).andReturn();
        String refresh = JsonPath.read(login.getResponse().getContentAsString(), "$.refreshToken");
        mvc.perform(post("/api/v1/auth/logout").contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"" + refresh + "\"}")).andExpect(status().isNoContent());
        mvc.perform(refresh(refresh)).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("TOKEN_REVOKED"));
    }

    @Test
    void changePasswordInvalidatesOldTokensAndSessions() throws Exception {
        MvcResult login = mvc.perform(login("admin@a1.test", PASSWORD)).andReturn();
        String access = JsonPath.read(login.getResponse().getContentAsString(), "$.accessToken");
        String refresh = JsonPath.read(login.getResponse().getContentAsString(), "$.refreshToken");

        MvcResult changed = mvc.perform(post("/api/v1/auth/change-password")
                        .header("Authorization", "Bearer " + access).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"" + PASSWORD + "\",\"newPassword\":\"NewPassword@456\"}"))
                .andExpect(status().isOk()).andReturn();
        String newAccess = JsonPath.read(changed.getResponse().getContentAsString(), "$.accessToken");

        mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + access))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.error").value("TOKEN_REVOKED"));
        mvc.perform(refresh(refresh)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + newAccess)).andExpect(status().isOk());
        mvc.perform(login("admin@a1.test", "NewPassword@456")).andExpect(status().isOk());
        mvc.perform(login("admin@a1.test", PASSWORD)).andExpect(status().isUnauthorized());
    }

    // ---------------- tenant and branch isolation ----------------

    @Test
    void adminOfBranchACannotReadOrWriteBranchB() throws Exception {
        String t = accessToken("admin@a1.test");
        String base = "/api/v1/organizations/" + orgA.getId() + "/students/";
        mvc.perform(get(base + sA1.getId()).header("Authorization", "Bearer " + t)).andExpect(status().isOk());
        mvc.perform(get(base + sA2.getId()).header("Authorization", "Bearer " + t))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.error").value("FORBIDDEN"));
        mvc.perform(put(base + sA2.getId()).header("Authorization", "Bearer " + t)
                .contentType(MediaType.APPLICATION_JSON).content("{\"firstName\":\"Hacked\"}"))
                .andExpect(status().isForbidden());
        assertThat(students.findById(sA2.getId()).orElseThrow().getFirstName()).isEqualTo("Amy");
        // list is silently restricted to the caller's branch
        mvc.perform(get("/api/v1/organizations/" + orgA.getId() + "/students").header("Authorization", "Bearer " + t))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));
        // asking for another branch explicitly is denied
        mvc.perform(get("/api/v1/organizations/" + orgA.getId() + "/students?branchId=" + a2.getId())
                .header("Authorization", "Bearer " + t)).andExpect(status().isForbidden());
    }

    @Test
    void userCannotWriteAndSeesMaskedGovtId() throws Exception {
        String t = accessToken("user@a1.test");
        String base = "/api/v1/organizations/" + orgA.getId() + "/students/";
        mvc.perform(put(base + sA1.getId()).header("Authorization", "Bearer " + t)
                .contentType(MediaType.APPLICATION_JSON).content("{\"firstName\":\"X\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(get(base + sA1.getId()).header("Authorization", "Bearer " + t))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.govtIdNumber").value("******3456"));
        // privileged roles still see the full value (and storage is encrypted)
        String admin = accessToken("admin@a1.test");
        mvc.perform(get(base + sA1.getId()).header("Authorization", "Bearer " + admin))
                .andExpect(jsonPath("$.govtIdNumber").value("ABCD123456"));
    }

    @Test
    void govtIdIsEncryptedAtRest() {
        String raw = org.bson.Document.parse(
                        mongoJson("students", sA1.getId())).getString("govtIdNumber");
        assertThat(raw).startsWith("enc:v1:").doesNotContain("ABCD123456");
    }

    @Test
    void idFromAnotherOrgIs404UnderMyOrgPath() throws Exception {
        String t = accessToken("orgadmin@a.test");
        mvc.perform(get("/api/v1/organizations/" + orgA.getId() + "/students/" + sB1.getId())
                .header("Authorization", "Bearer " + t)).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
        mvc.perform(get("/api/v1/organizations/" + orgB.getId() + "/students/" + sB1.getId())
                .header("Authorization", "Bearer " + t)).andExpect(status().isForbidden());
    }

    @Test
    void orgAdminOrgSelfServiceRules() throws Exception {
        String t = accessToken("orgadmin@a.test");
        mvc.perform(put("/api/v1/organizations/" + orgA.getId()).header("Authorization", "Bearer " + t)
                .contentType(MediaType.APPLICATION_JSON).content("{\"address\":\"1 New Street\"}"))
                .andExpect(status().isOk());
        mvc.perform(put("/api/v1/organizations/" + orgA.getId()).header("Authorization", "Bearer " + t)
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"INACTIVE\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/v1/organizations/" + orgB.getId()).header("Authorization", "Bearer " + t)
                .contentType(MediaType.APPLICATION_JSON).content("{\"address\":\"x\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/organizations/" + orgA.getId() + "/audit-logs").header("Authorization", "Bearer " + t))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/organizations/" + orgB.getId() + "/audit-logs").header("Authorization", "Bearer " + t))
                .andExpect(status().isForbidden());
    }

    // ---------------- user management rules ----------------

    @Test
    void userManagementRules() throws Exception {
        String admin = accessToken("admin@a1.test");
        String usersUrl = "/api/v1/organizations/" + orgA.getId() + "/users";
        // ADMIN cannot create a user in another branch
        mvc.perform(post(usersUrl).header("Authorization", "Bearer " + admin).contentType(MediaType.APPLICATION_JSON)
                .content("{\"fullName\":\"N\",\"email\":\"n@a2.test\",\"password\":\"Password@123\",\"role\":\"USER\",\"branchId\":\""
                        + a2.getId() + "\"}")).andExpect(status().isForbidden());
        // ...but can in their own
        mvc.perform(post(usersUrl).header("Authorization", "Bearer " + admin).contentType(MediaType.APPLICATION_JSON)
                .content("{\"fullName\":\"N\",\"email\":\"n@a1.test\",\"password\":\"Password@123\",\"role\":\"USER\",\"branchId\":\""
                        + a1.getId() + "\"}")).andExpect(status().isCreated());
        // ADMIN cannot see the ORG_ADMIN
        mvc.perform(get(usersUrl + "/" + orgAdminA.getId()).header("Authorization", "Bearer " + admin))
                .andExpect(status().isForbidden());

        String orgAdmin = accessToken("orgadmin@a.test");
        // cannot delete yourself
        mvc.perform(delete(usersUrl + "/" + orgAdminA.getId()).header("Authorization", "Bearer " + orgAdmin))
                .andExpect(status().isForbidden());
        // nobody (not even SUPER_ADMIN) may remove the last ORG_ADMIN
        String sup = accessToken("super@ark.test");
        mvc.perform(delete(usersUrl + "/" + orgAdminA.getId()).header("Authorization", "Bearer " + sup))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("CONFLICT"));
    }

    // ---------------- path-id bugs ----------------

    @Test
    void resultsBySubjectAreFilteredBySubject() throws Exception {
        String t = accessToken("orgadmin@a.test");
        mvc.perform(get("/api/v1/organizations/" + orgA.getId() + "/examinations/" + e1.getId()
                        + "/subjects/" + subj1.getId() + "/results").header("Authorization", "Bearer " + t))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(r1.getId()));
    }

    @Test
    void updatingResultThroughAnotherExamPathIs404() throws Exception {
        String t = accessToken("orgadmin@a.test");
        String base = "/api/v1/organizations/" + orgA.getId() + "/examinations/";
        mvc.perform(put(base + e2.getId() + "/results/" + r1.getId()).header("Authorization", "Bearer " + t)
                .contentType(MediaType.APPLICATION_JSON).content("{\"marksObtained\":50}"))
                .andExpect(status().isNotFound());
        assertThat(results.findById(r1.getId()).orElseThrow().getMarksObtained()).isEqualTo(40.0);
        mvc.perform(put(base + e1.getId() + "/results/" + r1.getId()).header("Authorization", "Bearer " + t)
                .contentType(MediaType.APPLICATION_JSON).content("{\"marksObtained\":45}"))
                .andExpect(status().isOk());
    }

    @Test
    void malformedJsonReturnsStableBadRequest() throws Exception {
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"));
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }

    // ---------------- helpers ----------------

    private org.springframework.test.web.servlet.RequestBuilder login(String email, String password) {
        return post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}");
    }

    private org.springframework.test.web.servlet.RequestBuilder refresh(String token) {
        return post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"" + token + "\"}");
    }

    private String accessToken(String email) throws Exception {
        MvcResult res = mvc.perform(login(email, PASSWORD)).andExpect(status().isOk()).andReturn();
        return JsonPath.read(res.getResponse().getContentAsString(), "$.accessToken");
    }

    @Autowired org.springframework.data.mongodb.core.MongoTemplate mongo;

    private String mongoJson(String collection, String id) {
        org.bson.Document doc = mongo.getCollection(collection)
                .find(new org.bson.Document("_id", new org.bson.types.ObjectId(id))).first();
        return doc.toJson();
    }

    private Organization org(String name) {
        Organization o = new Organization();
        o.setArkId(ArkIdGenerator.generateOrgId());
        o.setName(name);
        o.setStatus(Organization.OrganizationStatus.ACTIVE);
        return orgs.save(o);
    }

    private Branch branch(Organization o, String name) {
        Branch b = new Branch();
        b.setArkId(ArkIdGenerator.generateBranchId());
        b.setOrganizationId(o.getId());
        b.setName(name);
        b.setStatus(Branch.BranchStatus.ACTIVE);
        return branches.save(b);
    }

    private User user(String email, User.Role role, Organization o, Branch b) {
        User u = new User();
        u.setFullName(email);
        u.setEmail(email);
        u.setPassword(encoder.encode(PASSWORD));
        u.setRole(role);
        u.setOrganizationId(o == null ? null : o.getId());
        u.setBranchId(b == null ? null : b.getId());
        u.setStatus(User.UserStatus.ACTIVE);
        return users.save(u);
    }

    private Student student(Organization o, Branch b, String first, String govtId) {
        Student s = new Student();
        s.setArkId(ArkIdGenerator.generateStudentId());
        s.setOrganizationId(o.getId());
        s.setBranchId(b.getId());
        s.setFirstName(first);
        s.setLastName("Test");
        s.setGovtIdNumber(govtId);
        s.setStatus(Student.StudentStatus.ACTIVE);
        return students.save(s);
    }

    private Examination exam(Organization o, Branch b, String name) {
        Examination e = new Examination();
        e.setArkId(ArkIdGenerator.generateExamId());
        e.setOrganizationId(o.getId());
        e.setBranchId(b.getId());
        e.setName(name);
        e.setAcademicYear("2025-2026");
        e.setStatus(Examination.ExamStatus.SCHEDULED);
        return exams.save(e);
    }

    private ExamSubject subject(Examination e, String name) {
        ExamSubject s = new ExamSubject();
        s.setExaminationId(e.getId());
        s.setOrganizationId(e.getOrganizationId());
        s.setBranchId(e.getBranchId());
        s.setClassId("class-1");
        s.setSubjectName(name);
        s.setMaxMarks(50.0);
        s.setPassingMarks(20.0);
        return subjects.save(s);
    }

    private ExamResult result(Examination e, ExamSubject s, Student st, double marks) {
        ExamResult r = new ExamResult();
        r.setExaminationId(e.getId());
        r.setExamSubjectId(s.getId());
        r.setOrganizationId(e.getOrganizationId());
        r.setBranchId(e.getBranchId());
        r.setClassId("class-1");
        r.setStudentId(st.getId());
        r.setMarksObtained(marks);
        r.setGrade("A");
        r.setStatus(ExamResult.ResultStatus.PASS);
        return results.save(r);
    }
}
