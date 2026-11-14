package com.app.ark_backend_services.dto;

import com.app.ark_backend_services.model.User;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthResponse {

    private String accessToken;
    private String refreshToken;
    private String tokenType;
    /** Access token lifetime in seconds. */
    private long expiresIn;
    private UserInfo user;

    @Data
    @Builder
    public static class UserInfo {
        private String id;
        /** Same value as fullName; "name" is the contract field, fullName kept for existing clients. */
        private String name;
        private String fullName;
        private String email;
        private User.Role role;
        private String organizationId;
        private String branchId;

        public static UserInfo from(User user) {
            return UserInfo.builder()
                    .id(user.getId())
                    .name(user.getFullName())
                    .fullName(user.getFullName())
                    .email(user.getEmail())
                    .role(user.getRole())
                    .organizationId(user.getOrganizationId())
                    .branchId(user.getBranchId())
                    .build();
        }
    }
}
