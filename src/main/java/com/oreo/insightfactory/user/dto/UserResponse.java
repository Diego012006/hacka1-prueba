package com.oreo.insightfactory.user.dto;

import com.oreo.insightfactory.user.User;
import com.oreo.insightfactory.user.UserRole;
import java.time.OffsetDateTime;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String username,
        String email,
        UserRole role,
        String branch,
        OffsetDateTime createdAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole(),
                user.getBranch(),
                user.getCreatedAt()
        );
    }
}
