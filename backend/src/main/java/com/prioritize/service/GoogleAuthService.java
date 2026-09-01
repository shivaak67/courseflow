package com.prioritize.service;

import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.prioritize.dto.AuthResponse;
import com.prioritize.exception.ApiException;
import com.prioritize.mapper.UserMapper;
import com.prioritize.model.AuthProvider;
import com.prioritize.model.Role;
import com.prioritize.model.User;
import com.prioritize.repository.UserRepository;
import com.prioritize.security.JwtService;

@Service
public class GoogleAuthService {

    private final UserRepository userRepository;
    private final JwtService jwtService;

    public GoogleAuthService(UserRepository userRepository, JwtService jwtService) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse loginOrRegister(OidcUser oidcUser) {
        if (oidcUser.getEmail() == null || oidcUser.getEmail().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Google account email is required");
        }
        if (!Boolean.TRUE.equals(oidcUser.getEmailVerified())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Google account email must be verified");
        }

        String email = oidcUser.getEmail().trim().toLowerCase(Locale.ROOT);
        String subject = oidcUser.getSubject();
        User user = userRepository.findByProviderId(subject)
                .or(() -> userRepository.findByEmailIgnoreCase(email))
                .map(existing -> linkOrRefresh(existing, subject, oidcUser))
                .orElseGet(() -> createGoogleUser(email, subject, oidcUser));

        return new AuthResponse(
                jwtService.generateToken(user.getId(), user.getEmail()),
                jwtService.getExpirationMs(),
                UserMapper.toResponse(user));
    }

    private User linkOrRefresh(User existing, String subject, OidcUser oidcUser) {
        if (existing.getProviderId() == null || existing.getProviderId().isBlank()) {
            existing.setProviderId(subject);
        } else if (!existing.getProviderId().equals(subject)) {
            throw new ApiException(HttpStatus.CONFLICT, "Email is already linked to another account");
        }

        if (existing.getAuthProvider() == AuthProvider.LOCAL) {
            // Keep LOCAL password; Google is an additional sign-in method via providerId.
            existing.setProviderId(subject);
        } else {
            existing.setAuthProvider(AuthProvider.GOOGLE);
            existing.setProviderId(subject);
        }

        if (isBlank(existing.getFirstName())) {
            existing.setFirstName(resolveFirstName(oidcUser));
        }
        if (isBlank(existing.getLastName())) {
            existing.setLastName(resolveLastName(oidcUser));
        }
        return userRepository.save(existing);
    }

    private User createGoogleUser(String email, String subject, OidcUser oidcUser) {
        User user = new User();
        user.setEmail(email);
        user.setFirstName(resolveFirstName(oidcUser));
        user.setLastName(resolveLastName(oidcUser));
        user.setAuthProvider(AuthProvider.GOOGLE);
        user.setProviderId(subject);
        user.setPasswordHash(null);
        user.setRole(Role.USER);
        return userRepository.save(user);
    }

    private String resolveFirstName(OidcUser oidcUser) {
        if (!isBlank(oidcUser.getGivenName())) {
            return oidcUser.getGivenName().trim();
        }
        String fullName = oidcUser.getFullName();
        if (!isBlank(fullName)) {
            return fullName.trim().split("\\s+")[0];
        }
        return "Google";
    }

    private String resolveLastName(OidcUser oidcUser) {
        if (!isBlank(oidcUser.getFamilyName())) {
            return oidcUser.getFamilyName().trim();
        }
        String fullName = oidcUser.getFullName();
        if (!isBlank(fullName)) {
            String[] parts = fullName.trim().split("\\s+");
            if (parts.length > 1) {
                return parts[parts.length - 1];
            }
        }
        return "User";
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
