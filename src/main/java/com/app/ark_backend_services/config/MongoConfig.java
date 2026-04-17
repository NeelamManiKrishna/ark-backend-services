package com.app.ark_backend_services.config;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.core.MongoTemplate;

@Configuration
@EnableMongoAuditing
public class MongoConfig {

    private final MongoTemplate mongoTemplate;

    public MongoConfig(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void ensureIndexes() {
        // Drop legacy unique indexes — arkId is the unique identifier; rollNumber and employeeId are optional display labels
        dropIndexSilently("students", "org_student_rollno");
        dropIndexSilently("students", "organizationId_1_rollNumber_1");
        dropIndexSilently("faculty", "org_faculty_empid");
        dropIndexSilently("faculty", "organizationId_1_employeeId_1");

        // Drop stale academic_classes index that's missing academicYear — replaced by {orgId, branchId, name, section, academicYear}
        dropIndexSilently("academic_classes", "organizationId_1_branchId_1_name_1_section_1");
    }

    private void dropIndexSilently(String collection, String indexName) {
        try {
            mongoTemplate.getCollection(collection).dropIndex(indexName);
        } catch (Exception ignored) {
            // Index may not exist
        }
    }
}
