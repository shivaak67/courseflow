package com.prioritize.security;

import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.prioritize.exception.UnauthorizedException;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Temporary ownership stand-in until JWT auth merges.
 * Controllers/services resolve the acting user from the {@code X-User-Id} header.
 * TODO: replace with SecurityContext JWT principal (Agent A).
 */
@Component
public class CurrentUserService {

    public static final String USER_ID_HEADER = "X-User-Id";

    public UUID requireCurrentUserId() {
        HttpServletRequest request = currentRequest();
        String header = request.getHeader(USER_ID_HEADER);
        if (header == null || header.isBlank()) {
            throw new UnauthorizedException("Missing " + USER_ID_HEADER + " header");
        }
        try {
            return UUID.fromString(header.trim());
        } catch (IllegalArgumentException ex) {
            throw new UnauthorizedException("Invalid " + USER_ID_HEADER + " header");
        }
    }

    private HttpServletRequest currentRequest() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (!(attributes instanceof ServletRequestAttributes servletAttributes)) {
            throw new UnauthorizedException("No request context available");
        }
        return servletAttributes.getRequest();
    }
}
