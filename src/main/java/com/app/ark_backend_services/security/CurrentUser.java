package com.app.ark_backend_services.security;

import com.app.ark_backend_services.model.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** Thin accessor for the authenticated user. Access decisions belong in {@link AccessPolicy}. */
public final class CurrentUser {

    private CurrentUser() {}

    public static User get() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof User) {
            return (User) auth.getPrincipal();
        }
        return null;
    }

    public static String getOrganizationId() {
        User user = get();
        return user != null ? user.getOrganizationId() : null;
    }

    public static String getBranchId() {
        User user = get();
        return user != null ? user.getBranchId() : null;
    }

    public static boolean isSuperAdmin() {
        User user = get();
        return user != null && user.getRole() == User.Role.SUPER_ADMIN;
    }
}
