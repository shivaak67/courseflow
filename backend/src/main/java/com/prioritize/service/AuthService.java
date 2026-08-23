package com.prioritize.service;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.prioritize.dto.AuthResponse;
import com.prioritize.dto.LoginRequest;
import com.prioritize.dto.RegisterRequest;
import com.prioritize.dto.UserResponse;
import com.prioritize.exception.ApiException;
import com.prioritize.mapper.UserMapper;
import com.prioritize.model.AuthProvider;
import com.prioritize.model.Role;
import com.prioritize.model.User;
import com.prioritize.repository.UserRepository;
import com.prioritize.security.JwtService;
import com.prioritize.security.UserPrincipal;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        validatePasswordConfirmation(request.password(), request.passwordConfirmation());

        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Email is already registered");
        }

        User user = new User();
        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setAuthProvider(AuthProvider.LOCAL);
        user.setRole(Role.USER);

        User saved = userRepository.save(user);
        return toAuthResponse(saved);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String email = normalizeEmail(request.email());
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));

        if (user.getAuthProvider() != AuthProvider.LOCAL || user.getPasswordHash() == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }

        return toAuthResponse(user);
    }

    @Transactional(readOnly = true)
    public UserResponse me(UserPrincipal principal) {
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "User not found"));
        return UserMapper.toResponse(user);
    }

    private AuthResponse toAuthResponse(User user) {
        String token = jwtService.generateToken(user.getId(), user.getEmail());
        return new AuthResponse(token, jwtService.getExpirationMs(), UserMapper.toResponse(user));
    }

    private void validatePasswordConfirmation(String password, String confirmation) {
        if (!password.equals(confirmation)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "passwordConfirmation must match password");
        }
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }
}
