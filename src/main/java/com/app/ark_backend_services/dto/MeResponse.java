package com.app.ark_backend_services.dto;

import com.app.ark_backend_services.model.User;
import com.app.ark_backend_services.security.AccessScope;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/** Who am I and what can I see: the single source of truth for web and mobile clients. */
@Data
@Builder
public class MeResponse {

    private AuthResponse.UserInfo user;
    private Scope scope;

    @Data
    @Builder
    public static class Scope {
        private User.Role role;
        private String organizationId;
        /** Branches the caller is limited to; empty with allBranches=true means every branch in the org. */
        private List<String> branchIds;
        private List<String> studentIds;
        private boolean allBranches;
    }

    public static MeResponse from(User user) {
        AccessScope s = AccessScope.of(user);
        return MeResponse.builder()
                .user(AuthResponse.UserInfo.from(user))
                .scope(Scope.builder()
                        .role(s.role())
                        .organizationId(s.organizationId())
                        .branchIds(List.copyOf(s.branchIds()))
                        .studentIds(List.copyOf(s.studentIds()))
                        .allBranches(s.allBranches())
                        .build())
                .build();
    }
}
