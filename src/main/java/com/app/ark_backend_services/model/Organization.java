package com.app.ark_backend_services.model;

import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;

@Data
@Document(collection = "organizations")
public class Organization {

    @Id
    private String id;

    @Indexed(unique = true)
    private String arkId;

    @Indexed(unique = true)
    private String name;

    private String address;

    private String contactEmail;

    private String contactPhone;

    private String website;

    private String logoUrl;

    private OrganizationStatus status;

    @Version
    private Long version;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    public enum OrganizationStatus {
        ACTIVE, INACTIVE, SUSPENDED
    }
}
