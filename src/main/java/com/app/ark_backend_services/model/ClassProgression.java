package com.app.ark_backend_services.model;

import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Data
@Document(collection = "class_progressions")
@CompoundIndex(name = "org_branch_unique", def = "{'organizationId': 1, 'branchId': 1}", unique = true)
public class ClassProgression {

    @Id
    private String id;

    private String organizationId;

    private String branchId;

    private List<ClassLevel> sequence;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    @Data
    public static class ClassLevel {
        private String className;
        private Integer displayOrder;
        private Boolean isTerminal;
    }
}
