package com.app.ark_backend_services.model;

import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/** An organization's subject catalog (e.g. MATH / Mathematics). */
@Data
@Document(collection = "subjects")
@CompoundIndex(name = "org_code_idx", def = "{'organizationId': 1, 'code': 1}", unique = true)
public class Subject {

    @Id
    private String id;

    private String organizationId;

    private String code;

    private String name;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}
