package com.prioritize.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import com.prioritize.exception.ApiException;
import com.prioritize.model.AuthProvider;
import com.prioritize.model.Role;
import com.prioritize.model.User;
import com.prioritize.repository.UserRepository;
import com.prioritize.security.JwtService;
import com.prioritize.service.GoogleAuthService;

@ExtendWith(MockitoExtension.class)
class GoogleAuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtService jwtService;

    @Mock
    private OidcUser oidcUser;

    @InjectMocks
    private GoogleAuthService googleAuthService;

    @Test
    void createsGoogleUserWhenUnknown() {
        when(oidcUser.getEmail()).thenReturn("ada@example.com");
        when(oidcUser.getEmailVerified()).thenReturn(true);
        when(oidcUser.getSubject()).thenReturn("google-sub-1");
        when(oidcUser.getGivenName()).thenReturn("Ada");
        when(oidcUser.getFamilyName()).thenReturn("Lovelace");
        when(userRepository.findByProviderId("google-sub-1")).thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase("ada@example.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(UUID.randomUUID());
            return user;
        });
        when(jwtService.generateToken(any(UUID.class), any(String.class))).thenReturn("jwt-token");
        when(jwtService.getExpirationMs()).thenReturn(86400000L);

        var auth = googleAuthService.loginOrRegister(oidcUser);

        assertThat(auth.accessToken()).isEqualTo("jwt-token");
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getEmail()).isEqualTo("ada@example.com");
        assertThat(saved.getAuthProvider()).isEqualTo(AuthProvider.GOOGLE);
        assertThat(saved.getProviderId()).isEqualTo("google-sub-1");
        assertThat(saved.getPasswordHash()).isNull();
        assertThat(saved.getRole()).isEqualTo(Role.USER);
    }

    @Test
    void linksGoogleToExistingLocalAccount() {
        UUID id = UUID.randomUUID();
        User local = new User();
        local.setId(id);
        local.setEmail("ada@example.com");
        local.setFirstName("Ada");
        local.setLastName("Lovelace");
        local.setAuthProvider(AuthProvider.LOCAL);
        local.setPasswordHash("hash");
        local.setRole(Role.USER);

        when(oidcUser.getEmail()).thenReturn("ada@example.com");
        when(oidcUser.getEmailVerified()).thenReturn(true);
        when(oidcUser.getSubject()).thenReturn("google-sub-1");
        when(userRepository.findByProviderId("google-sub-1")).thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase("ada@example.com")).thenReturn(Optional.of(local));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtService.generateToken(id, "ada@example.com")).thenReturn("jwt-token");
        when(jwtService.getExpirationMs()).thenReturn(86400000L);

        googleAuthService.loginOrRegister(oidcUser);

        assertThat(local.getProviderId()).isEqualTo("google-sub-1");
        assertThat(local.getAuthProvider()).isEqualTo(AuthProvider.LOCAL);
        assertThat(local.getPasswordHash()).isEqualTo("hash");
    }

    @Test
    void rejectsUnverifiedEmail() {
        when(oidcUser.getEmail()).thenReturn("ada@example.com");
        when(oidcUser.getEmailVerified()).thenReturn(false);

        assertThatThrownBy(() -> googleAuthService.loginOrRegister(oidcUser))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("verified");
    }
}
