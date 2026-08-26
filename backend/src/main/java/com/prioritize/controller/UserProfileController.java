package com.prioritize.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.prioritize.dto.PhoneVerifyRequest;
import com.prioritize.dto.UserProfileRequest;
import com.prioritize.dto.UserProfileResponse;
import com.prioritize.security.CurrentUserService;
import com.prioritize.service.UserProfileService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/me")
public class UserProfileController {

    private final UserProfileService userProfileService;
    private final CurrentUserService currentUserService;

    public UserProfileController(
            UserProfileService userProfileService,
            CurrentUserService currentUserService) {
        this.userProfileService = userProfileService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public UserProfileResponse get() {
        UUID userId = currentUserService.requireCurrentUserId();
        return userProfileService.getProfile(userId);
    }

    @PutMapping
    public UserProfileResponse update(@Valid @RequestBody UserProfileRequest request) {
        UUID userId = currentUserService.requireCurrentUserId();
        return userProfileService.updateProfile(userId, request);
    }

    @PostMapping("/phone/request-verification")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void requestPhoneVerification() {
        UUID userId = currentUserService.requireCurrentUserId();
        userProfileService.requestPhoneVerification(userId);
    }

    @PostMapping("/phone/verify")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void verifyPhone(@Valid @RequestBody PhoneVerifyRequest request) {
        UUID userId = currentUserService.requireCurrentUserId();
        userProfileService.verifyPhone(userId, request.code());
    }
}
