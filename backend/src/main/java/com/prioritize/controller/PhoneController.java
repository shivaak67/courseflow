package com.prioritize.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.prioritize.dto.PhoneStatusResponse;
import com.prioritize.dto.PhoneUpdateRequest;
import com.prioritize.dto.PhoneVerifyRequest;
import com.prioritize.security.CurrentUserService;
import com.prioritize.service.PhoneVerificationService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/phone")
public class PhoneController {

    private final PhoneVerificationService phoneVerificationService;
    private final CurrentUserService currentUserService;

    public PhoneController(
            PhoneVerificationService phoneVerificationService,
            CurrentUserService currentUserService) {
        this.phoneVerificationService = phoneVerificationService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public PhoneStatusResponse status() {
        UUID userId = currentUserService.requireCurrentUserId();
        return phoneVerificationService.status(userId);
    }

    @PutMapping
    public PhoneStatusResponse updatePhone(@Valid @RequestBody PhoneUpdateRequest request) {
        UUID userId = currentUserService.requireCurrentUserId();
        return phoneVerificationService.updatePhone(userId, request);
    }

    @PostMapping("/send-code")
    public PhoneStatusResponse sendCode() {
        UUID userId = currentUserService.requireCurrentUserId();
        return phoneVerificationService.sendCode(userId);
    }

    @PostMapping("/verify")
    public PhoneStatusResponse verify(@Valid @RequestBody PhoneVerifyRequest request) {
        UUID userId = currentUserService.requireCurrentUserId();
        return phoneVerificationService.verify(userId, request);
    }
}
