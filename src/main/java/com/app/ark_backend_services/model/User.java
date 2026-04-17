package com.app.ark_backend_services.model;

import lombok.Data;
import lombok.ToString;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@ToString(exclude = "password")
@Document(collection = "users")
@CompoundIndex(name = "org_email_idx", def = "{'organizationId': 1, 'email': 1}", unique = true)
public class User {

    @Id
    private String id;

    private String fullName;

    private String email;

    private String password;

    private Role role;

    private String organizationId;

    private String branchId;

    private String department;

    private UserStatus status;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    public enum Role {
        SUPER_ADMIN, ORG_ADMIN, ADMIN, USER
    }

    public enum UserStatus {
        ACTIVE, INACTIVE, LOCKED
    }
}
