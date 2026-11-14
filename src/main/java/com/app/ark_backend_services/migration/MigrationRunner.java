package com.app.ark_backend_services.migration;

import com.app.ark_backend_services.security.FieldEncryption;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * One-off data migrations. They never run implicitly. Start the application with:
 * <pre>
 *   --migrate.dry-run   print what each pending migration would do, change nothing
 *   --migrate           apply pending migrations and record them in the "migrations" collection (each runs once)
 * </pre>
 * The process exits when the run is finished. Always rehearse on a copy of the database first.
 */
@Slf4j
@Component
public class MigrationRunner implements ApplicationRunner {

    static final String COLLECTION = "migrations";

    private final MongoTemplate mongo;
    private final ConfigurableApplicationContext context;

    public MigrationRunner(MongoTemplate mongo, ConfigurableApplicationContext context) {
        this.mongo = mongo;
        this.context = context;
    }

    /** What one migration did (or would do). A failed migration is never recorded as applied. */
    public record Result(boolean ok, List<String> lines) {}

    public interface Migration {
        String id();
        String description();
        Result run(MongoTemplate mongo, boolean dryRun);
    }

    private static List<Migration> migrations() {
        return List.of(new EncryptGovtIds(), new CheckUserEmailsUnique(), new DropStudentClassId(),
                new BackfillCatalogIds(), new NormalizeGender(), new FixCopiedBranchIds());
    }

    @Override
    public void run(ApplicationArguments args) {
        boolean dry = args.containsOption("migrate.dry-run");
        if (!dry && !args.containsOption("migrate")) {
            return;
        }
        List<String> report = execute(dry);
        report.forEach(l -> log.info("[migration] {}", l));
        System.exit(SpringApplication.exit(context, () -> 0));
    }

    /** Runs every pending migration; returns the report. Public so tests can call it directly. */
    public List<String> execute(boolean dryRun) {
        List<String> report = new ArrayList<>();
        report.add(dryRun ? "DRY RUN: nothing will be changed" : "Applying pending migrations");
        for (Migration m : migrations()) {
            if (mongo.exists(Query.query(Criteria.where("_id").is(m.id())), COLLECTION)) {
                report.add(m.id() + ": already applied, skipped");
                continue;
            }
            report.add(m.id() + ": " + m.description());
            Result result;
            try {
                result = m.run(mongo, dryRun);
            } catch (Exception e) {
                result = new Result(false, List.of("failed with " + e));
            }
            result.lines().forEach(l -> report.add("    " + l));
            if (!result.ok()) {
                report.add(m.id() + ": FAILED, not recorded; fix the problem and run again");
                break; // later migrations may depend on this one
            }
            if (!dryRun) {
                mongo.save(new Document("_id", m.id())
                        .append("description", m.description())
                        .append("appliedAt", Instant.now().toString())
                        .append("report", result.lines()), COLLECTION);
                report.add(m.id() + ": applied");
            }
        }
        return report;
    }

    // ---------------------------------------------------------------------------------------------

    /** Encrypts government ID numbers written before field encryption existed. */
    static class EncryptGovtIds implements Migration {
        public String id() { return "001-encrypt-govt-ids"; }
        public String description() { return "Encrypt plaintext govtIdNumber in students and faculty"; }

        public Result run(MongoTemplate mongo, boolean dryRun) {
            List<String> lines = new ArrayList<>();
            for (String collection : List.of("students", "faculty")) {
                long converted = 0;
                Query plain = Query.query(Criteria.where("govtIdNumber").exists(true).ne(null)
                        .not().regex("^enc:v1:"));
                for (Document doc : mongo.find(plain, Document.class, collection)) {
                    Object value = doc.get("govtIdNumber");
                    if (!(value instanceof String s) || s.isEmpty()) continue;
                    if (!dryRun) {
                        mongo.updateFirst(Query.query(Criteria.where("_id").is(doc.get("_id"))),
                                new Update().set("govtIdNumber", FieldEncryption.encrypt(s)), collection);
                    }
                    converted++;
                }
                lines.add(collection + ": " + converted + " value(s) " + (dryRun ? "would be encrypted" : "encrypted"));
            }
            return new Result(true, lines);
        }
    }

    /** The users.email unique index needs unique emails; report any duplicates instead of guessing which to keep. */
    static class CheckUserEmailsUnique implements Migration {
        public String id() { return "002-check-user-email-duplicates"; }
        public String description() { return "Verify user emails are unique (required by the unique email index)"; }

        public Result run(MongoTemplate mongo, boolean dryRun) {
            List<Document> dupes = mongo.getCollection("users").aggregate(List.of(
                    new Document("$group", new Document("_id", new Document("$toLower", "$email"))
                            .append("count", new Document("$sum", 1))),
                    new Document("$match", new Document("count", new Document("$gt", 1)))
            )).into(new ArrayList<>());
            if (dupes.isEmpty()) {
                return new Result(true, List.of("no duplicate emails"));
            }
            List<String> lines = new ArrayList<>();
            dupes.forEach(d -> lines.add("duplicate email (case-insensitive): " + d.get("_id") + " x" + d.get("count")));
            lines.add("resolve these accounts manually (rename or delete), then run the migration again");
            return new Result(false, lines);
        }
    }

