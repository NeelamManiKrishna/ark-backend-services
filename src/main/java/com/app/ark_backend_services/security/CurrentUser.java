package com.app.ark_backend_services.security;

import com.app.ark_backend_services.model.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

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

    public static boolean isSuperAdmin() {
        User user = get();
        return user != null && user.getRole() == User.Role.SUPER_ADMIN;
    }

    public static boolean belongsToOrg(String organizationId) {
        if (organizationId == null) return false;
        User user = get();
        if (user == null) return false;
        if (user.getRole() == User.Role.SUPER_ADMIN) return true;
        return organizationId.equals(user.getOrganizationId());
    }
}
