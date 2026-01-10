package com.grievance.security;

import com.grievance.entity.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SecurityUtils {

    public static User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof User) {
            return (User) principal;
        }

        return null;
    }

    public static Long getCurrentUserId() {
        User user = getCurrentUser();
        return user != null ? user.getId() : null;
    }

    public static String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null ? authentication.getName() : null;
    }

    public static boolean isAdmin() {
        User user = getCurrentUser();
        return user != null &&
                (User.Role.ADMIN.equals(user.getRole()) ||
                        User.Role.SUPER_ADMIN.equals(user.getRole()));
    }

    public static boolean isAgent() {
        User user = getCurrentUser();
        return user != null && User.Role.AGENT.equals(user.getRole());
    }

    public static boolean isSuperAdmin() {
        User user = getCurrentUser();
        return user != null && User.Role.SUPER_ADMIN.equals(user.getRole());
    }
}