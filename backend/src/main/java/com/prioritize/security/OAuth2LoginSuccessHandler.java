package com.prioritize.security;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.prioritize.config.OAuthProperties;
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

        String token = googleAuthService.loginOrRegister(oidcUser);
        String redirectBase = oauthProperties.getSuccessRedirect();
        String separator = redirectBase.contains("?") ? "&" : "?";
        String location = redirectBase + separator + "token=" + URLEncoder.encode(token, StandardCharsets.UTF_8);
        response.sendRedirect(location);
    }
}
