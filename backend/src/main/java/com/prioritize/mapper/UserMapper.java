package com.prioritize.mapper;

import com.prioritize.dto.UserResponse;
import com.prioritize.model.User;

public final class UserMapper {

    private UserMapper() {
    }

    public static UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getAuthProvider(),
                user.getRole());
    }
}
