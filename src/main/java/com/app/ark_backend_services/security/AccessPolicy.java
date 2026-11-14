package com.app.ark_backend_services.security;

import com.app.ark_backend_services.model.User;
import org.springframework.security.access.AccessDeniedException;

/**
 * Single place for tenant and branch access decisions. Services call these instead of keeping their own
 * copies: {@code assertOrg} first, then {@code assertBranch} / {@code restrictBranch}, and
 * {@code assertCanWrite} on mutations.
 */
public final class AccessPolicy {

    private AccessPolicy() {}

    /** The caller's scope; denies when nobody is authenticated. */
    public static AccessScope scope() {
        User user = CurrentUser.get();
        if (user == null) {
            throw new AccessDeniedException("Authentication required");
        }
        return AccessScope.of(user);
    }

    /** Caller must belong to the organization (SUPER_ADMIN may access any). */
    public static void assertOrg(String organizationId) {
        AccessScope scope = scope();
        if (scope.isSuperAdmin()) {
            return;
        }
        if (organizationId == null || !organizationId.equals(scope.organizationId())) {
            throw new AccessDeniedException("You do not have access to this organization");
        }
    }

    /** Branch-scoped roles (ADMIN, USER) may only touch their own branch; other roles pass. */
    public static void assertBranch(String branchId) {
        AccessScope scope = scope();
        if (scope.allBranches()) {
            return;
        }
        if (branchId == null || !scope.branchIds().contains(branchId)) {
            throw new AccessDeniedException("You do not have access to this branch");
        }
    }

    /**
     * Resolves the branch a list/query should be limited to. Branch-scoped callers always get their own
     * branch (and are denied if they ask for another); org-wide callers get what they asked for (may be null).
     */
    public static String restrictBranch(String requestedBranchId) {
        AccessScope scope = scope();
        if (scope.allBranches()) {
            return requestedBranchId;
        }
        if (scope.branchIds().isEmpty()) {
            throw new AccessDeniedException("You are not assigned to a branch");
        }
        String own = scope.branchIds().iterator().next();
        if (requestedBranchId != null && !requestedBranchId.equals(own)) {
            throw new AccessDeniedException("You do not have access to this branch");
        }
        return own;
    }

    /** True when the caller is limited to a single branch. */
    public static boolean isBranchScoped() {
        return !scope().allBranches();
    }

    /** Read-only roles (USER) may not modify anything. */
    public static void assertCanWrite() {
        if (!scope().role().canWrite()) {
            throw new AccessDeniedException("Your role has read-only access");
        }
    }

    /** USER sees government ID numbers masked. */
    public static boolean shouldMaskSensitive() {
        User user = CurrentUser.get();
        return user != null && user.getRole() == User.Role.USER;
    }
}
