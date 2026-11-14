package com.app.ark_backend_services.service;

import com.app.ark_backend_services.model.AcademicClass;
import com.app.ark_backend_services.model.Branch;
import com.app.ark_backend_services.model.ExamSubject;
import com.app.ark_backend_services.model.Examination;
import com.app.ark_backend_services.repository.AcademicClassRepository;
import com.app.ark_backend_services.repository.BranchRepository;
import com.app.ark_backend_services.repository.ExamSubjectRepository;
import com.app.ark_backend_services.security.AccessPolicy;
import com.app.ark_backend_services.util.AcademicYears;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Two reports, computed in the database with aggregation pipelines: enrollment summary and exam performance.
 * Both are scoped to the caller (branch-scoped roles only ever see their own branch).
 */
@Service
@RequiredArgsConstructor
public class ReportService {

    private final MongoTemplate mongoTemplate;
    private final BranchRepository branchRepository;
    private final AcademicClassRepository classRepository;
    private final ExamSubjectRepository examSubjectRepository;

    /** A tabular report: column names plus one map per row, so JSON and CSV share one shape. */
    public record Report(String title, String academicYear, List<String> columns, List<Map<String, Object>> rows) {}

    /** Enrollment counts per branch and class for an academic year. */
    public Report enrollmentSummary(String organizationId, String branchId, String academicYear) {
        AccessPolicy.assertOrg(organizationId);
        AcademicYears.validate(academicYear);
        String branch = AccessPolicy.restrictBranch(branchId);

        Document match = new Document("organizationId", organizationId).append("academicYear", academicYear);
        if (branch != null) {
            match.append("branchId", branch);
        }
        List<Document> groups = mongoTemplate.getCollection("student_enrollments").aggregate(List.of(
                new Document("$match", match),
                new Document("$group", new Document("_id", new Document("branchId", "$branchId").append("classId", "$classId"))
                        .append("total", new Document("$sum", 1))
                        .append("active", new Document("$sum", cond(new Document("$eq", List.of("$status", "ACTIVE")))))
                        .append("completed", new Document("$sum", cond(new Document("$eq", List.of("$status", "COMPLETED"))))))
        )).into(new ArrayList<>());

        Map<String, String> branchNames = branchRepository.findByOrganizationId(organizationId).stream()
                .collect(Collectors.toMap(Branch::getId, Branch::getName, (a, b) -> a));
        List<String> classIds = groups.stream().map(d -> ((Document) d.get("_id")).getString("classId")).distinct().toList();
        Map<String, AcademicClass> classes = classRepository.findAllById(classIds).stream()
                .collect(Collectors.toMap(AcademicClass::getId, c -> c));

        List<Map<String, Object>> rows = new ArrayList<>();
        long total = 0, active = 0, completed = 0;
        for (Document g : groups) {
            Document id = (Document) g.get("_id");
            AcademicClass c = classes.get(id.getString("classId"));
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("branch", branchNames.getOrDefault(id.getString("branchId"), id.getString("branchId")));
            row.put("class", c != null ? c.getName() : id.getString("classId"));
            row.put("section", c != null ? c.getSection() : null);
            row.put("total", n(g, "total"));
            row.put("active", n(g, "active"));
            row.put("completed", n(g, "completed"));
            rows.add(row);
            total += n(g, "total");
            active += n(g, "active");
            completed += n(g, "completed");
        }
        rows.sort(Comparator.comparing((Map<String, Object> r) -> String.valueOf(r.get("branch")))
                .thenComparing(r -> String.valueOf(r.get("class")))
                .thenComparing(r -> String.valueOf(r.get("section"))));
        Map<String, Object> totals = new LinkedHashMap<>();
        totals.put("branch", "TOTAL");
        totals.put("class", null);
        totals.put("section", null);
        totals.put("total", total);
        totals.put("active", active);
        totals.put("completed", completed);
        rows.add(totals);

        return new Report("Enrollment summary", academicYear,
                List.of("branch", "class", "section", "total", "active", "completed"), rows);
    }

