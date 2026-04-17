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
    private UserInfo user;

    @Data
    @Builder
    public static class UserInfo {
        private String id;
        private String fullName;
        private String email;
        private User.Role role;
        private String organizationId;
    }
}
