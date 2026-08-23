package com.prioritize.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.prioritize.dto.AuthResponse;
import com.prioritize.dto.LoginRequest;
import com.prioritize.dto.RegisterRequest;
import com.prioritize.exception.ApiException;
import com.prioritize.model.AuthProvider;
import com.prioritize.model.Role;
import com.prioritize.model.User;
import com.prioritize.repository.UserRepository;
import com.prioritize.security.JwtService;
import com.prioritize.service.AuthService;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    private User existingUser;

    @BeforeEach
    void setUp() {
        existingUser = new User();
        existingUser.setId(UUID.randomUUID());
        existingUser.setFirstName("Ada");
        existingUser.setLastName("Lovelace");
        existingUser.setEmail("ada@example.com");
        existingUser.setPasswordHash("hashed");
        existingUser.setAuthProvider(AuthProvider.LOCAL);
        existingUser.setRole(Role.USER);
    }

    @Test
    void registerCreatesLocalUserAndReturnsToken() {
        RegisterRequest request = new RegisterRequest(
                "Ada", "Lovelace", "ada@example.com", "password1", "password1");

        when(userRepository.existsByEmailIgnoreCase("ada@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password1")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(UUID.randomUUID());
            return user;
        });
        when(jwtService.generateToken(any(UUID.class), any(String.class))).thenReturn("jwt-token");
        when(jwtService.getExpirationMs()).thenReturn(86400000L);

        AuthResponse response = authService.register(request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getEmail()).isEqualTo("ada@example.com");
        assertThat(saved.getAuthProvider()).isEqualTo(AuthProvider.LOCAL);
        assertThat(saved.getRole()).isEqualTo(Role.USER);
        assertThat(saved.getPasswordHash()).isEqualTo("hashed");
        assertThat(response.accessToken()).isEqualTo("jwt-token");
        assertThat(response.expiresIn()).isEqualTo(86400000L);
        assertThat(response.user().email()).isEqualTo("ada@example.com");
    }

    @Test
    void registerRejectsMismatchedPasswordConfirmation() {
        RegisterRequest request = new RegisterRequest(
                "Ada", "Lovelace", "ada@example.com", "password1", "password2");

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void registerRejectsDuplicateEmail() {
        RegisterRequest request = new RegisterRequest(
                "Ada", "Lovelace", "ada@example.com", "password1", "password1");
        when(userRepository.existsByEmailIgnoreCase("ada@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void loginReturnsTokenForValidCredentials() {
        when(userRepository.findByEmailIgnoreCase("ada@example.com")).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches("password1", "hashed")).thenReturn(true);
        when(jwtService.generateToken(existingUser.getId(), existingUser.getEmail())).thenReturn("jwt-token");
        when(jwtService.getExpirationMs()).thenReturn(86400000L);

        AuthResponse response = authService.login(new LoginRequest("ada@example.com", "password1"));

        assertThat(response.accessToken()).isEqualTo("jwt-token");
        assertThat(response.user().id()).isEqualTo(existingUser.getId());
    }

    @Test
    void loginRejectsInvalidPassword() {
        when(userRepository.findByEmailIgnoreCase("ada@example.com")).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("ada@example.com", "wrong")))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED));
    }
}
