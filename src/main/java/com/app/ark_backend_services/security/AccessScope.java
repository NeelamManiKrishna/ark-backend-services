package com.app.ark_backend_services.security;

import com.app.ark_backend_services.model.User;

import java.util.Set;

/**
 * Everything an access decision needs to know about the caller. Parents will later get {@code studentIds}
 * from a parent-student link without changing the rule engine.
 *
 * @param allBranches true when the role sees every branch of its organization (SUPER_ADMIN, ORG_ADMIN)
 */
public record AccessScope(User.Role role, String organizationId, Set<String> branchIds,
                          Set<String> studentIds, boolean allBranches) {

    public static AccessScope of(User user) {
        User.Role role = user.getRole();
        boolean branchScoped = role.isBranchScoped();
        Set<String> branches = (branchScoped && user.getBranchId() != null)
                ? Set.of(user.getBranchId()) : Set.of();
        return new AccessScope(role, user.getOrganizationId(), branches, Set.of(), !branchScoped);
    }

    public boolean isSuperAdmin() {
        return role == User.Role.SUPER_ADMIN;
    }
}
