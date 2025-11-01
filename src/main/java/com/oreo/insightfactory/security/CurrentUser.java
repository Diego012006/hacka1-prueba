package com.oreo.insightfactory.security;

import com.oreo.insightfactory.user.User;
import com.oreo.insightfactory.user.UserRole;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class CurrentUser {

    private CurrentUser() {
    }

    public static User get() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AppUserDetails details) {
            return details.getUser();
        }
        return null;
    }

    public static UserRole getRole() {
        User user = get();
        return user != null ? user.getRole() : null;
    }

    public static String getBranch() {
        User user = get();
        return user != null ? user.getBranch() : null;
    }
}
