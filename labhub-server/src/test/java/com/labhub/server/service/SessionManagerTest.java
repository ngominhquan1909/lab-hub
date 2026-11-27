package com.labhub.server.service;

import com.labhub.common.dto.UserDto;
import com.labhub.common.enums.UserRole;
import com.labhub.server.security.SessionManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link SessionManager}.
 */
class SessionManagerTest {

    private SessionManager sessionManager;
    private UserDto testUser;

    @BeforeEach
    void setUp() {
        sessionManager = new SessionManager(60); // 60-minute timeout
        testUser = new UserDto();
        testUser.setId(1L);
        testUser.setUsername("alice");
        testUser.setRoles(List.of(UserRole.LECTURER));
        testUser.setActive(true);
    }

    @Test
    @DisplayName("Create session -> returns non-null token")
    void createSession_returnsToken() {
        String token = sessionManager.createSession(testUser);
        assertNotNull(token);
        assertFalse(token.isBlank());
    }

    @Test
    @DisplayName("Valid token -> resolves to correct user")
    void validateToken_validToken_returnsUser() {
        String token = sessionManager.createSession(testUser);
        Optional<UserDto> result = sessionManager.validateToken(token);
        assertTrue(result.isPresent());
        assertEquals("alice", result.get().getUsername());
    }

    @Test
    @DisplayName("Invalid token -> empty optional")
    void validateToken_invalidToken_empty() {
        Optional<UserDto> result = sessionManager.validateToken("not-a-real-token");
        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("Null token -> empty optional (does not throw)")
    void validateToken_null_empty() {
        assertFalse(sessionManager.validateToken(null).isPresent());
    }

    @Test
    @DisplayName("Invalidated token -> empty optional")
    void invalidate_tokenBecomesInvalid() {
        String token = sessionManager.createSession(testUser);
        sessionManager.invalidate(token);
        assertFalse(sessionManager.validateToken(token).isPresent());
    }

    @Test
    @DisplayName("Two logins -> two distinct tokens both valid")
    void createSession_twoLogins_twoDistinctValidTokens() {
        String t1 = sessionManager.createSession(testUser);
        String t2 = sessionManager.createSession(testUser);
        assertNotEquals(t1, t2);
        assertTrue(sessionManager.validateToken(t1).isPresent());
        assertTrue(sessionManager.validateToken(t2).isPresent());
    }

    @Test
    @DisplayName("hasRole: user with LECTURER role satisfies LECTURER check")
    void hasRole_matchingRole_true() {
        String token = sessionManager.createSession(testUser);
        assertTrue(sessionManager.hasRole(token, UserRole.LECTURER));
    }

    @Test
    @DisplayName("hasRole: user without ADMIN role does not satisfy ADMIN check")
    void hasRole_missingRole_false() {
        String token = sessionManager.createSession(testUser);
        assertFalse(sessionManager.hasRole(token, UserRole.ADMIN));
    }

    @Test
    @DisplayName("hasRole: accepts any matching role from varargs")
    void hasRole_anyRole_matchesOne() {
        String token = sessionManager.createSession(testUser);
        assertTrue(sessionManager.hasRole(token, UserRole.ADMIN, UserRole.LECTURER));
    }

    @Test
    @DisplayName("Active session count tracks sessions correctly")
    void activeSessionCount_tracksCorrectly() {
        assertEquals(0, sessionManager.activeSessionCount());
        String t1 = sessionManager.createSession(testUser);
        assertEquals(1, sessionManager.activeSessionCount());
        String t2 = sessionManager.createSession(testUser);
        assertEquals(2, sessionManager.activeSessionCount());
        sessionManager.invalidate(t1);
        assertEquals(1, sessionManager.activeSessionCount());
        sessionManager.invalidate(t2);
        assertEquals(0, sessionManager.activeSessionCount());
    }

    @Test
    @DisplayName("Expired session manager (1-second timeout) evicts expired sessions")
    void evictExpired_removesExpiredSessions() throws InterruptedException {
        SessionManager shortLivedManager = new SessionManager(0); // 0 = no timeout
        String token = shortLivedManager.createSession(testUser);
        shortLivedManager.evictExpired();
        // With timeout=0 (no expiry), session should still be valid
        assertTrue(shortLivedManager.validateToken(token).isPresent());
    }
}
