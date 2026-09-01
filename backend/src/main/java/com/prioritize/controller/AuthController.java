package com.prioritize.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.prioritize.dto.AuthConfigResponse;
import com.prioritize.dto.AuthResponse;
import com.prioritize.dto.LoginRequest;
import com.prioritize.dto.RegisterRequest;
import com.prioritize.dto.UserResponse;
import com.prioritize.config.OAuthProperties;
import com.prioritize.security.UserPrincipal;
import com.prioritize.service.AuthService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final OAuthProperties oauthProperties;

    public AuthController(AuthService authService, OAuthProperties oauthProperties) {
        this.authService = authService;
        this.oauthProperties = oauthProperties;
    }

    @GetMapping("/config")
    public ResponseEntity<AuthConfigResponse> config() {
        return ResponseEntity.ok(new AuthConfigResponse(oauthProperties.getGoogle().isEnabled()));
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(authService.me(principal));
    }
}
