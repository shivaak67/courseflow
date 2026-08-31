package com.prioritize.dto;

public record AssistantStatusResponse(
        boolean configured,
        boolean ready,
        boolean warming,
        String message) {
}
