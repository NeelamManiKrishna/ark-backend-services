package com.app.ark_backend_services.model;

import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Document(collection = "branches")
@CompoundIndex(name = "org_branch_name", def = "{'organizationId': 1, 'name': 1}", unique = true)
public class Branch {

    @Id
    private String id;

    @org.springframework.data.mongodb.core.index.Indexed(unique = true)
    private String arkId;

    private String organizationId;

    private String name;

    private String address;

    private String city;

    private String state;

    private String zipCode;

    private String contactEmail;

    private String contactPhone;

    private BranchStatus status;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    public enum BranchStatus {
        ACTIVE, INACTIVE
    }
}
