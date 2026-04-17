package com.app.ark_backend_services.dto;

import com.app.ark_backend_services.model.User;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class UserResponse {

    private String id;
    private String fullName;
    private String email;
    private User.Role role;
    private String organizationId;
    private String branchId;
    private String department;
    private User.UserStatus status;
    private Instant createdAt;
    private Instant updatedAt;

    public static UserResponse from(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole())
                .organizationId(user.getOrganizationId())
                .branchId(user.getBranchId())
                .department(user.getDepartment())
                .status(user.getStatus())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
