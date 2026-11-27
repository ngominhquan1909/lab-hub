package com.labhub.server.service;

import com.labhub.common.dto.UserDto;
import com.labhub.server.domain.User;
import com.labhub.server.repository.UserRepository;
import com.labhub.server.security.PasswordHasher;
import com.labhub.server.security.SessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.Optional;

/**
 * Business logic for authentication operations.
 *
 * <p>Security invariants maintained here:
 * <ul>
 *   <li>Plaintext passwords are never logged.</li>
 *   <li>BCrypt verification uses constant-time comparison.</li>
 *   <li>Login failures return a generic message (no username enumeration).</li>
 *   <li>Disabled accounts are rejected after password verification
 *       (prevents timing-based account enumeration via short-circuit).</li>
 * </ul>
 */
public class AuthService {

    private static final Logger LOG = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final SessionManager sessionManager;

    public AuthService(UserRepository userRepository, SessionManager sessionManager) {
        this.userRepository = userRepository;
        this.sessionManager = sessionManager;
    }

    /**
     * Authenticates a user and returns a session token on success.
     *
     * @param username  the submitted login name
     * @param plaintext the submitted password (never stored or logged)
     * @return login result
     */
    public LoginResult login(String username, String plaintext) {
        // Basic input validation
        if (username == null || username.isBlank()) {
            return LoginResult.failure("Username is required.");
        }
        if (plaintext == null || plaintext.isEmpty()) {
            return LoginResult.failure("Password is required.");
        }

        // Look up user
        Optional<User> maybeUser = userRepository.findByUsername(username.trim().toLowerCase());
        if (maybeUser.isEmpty()) {
            // Always verify a dummy hash to prevent timing attacks revealing valid usernames
            PasswordHasher.verify(plaintext, "$2a$12$invalidHashToPreventTimingAttacks....");
            LOG.info("Login failed: unknown username '{}'", username);
            return LoginResult.failure("Invalid username or password.");
        }

        User user = maybeUser.get();

        // Verify password (BCrypt constant-time)
        if (!PasswordHasher.verify(plaintext, user.getPasswordHash())) {
            LOG.info("Login failed: wrong password for user '{}'", username);
            return LoginResult.failure("Invalid username or password.");
        }

        // Check account is enabled
        if (!user.isActive()) {
            LOG.info("Login blocked: account '{}' is disabled", username);
            return LoginResult.failure("Your account has been disabled. Contact an administrator.");
        }

        // Success - create session, record login timestamp
        UserDto userDto = user.toDto();
        String token = sessionManager.createSession(userDto);
        userRepository.recordLogin(user.getId());
        LOG.info("User '{}' (id={}) logged in successfully.", username, user.getId());
        return LoginResult.success(token, userDto);
    }

    /**
     * Invalidates a session token (logout).
     *
     * @param token session token to invalidate
     */
    public void logout(String token) {
        sessionManager.invalidate(token);
    }

    /**
     * Changes a user's password after verifying the current one.
     *
     * @param userId       the user changing their password
     * @param currentPlain the current plaintext password for verification
     * @param newPlain     the new plaintext password
     * @return change result
     */
    public ChangePasswordResult changePassword(long userId, String currentPlain, String newPlain) {
        if (newPlain == null || newPlain.length() < 8) {
            return ChangePasswordResult.failure("New password must be at least 8 characters.");
        }

        Optional<User> maybeUser = userRepository.findById(userId);
        if (maybeUser.isEmpty()) {
            return ChangePasswordResult.failure("User not found.");
        }

        User user = maybeUser.get();
        if (!PasswordHasher.verify(currentPlain, user.getPasswordHash())) {
            return ChangePasswordResult.failure("Current password is incorrect.");
        }

        String newHash = PasswordHasher.hash(newPlain);
        try {
            userRepository.updatePassword(userId, newHash);
            LOG.info("Password changed for user id={}", userId);
            return ChangePasswordResult.success();
        } catch (SQLException e) {
            LOG.error("Failed to update password for user {}: {}", userId, e.getMessage(), e);
            return ChangePasswordResult.failure("Failed to update password. Please try again.");
        }
    }

    /**
     * Result of a login attempt.
     */
    public static final class LoginResult {

        private final boolean success;
        private final String token;
        private final UserDto user;
        private final String errorMessage;

        private LoginResult(boolean success, String token, UserDto user, String errorMessage) {
            this.success = success;
            this.token = token;
            this.user = user;
            this.errorMessage = errorMessage;
        }

        static LoginResult success(String token, UserDto user) {
            return new LoginResult(true, token, user, null);
        }

        static LoginResult failure(String message) {
            return new LoginResult(false, null, null, message);
        }

        public boolean isSuccess() { return success; }

        public String getToken() { return token; }

        public UserDto getUser() { return user; }

        public String getErrorMessage() { return errorMessage; }
    }

    /**
     * Result of a change-password operation.
     */
    public static final class ChangePasswordResult {

        private final boolean success;
        private final String errorMessage;

        private ChangePasswordResult(boolean success, String errorMessage) {
            this.success = success;
            this.errorMessage = errorMessage;
        }

        static ChangePasswordResult success() {
            return new ChangePasswordResult(true, null);
        }

        static ChangePasswordResult failure(String message) {
            return new ChangePasswordResult(false, message);
        }

        public boolean isSuccess() { return success; }

        public String getErrorMessage() { return errorMessage; }
    }
}
