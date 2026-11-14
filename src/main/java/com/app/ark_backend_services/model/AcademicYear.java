package com.app.ark_backend_services.model;

import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.time.LocalDate;

/** The academic years an organization works with. {@code label} is YYYY-YYYY; exactly one may be current. */
@Data
@Document(collection = "academic_years")
@CompoundIndex(name = "org_label_idx", def = "{'organizationId': 1, 'label': 1}", unique = true)
public class AcademicYear {

    @Id
    private String id;

    private String organizationId;

    private String label;

    private LocalDate startDate;

    private LocalDate endDate;

    private boolean current;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}
