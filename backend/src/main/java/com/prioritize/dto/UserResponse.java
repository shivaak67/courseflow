package com.prioritize.dto;

import java.util.UUID;

import com.prioritize.model.AuthProvider;
import com.prioritize.model.Role;

public record UserResponse(
        UUID id,
        String firstName,
        String lastName,
        String email,
        AuthProvider authProvider,
        Role role) {
}
