package com.app.ark_backend_services.model;

import lombok.Data;
import lombok.ToString;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@ToString(exclude = "password")
@Document(collection = "users")
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

    /** Bumped on password change, role change or disable; access tokens carry it in the "ver" claim. */
    private int tokenVersion;

    private int failedAttempts;

    private Instant lockedUntil;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    /**
     * Roles are an extensible set (PARENT will be added later). Access rules must go through
     * {@code AccessPolicy}/{@code AccessScope}, never assume exactly four roles.
     */
    public enum Role {
        SUPER_ADMIN, ORG_ADMIN, ADMIN, USER;

        /** True for roles restricted to a single branch (ADMIN, USER). */
        public boolean isBranchScoped() {
            return this == ADMIN || this == USER;
        }

        /** True for roles allowed to modify records at all. */
        public boolean canWrite() {
            return this != USER;
        }
    }

    public enum UserStatus {
        ACTIVE, INACTIVE, LOCKED
    }
}
