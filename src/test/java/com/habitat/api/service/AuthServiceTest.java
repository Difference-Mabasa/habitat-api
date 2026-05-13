package com.habitat.api.service;

import com.habitat.api.constants.ErrorMessages;
import com.habitat.api.dto.auth.AuthResponse;
import com.habitat.api.dto.auth.LoginRequest;
import com.habitat.api.dto.auth.RegisterRequest;
import com.habitat.api.entity.User;
import com.habitat.api.enums.Role;
import com.habitat.api.exception.ConflictException;
import com.habitat.api.exception.ResourceNotFoundException;
import com.habitat.api.exception.UnauthorizedException;
import com.habitat.api.exception.ValidationException;
import com.habitat.api.repository.UserRepository;
import com.habitat.api.security.HabitatPrincipal;
import com.habitat.api.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtService jwt;
    @Mock TokenBlocklistService blocklist;
    @Mock org.springframework.context.ApplicationEventPublisher events;

    @InjectMocks AuthService authService;

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @BeforeEach
    void setUp() {
        // jwt.issueAccess and jwt.issueRefresh return small fixed values so we
        // can assert the AuthResponse shape without coupling tests to the JJWT
        // signature format.
        Instant exp = Instant.now().plusSeconds(900);
        lenient(jwt.issueAccess(any())).thenReturn(new JwtService.IssuedToken("access-token", "access-jti", exp));
        lenient(jwt.issueRefresh(any())).thenReturn(new JwtService.IssuedToken("refresh-token", "refresh-jti", exp.plusSeconds(86400)));
    }

    /** Mockito.lenient() returns the stubber — wrapper for readability. */
    @SuppressWarnings("UnusedReturnValue")
    private static <T> org.mockito.stubbing.OngoingStubbing<T> lenient(T methodCall) {
        return org.mockito.Mockito.lenient().when(methodCall);
    }

    // ── register ───────────────────────────────────────────────────────────

    @Test
    void register_persists_user_and_returns_tokens() {
        when(userRepository.existsByEmailIgnoreCase("new@example.co.za")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            // Simulate JPA assigning an ID.
            java.lang.reflect.Field idField;
            try {
                idField = com.habitat.api.entity.base.BaseEntity.class.getDeclaredField("id");
                idField.setAccessible(true);
                idField.set(u, USER_ID);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException(e);
            }
            return u;
        });

        AuthResponse out = authService.register(
                new RegisterRequest("new@example.co.za", "password123", "Sipho", "Dlamini", Role.USER, "Brixton"));

        assertThat(out.userId()).isEqualTo(USER_ID);
        assertThat(out.email()).isEqualTo("new@example.co.za");
        assertThat(out.activeRole()).isEqualTo(Role.USER);
        assertThat(out.accessToken()).isEqualTo("access-token");
        assertThat(out.refreshToken()).isEqualTo("refresh-token");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getEmail()).isEqualTo("new@example.co.za");
        assertThat(captor.getValue().getPasswordHash()).isEqualTo("hashed");
        assertThat(captor.getValue().getRoles()).containsExactly(Role.USER);
        assertThat(captor.getValue().isEmailVerified()).isFalse();

        // A UserRegisteredEvent is published exactly once for downstream
        // listeners (welcome notification, future welcome email).
        ArgumentCaptor<com.habitat.api.event.UserRegisteredEvent> eventCaptor =
                ArgumentCaptor.forClass(com.habitat.api.event.UserRegisteredEvent.class);
        verify(events).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().userId()).isEqualTo(USER_ID);
        assertThat(eventCaptor.getValue().user().getEmail()).isEqualTo("new@example.co.za");
    }

    @Test
    void register_rejects_self_assigned_admin_role() {
        // The DTO whitelist nulls the role; the service detects it and throws.
        RegisterRequest req = new RegisterRequest(
                "a@example.co.za", "password123", "Test", "User", Role.ADMIN, null);

        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(ValidationException.class)
                .hasMessage(ErrorMessages.ROLE_NOT_SELF_ASSIGNABLE);
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_rejects_duplicate_email() {
        when(userRepository.existsByEmailIgnoreCase("dup@example.co.za")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(
                new RegisterRequest("dup@example.co.za", "password123", "Test", "User", Role.USER, null)))
                .isInstanceOf(ConflictException.class)
                .hasMessage(ErrorMessages.EMAIL_ALREADY_REGISTERED);
        verify(userRepository, never()).save(any());
    }

    // ── login ──────────────────────────────────────────────────────────────

    @Test
    void login_with_correct_password_issues_tokens() {
        User user = userOf("sipho@example.co.za", "hash", Role.USER);
        when(userRepository.findByEmailIgnoreCase("sipho@example.co.za")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "hash")).thenReturn(true);

        AuthResponse out = authService.login(new LoginRequest("sipho@example.co.za", "password123"));
        assertThat(out.accessToken()).isEqualTo("access-token");
        assertThat(out.refreshToken()).isEqualTo("refresh-token");
    }

    @Test
    void login_with_unknown_email_throws_unauthorized() {
        when(userRepository.findByEmailIgnoreCase(any())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> authService.login(new LoginRequest("nobody@example.co.za", "p")))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage(ErrorMessages.INVALID_CREDENTIALS);
    }

    @Test
    void login_with_wrong_password_throws_unauthorized() {
        User user = userOf("a@example.co.za", "hash", Role.USER);
        when(userRepository.findByEmailIgnoreCase("a@example.co.za")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hash")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("a@example.co.za", "wrong")))
                .isInstanceOf(UnauthorizedException.class);
    }

    // ── refresh ────────────────────────────────────────────────────────────

    @Test
    void refresh_revokes_old_jti_and_issues_new_tokens() {
        Instant futureExp = Instant.now().plusSeconds(3600);
        HabitatPrincipal refreshPrincipal = new HabitatPrincipal(
                USER_ID, "a@example.co.za", Set.of(Role.USER), Role.USER, "old-refresh-jti", futureExp);
        when(jwt.verifyRefresh("the-refresh-token")).thenReturn(refreshPrincipal);
        when(blocklist.isRevoked("old-refresh-jti")).thenReturn(false);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(userOf("a@example.co.za", "h", Role.USER)));

        AuthResponse out = authService.refresh("the-refresh-token");

        verify(blocklist).revoke(eq("old-refresh-jti"), eq(futureExp));
        assertThat(out.accessToken()).isEqualTo("access-token");
        assertThat(out.refreshToken()).isEqualTo("refresh-token");
    }

    @Test
    void refresh_throws_when_jti_already_blocklisted() {
        Instant exp = Instant.now().plusSeconds(60);
        HabitatPrincipal p = new HabitatPrincipal(
                USER_ID, "a@example.co.za", Set.of(Role.USER), Role.USER, "revoked-jti", exp);
        when(jwt.verifyRefresh("token")).thenReturn(p);
        when(blocklist.isRevoked("revoked-jti")).thenReturn(true);

        assertThatThrownBy(() -> authService.refresh("token"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage(ErrorMessages.REFRESH_TOKEN_REVOKED);
        verify(userRepository, never()).findById(any());
    }

    @Test
    void refresh_throws_when_user_no_longer_exists() {
        Instant exp = Instant.now().plusSeconds(60);
        HabitatPrincipal p = new HabitatPrincipal(
                USER_ID, "ghost@example.co.za", Set.of(Role.USER), Role.USER, "jti", exp);
        when(jwt.verifyRefresh(any())).thenReturn(p);
        when(blocklist.isRevoked(any())).thenReturn(false);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refresh("t"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── logout ─────────────────────────────────────────────────────────────

    @Test
    void logout_revokes_access_and_refresh_jtis() {
        Instant accessExp = Instant.now().plusSeconds(900);
        Instant refreshExp = Instant.now().plusSeconds(86400);
        HabitatPrincipal accessP = new HabitatPrincipal(
                USER_ID, "a@example.co.za", Set.of(Role.USER), Role.USER, "access-jti", accessExp);
        HabitatPrincipal refreshP = new HabitatPrincipal(
                USER_ID, "a@example.co.za", Set.of(Role.USER), Role.USER, "refresh-jti", refreshExp);
        when(jwt.verifyRefresh("refresh-token")).thenReturn(refreshP);

        authService.logout(accessP, "refresh-token");

        verify(blocklist).revoke("access-jti", accessExp);
        verify(blocklist).revoke("refresh-jti", refreshExp);
    }

    @Test
    void logout_with_no_refresh_token_only_revokes_access() {
        Instant accessExp = Instant.now().plusSeconds(900);
        HabitatPrincipal accessP = new HabitatPrincipal(
                USER_ID, "a@example.co.za", Set.of(Role.USER), Role.USER, "access-jti", accessExp);

        authService.logout(accessP, null);

        verify(blocklist).revoke("access-jti", accessExp);
        verify(jwt, never()).verifyRefresh(any());
    }

    @Test
    void logout_swallows_invalid_refresh_token() {
        Instant accessExp = Instant.now().plusSeconds(900);
        HabitatPrincipal accessP = new HabitatPrincipal(
                USER_ID, "a@example.co.za", Set.of(Role.USER), Role.USER, "access-jti", accessExp);
        when(jwt.verifyRefresh("garbage")).thenThrow(new UnauthorizedException("nope"));

        // Should not throw — partial logout is OK.
        authService.logout(accessP, "garbage");

        verify(blocklist).revoke("access-jti", accessExp);
    }

    @Test
    void logout_with_null_principal_is_a_noop() {
        authService.logout(null, null);
        // Should not throw.
    }

    // ── helpers ────────────────────────────────────────────────────────────

    private static User userOf(String email, String hash, Role role) {
        User u = User.builder()
                .email(email)
                .passwordHash(hash)
                .firstName("Test")
                .surname("User")
                .roles(new java.util.HashSet<>(Set.of(role)))
                .activeRole(role)
                .emailVerified(true)
                .build();
        try {
            java.lang.reflect.Field idField =
                    com.habitat.api.entity.base.BaseEntity.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(u, USER_ID);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
        return u;
    }
}
