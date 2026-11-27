package com.labhub.server.service;

import com.labhub.common.dto.UserDto;
import com.labhub.common.enums.UserRole;
import com.labhub.server.domain.User;
import com.labhub.server.repository.UserRepository;
import com.labhub.server.security.PasswordHasher;
import com.labhub.server.security.SessionManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link AuthService}.
 * Uses hand-written fakes - no Mockito dependency required.
 */
class AuthServiceTest {

    // Fakes

    private static class FakeUserRepository extends UserRepository {
        private final User storedUser;

        FakeUserRepository(User user) {
            super(null);
            this.storedUser = user;
        }

        @Override
        public Optional<User> findByUsername(String username) {
            return storedUser != null && storedUser.getUsername().equals(username)
                    ? Optional.of(storedUser) : Optional.empty();
        }

        @Override
        public Optional<User> findById(long id) {
            return storedUser != null && storedUser.getId() == id
                    ? Optional.of(storedUser) : Optional.empty();
        }

        @Override
        public void recordLogin(long id) { /* no-op */ }

        @Override
        public void updatePassword(long id, String newHashedPwd) throws SQLException { /* no-op */ }
    }

    // Setup

    private static final String PLAIN_PASSWORD = "correct-password-123";
    private User testUser;
    private AuthService authService;
    private SessionManager sessionManager;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setFullName("Test User");
        testUser.setPasswordHash(PasswordHasher.hash(PLAIN_PASSWORD));
        testUser.setRoles(List.of(UserRole.STUDENT));
        testUser.setActive(true);

        sessionManager = new SessionManager(60);
        authService = new AuthService(new FakeUserRepository(testUser), sessionManager);
    }

    // Login tests

    @Test
    @DisplayName("Valid credentials -> successful login with token")
    void login_validCredentials_returnsToken() {
        AuthService.LoginResult result = authService.login("testuser", PLAIN_PASSWORD);
        assertTrue(result.isSuccess());
        assertNotNull(result.getToken());
        assertNotNull(result.getUser());
        assertEquals("testuser", result.getUser().getUsername());
    }

    @Test
    @DisplayName("Wrong password -> login fails with generic message")
    void login_wrongPassword_fails() {
        AuthService.LoginResult result = authService.login("testuser", "wrong-password");
        assertFalse(result.isSuccess());
        // Generic message - no distinction between wrong user/wrong password
        assertEquals("Invalid username or password.", result.getErrorMessage());
    }

    @Test
    @DisplayName("Unknown username -> login fails")
    void login_unknownUsername_fails() {
        AuthService.LoginResult result = authService.login("nobody", PLAIN_PASSWORD);
        assertFalse(result.isSuccess());
        assertEquals("Invalid username or password.", result.getErrorMessage());
    }

    @Test
    @DisplayName("Blank username -> validation error")
    void login_blankUsername_fails() {
        AuthService.LoginResult result = authService.login("  ", PLAIN_PASSWORD);
        assertFalse(result.isSuccess());
        assertTrue(result.getErrorMessage().contains("Username"));
    }

    @Test
    @DisplayName("Blank password -> validation error")
    void login_blankPassword_fails() {
        AuthService.LoginResult result = authService.login("testuser", "");
        assertFalse(result.isSuccess());
        assertTrue(result.getErrorMessage().contains("Password"));
    }

    @Test
    @DisplayName("Disabled account -> access denied even with correct password")
    void login_disabledAccount_denied() {
        testUser.setActive(false);
        AuthService.LoginResult result = authService.login("testuser", PLAIN_PASSWORD);
        assertFalse(result.isSuccess());
        assertTrue(result.getErrorMessage().contains("disabled"));
    }

    // Logout tests

    @Test
    @DisplayName("Logout invalidates the session token")
    void logout_invalidatesToken() {
        AuthService.LoginResult login = authService.login("testuser", PLAIN_PASSWORD);
        String token = login.getToken();
        assertTrue(sessionManager.validateToken(token).isPresent());

        authService.logout(token);
        assertFalse(sessionManager.validateToken(token).isPresent());
    }

    @Test
    @DisplayName("Logout with invalid token is a no-op (does not throw)")
    void logout_invalidToken_noOp() {
        assertDoesNotThrow(() -> authService.logout("non-existent-token"));
    }

    // Change password tests

    @Test
    @DisplayName("Change password with correct current password -> success")
    void changePassword_correctCurrent_succeeds() {
        AuthService.ChangePasswordResult result = authService.changePassword(
                1L, PLAIN_PASSWORD, "new-secure-password-456");
        assertTrue(result.isSuccess());
    }

    @Test
    @DisplayName("Change password with wrong current -> fails")
    void changePassword_wrongCurrent_fails() {
        AuthService.ChangePasswordResult result = authService.changePassword(
                1L, "wrong-current", "new-password-456");
        assertFalse(result.isSuccess());
        assertTrue(result.getErrorMessage().contains("incorrect"));
    }

    @Test
    @DisplayName("New password too short -> validation error")
    void changePassword_tooShort_fails() {
        AuthService.ChangePasswordResult result = authService.changePassword(
                1L, PLAIN_PASSWORD, "short");
        assertFalse(result.isSuccess());
        assertTrue(result.getErrorMessage().contains("8 characters"));
    }

    // Session token tests

    @Test
    @DisplayName("Session token is unique across concurrent logins")
    void login_concurrentLogins_uniqueTokens() {
        AuthService.LoginResult r1 = authService.login("testuser", PLAIN_PASSWORD);
        AuthService.LoginResult r2 = authService.login("testuser", PLAIN_PASSWORD);
        assertTrue(r1.isSuccess());
        assertTrue(r2.isSuccess());
        assertNotEquals(r1.getToken(), r2.getToken());
    }
}