    /** Student.classId was replaced by StudentEnrollment; remove the leftover field from old documents. */
    static class DropStudentClassId implements Migration {
        public String id() { return "003-drop-student-classid"; }
        public String description() { return "Remove the legacy classId field from students"; }

        public Result run(MongoTemplate mongo, boolean dryRun) {
            Query q = Query.query(Criteria.where("classId").exists(true));
            long n = mongo.count(q, "students");
            if (!dryRun && n > 0) {
                mongo.updateMulti(q, new Update().unset("classId"), "students");
            }
            return new Result(true, List.of(n + " student document(s) " + (dryRun ? "would be cleaned" : "cleaned")));
        }
    }

    /** Links existing records to the academic year / subject catalog entries with the same label or name. */
    static class BackfillCatalogIds implements Migration {
        public String id() { return "004-backfill-catalog-ids"; }
        public String description() { return "Set academicYearId / subjectId where the catalog has a matching label or name"; }

        public Result run(MongoTemplate mongo, boolean dryRun) {
            List<String> lines = new ArrayList<>();
            long years = 0, subjects = 0;
            for (Document y : mongo.getCollection("academic_years").find()) {
                String org = y.getString("organizationId"), label = y.getString("label");
                String yid = y.getObjectId("_id").toHexString();
                for (String c : List.of("academic_classes", "student_enrollments", "examinations", "faculty_assignments")) {
                    Query q = Query.query(Criteria.where("organizationId").is(org).and("academicYear").is(label)
                            .and("academicYearId").exists(false));
                    years += mongo.count(q, c);
                    if (!dryRun) mongo.updateMulti(q, new Update().set("academicYearId", yid), c);
                }
            }
            for (Document sub : mongo.getCollection("subjects").find()) {
                String org = sub.getString("organizationId"), name = sub.getString("name");
                String sid = sub.getObjectId("_id").toHexString();
                for (String c : List.of("exam_subjects", "faculty_assignments")) {
                    Query q = Query.query(Criteria.where("organizationId").is(org).and("subjectName").is(name)
                            .and("subjectId").exists(false));
                    subjects += mongo.count(q, c);
                    if (!dryRun) mongo.updateMulti(q, new Update().set("subjectId", sid), c);
                }
            }
            lines.add(years + " record(s) " + (dryRun ? "would be linked" : "linked") + " to an academic year");
            lines.add(subjects + " record(s) " + (dryRun ? "would be linked" : "linked") + " to a subject");
            return new Result(true, lines);
        }
    }

    /** Gender becomes MALE / FEMALE / OTHER; values that cannot be mapped are reported and left alone. */
    static class NormalizeGender implements Migration {
        public String id() { return "005-normalize-gender"; }
        public String description() { return "Upper-case gender values in students and faculty"; }

        public Result run(MongoTemplate mongo, boolean dryRun) {
            List<String> lines = new ArrayList<>();
            for (String c : List.of("students", "faculty")) {
                long changed = 0, unknown = 0;
                for (Document d : mongo.find(Query.query(Criteria.where("gender").exists(true).ne(null)), Document.class, c)) {
                    String g = d.getString("gender");
                    String normalized = com.app.ark_backend_services.util.Genders.normalizeOrNull(g);
                    if (normalized == null) {
                        if (g != null && !g.isBlank()) {
                            unknown++;
                            lines.add(c + " " + d.get("_id") + ": cannot map gender '" + g + "', left as is");
                        }
                    } else if (!normalized.equals(g)) {
                        changed++;
                        if (!dryRun) {
                            mongo.updateFirst(Query.query(Criteria.where("_id").is(d.get("_id"))),
                                    new Update().set("gender", normalized), c);
                        }
                    }
                }
                lines.add(c + ": " + changed + " value(s) " + (dryRun ? "would be normalized" : "normalized") + ", " + unknown + " unmappable");
            }
            return new Result(true, lines);
        }
    }

    /** ExamResult and FacultyAssignment keep a copy of the class's branchId; make every copy match its class. */
    static class FixCopiedBranchIds implements Migration {
        public String id() { return "006-fix-copied-branch-ids"; }
        public String description() { return "Make branchId on exam results and faculty assignments equal the branch of their class"; }

        public Result run(MongoTemplate mongo, boolean dryRun) {
            long results = 0, assignments = 0;
            for (Document cls : mongo.getCollection("academic_classes").find()) {
                String classId = cls.getObjectId("_id").toHexString();
                String branchId = cls.getString("branchId");
                Query wrong = Query.query(Criteria.where("classId").is(classId).and("branchId").ne(branchId));
                results += mongo.count(wrong, "exam_results");
                assignments += mongo.count(wrong, "faculty_assignments");
                if (!dryRun) {
                    mongo.updateMulti(wrong, new Update().set("branchId", branchId), "exam_results");
                    mongo.updateMulti(wrong, new Update().set("branchId", branchId), "faculty_assignments");
                }
            }
            String verb = dryRun ? "would be corrected" : "corrected";
            return new Result(true, List.of(results + " exam result(s) and " + assignments + " faculty assignment(s) " + verb));
        }
    }
}
