package com.prioritize.service;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.Executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.prioritize.config.AiProperties;

@Service
public class AiWarmupService {

    private static final Logger log = LoggerFactory.getLogger(AiWarmupService.class);

    private final AiProperties aiProperties;
    private final LlmClient llmClient;
    private final Executor aiWarmupExecutor;
    private final AtomicBoolean warming = new AtomicBoolean(false);
    private final AtomicBoolean ready = new AtomicBoolean(false);
    private final AtomicReference<String> statusMessage = new AtomicReference<>("AI assistant is not configured.");

    public AiWarmupService(
            AiProperties aiProperties,
            LlmClient llmClient,
            @Qualifier("aiWarmupExecutor") Executor aiWarmupExecutor) {
        this.aiProperties = aiProperties;
        this.llmClient = llmClient;
        this.aiWarmupExecutor = aiWarmupExecutor;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        if (!aiProperties.isConfigured() || !aiProperties.isWarmupEnabled()) {
            if (aiProperties.isConfigured()) {
                ready.set(true);
                statusMessage.set("AI assistant is ready.");
            }
            return;
        }

        aiWarmupExecutor.execute(this::runWarmup);
    }

    private void runWarmup() {
        if (!warming.compareAndSet(false, true)) {
            return;
        }

        statusMessage.set("Warming up the AI model. The first chat may take a minute.");
        log.info("Starting AI model warmup for {}", aiProperties.getModel());

        try {
            llmClient.warmup();
            ready.set(true);
            statusMessage.set("AI assistant is ready.");
            log.info("AI model warmup completed for {}", aiProperties.getModel());
        } catch (Exception ex) {
            ready.set(false);
            statusMessage.set("AI warmup failed. The first request may still be slow.");
            log.warn("AI model warmup failed: {}", ex.getMessage());
        } finally {
            warming.set(false);
        }
    }

    public boolean isConfigured() {
        return aiProperties.isConfigured();
    }

    public boolean isReady() {
        return !aiProperties.isConfigured() || ready.get();
    }

    public boolean isWarming() {
        return warming.get();
    }

    public String getStatusMessage() {
        return statusMessage.get();
    }
}
