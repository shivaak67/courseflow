package com.prioritize.security;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import com.prioritize.config.OAuthProperties;
import com.prioritize.dto.AuthResponse;
import com.prioritize.dto.UserResponse;
import com.prioritize.service.GoogleAuthService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@ConditionalOnProperty(name = "app.oauth.google.enabled", havingValue = "true")
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final GoogleAuthService googleAuthService;
    private final OAuthProperties oauthProperties;

    public OAuth2LoginSuccessHandler(GoogleAuthService googleAuthService, OAuthProperties oauthProperties) {
        this.googleAuthService = googleAuthService;
        this.oauthProperties = oauthProperties;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException {
        if (!(authentication.getPrincipal() instanceof OidcUser oidcUser)) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unsupported OAuth principal");
            return;
        }

        AuthResponse auth = googleAuthService.loginOrRegister(oidcUser);
        UserResponse user = auth.user();

        if (request.getSession(false) != null) {
            request.getSession(false).invalidate();
        }

        URI location = UriComponentsBuilder.fromUriString(oauthProperties.getSuccessRedirect())
                .queryParam("token", auth.accessToken())
                .queryParam("userId", user.id().toString())
                .queryParam("email", user.email())
                .queryParam("firstName", user.firstName())
                .queryParam("lastName", user.lastName())
                .queryParam("authProvider", user.authProvider().name())
                .queryParam("phoneVerified", user.phoneVerified())
                .build()
                .encode(StandardCharsets.UTF_8)
                .toUri();
        response.sendRedirect(location.toString());
    }
}
