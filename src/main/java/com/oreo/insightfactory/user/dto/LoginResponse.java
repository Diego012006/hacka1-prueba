package com.oreo.insightfactory.user.dto;

import com.oreo.insightfactory.user.UserRole;

public record LoginResponse(
        String token,
        long expiresIn,
        UserRole role,
        String branch
) {
}