    /** Pass rate, average, highest and lowest marks per exam subject for an academic year. */
    public Report examPerformance(String organizationId, String branchId, String academicYear) {
        AccessPolicy.assertOrg(organizationId);
        AcademicYears.validate(academicYear);
        String branch = AccessPolicy.restrictBranch(branchId);

        Criteria examScope = Criteria.where("organizationId").is(organizationId).and("academicYear").is(academicYear);
        if (branch != null) {
            examScope = examScope.and("branchId").is(branch);
        }
        List<Examination> exams = mongoTemplate.find(Query.query(examScope), Examination.class);
        List<String> columns = List.of("examination", "subject", "class", "section", "appeared", "absent",
                "passed", "passPercentage", "average", "highest", "lowest");
        if (exams.isEmpty()) {
            return new Report("Exam performance", academicYear, columns, List.of());
        }
        Map<String, Examination> examById = exams.stream().collect(Collectors.toMap(Examination::getId, e -> e));

        Document appearedCond = new Document("$in", List.of("$status", List.of("PASS", "FAIL")));
        Object marksIfAppeared = new Document("$cond", Arrays.asList(appearedCond, "$marksObtained", null));
        List<Document> groups = mongoTemplate.getCollection("exam_results").aggregate(List.of(
                new Document("$match", new Document("organizationId", organizationId)
                        .append("examinationId", new Document("$in", new ArrayList<>(examById.keySet())))),
                new Document("$group", new Document("_id", "$examSubjectId")
                        .append("appeared", new Document("$sum", cond(appearedCond)))
                        .append("absent", new Document("$sum", cond(new Document("$eq", List.of("$status", "ABSENT")))))
                        .append("passed", new Document("$sum", cond(new Document("$eq", List.of("$status", "PASS")))))
                        .append("average", new Document("$avg", marksIfAppeared))
                        .append("highest", new Document("$max", marksIfAppeared))
                        .append("lowest", new Document("$min", marksIfAppeared)))
        )).into(new ArrayList<>());

        List<String> subjectIds = groups.stream().map(d -> d.getString("_id")).toList();
        Map<String, ExamSubject> subjects = examSubjectRepository.findAllById(subjectIds).stream()
                .collect(Collectors.toMap(ExamSubject::getId, s -> s));
        Map<String, AcademicClass> classes = classRepository.findAllById(
                subjects.values().stream().map(ExamSubject::getClassId).distinct().toList()).stream()
                .collect(Collectors.toMap(AcademicClass::getId, c -> c));

        List<Map<String, Object>> rows = new ArrayList<>();
        for (Document g : groups) {
            ExamSubject s = subjects.get(g.getString("_id"));
            if (s == null) continue;
            AcademicClass c = classes.get(s.getClassId());
            long appeared = n(g, "appeared");
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("examination", examById.get(s.getExaminationId()).getName());
            row.put("subject", s.getSubjectName());
            row.put("class", c != null ? c.getName() : s.getClassId());
            row.put("section", c != null ? c.getSection() : null);
            row.put("appeared", appeared);
            row.put("absent", n(g, "absent"));
            row.put("passed", n(g, "passed"));
            row.put("passPercentage", appeared == 0 ? 0.0 : round2(n(g, "passed") * 100.0 / appeared));
            row.put("average", g.get("average") == null ? null : round2(((Number) g.get("average")).doubleValue()));
            row.put("highest", g.get("highest"));
            row.put("lowest", g.get("lowest"));
            rows.add(row);
        }
        rows.sort(Comparator.comparing((Map<String, Object> r) -> String.valueOf(r.get("examination")))
                .thenComparing(r -> String.valueOf(r.get("class")))
                .thenComparing(r -> String.valueOf(r.get("subject"))));
        return new Report("Exam performance", academicYear, columns, rows);
    }

    /** Renders a report as RFC 4180 CSV. */
    public static String toCsv(Report report) {
        StringBuilder sb = new StringBuilder();
        sb.append(report.columns().stream().map(ReportService::csv).collect(Collectors.joining(","))).append("\r\n");
        for (Map<String, Object> row : report.rows()) {
            sb.append(report.columns().stream().map(c -> csv(row.get(c))).collect(Collectors.joining(","))).append("\r\n");
        }
        return sb.toString();
    }

    private static String csv(Object value) {
        if (value == null) return "";
        String s = String.valueOf(value);
        // neutralise spreadsheet formula injection, then quote when needed
        if (!s.isEmpty() && "=+-@".indexOf(s.charAt(0)) >= 0 && !(value instanceof Number)) {
            s = "'" + s;
        }
        return (s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r"))
                ? "\"" + s.replace("\"", "\"\"") + "\"" : s;
    }

    private static Document cond(Object predicate) {
        return new Document("$cond", Arrays.asList(predicate, 1, 0));
    }

    private static long n(Document d, String key) {
        Object v = d.get(key);
        return v instanceof Number num ? num.longValue() : 0L;
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
