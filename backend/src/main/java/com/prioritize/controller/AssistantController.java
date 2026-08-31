package com.prioritize.controller;

import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.prioritize.dto.AssistantChatRequest;
import com.prioritize.dto.AssistantChatResponse;
import com.prioritize.dto.AssistantStatusResponse;
import com.prioritize.security.CurrentUserService;
import com.prioritize.service.AiWarmupService;
import com.prioritize.service.AssistantService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/assistant")
public class AssistantController {

    private final AssistantService assistantService;
    private final AiWarmupService aiWarmupService;
    private final CurrentUserService currentUserService;

    public AssistantController(
            AssistantService assistantService,
            AiWarmupService aiWarmupService,
            CurrentUserService currentUserService) {
        this.assistantService = assistantService;
        this.aiWarmupService = aiWarmupService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/status")
    public AssistantStatusResponse status() {
        return new AssistantStatusResponse(
                aiWarmupService.isConfigured(),
                aiWarmupService.isReady(),
                aiWarmupService.isWarming(),
                aiWarmupService.getStatusMessage());
    }

    @PostMapping("/chat")
    public AssistantChatResponse chat(@Valid @RequestBody AssistantChatRequest request) {
        UUID userId = currentUserService.requireCurrentUserId();
        return assistantService.chat(userId, request);
    }
}
