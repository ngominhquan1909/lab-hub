package com.labhub.server.security;

import com.labhub.common.dto.UserDto;
import com.labhub.common.enums.UserRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages user sessions using secure random tokens.
 *
 * <p>Sessions are stored in memory. All operations are thread-safe.
 * Expired sessions are removed lazily on access and also via a periodic cleanup
 * called from the main server thread.
 *
 * <p>Tokens are UUID v4 strings - 122 bits of randomness; not guessable.
 * Database credentials and password hashes are never stored here.
 */
public final class SessionManager {

    private static final Logger LOG = LoggerFactory.getLogger(SessionManager.class);

    private final Map<String, Session> sessions = new ConcurrentHashMap<>();
    private final int timeoutMinutes;

    public SessionManager(int timeoutMinutes) {
        this.timeoutMinutes = timeoutMinutes;
    }

    /**
     * Creates a new session for the authenticated user and returns the token.
     * Any previous session for the same user is NOT invalidated
     * (allows multiple concurrent clients).
     *
     * @param user the authenticated user DTO (no password in this DTO)
     * @return opaque session token
     */
    public String createSession(UserDto user) {
        String token = UUID.randomUUID().toString();
        sessions.put(token, new Session(token, user, Instant.now()));
        LOG.debug("Session created for user '{}' (id={})", user.getUsername(), user.getId());
        return token;
    }

    /**
     * Validates a token and returns the associated user if the session is active.
     * Updates the last-activity timestamp on success.
     *
     * @param token session token from the request
     * @return the user DTO, or empty if the token is unknown/expired
     */
    public Optional<UserDto> validateToken(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        Session session = sessions.get(token);
        if (session == null) {
            return Optional.empty();
        }
        if (isExpired(session)) {
            sessions.remove(token);
            LOG.debug("Token expired and removed.");
            return Optional.empty();
        }
        // Refresh last-activity
        session.touch();
        return Optional.of(session.user());
    }

    /**
     * Invalidates a session token (logout).
     *
     * @param token session token to invalidate
     */
    public void invalidate(String token) {
        Session removed = sessions.remove(token);
        if (removed != null) {
            LOG.debug("Session invalidated for user '{}'", removed.user().getUsername());
        }
    }

    /**
     * Removes all expired sessions. Call periodically from a maintenance task.
     */
    public void evictExpired() {
        long before = sessions.size();
        sessions.entrySet().removeIf(e -> isExpired(e.getValue()));
        long removed = before - sessions.size();
        if (removed > 0) {
            LOG.debug("Evicted {} expired sessions.", removed);
        }
    }

    /**
     * Returns the number of active sessions.
     *
     * @return session count
     */
    public int activeSessionCount() {
        return sessions.size();
    }

    /**
     * Returns true if the user associated with the token has at least one of the given roles.
     *
     * @param token         session token
     * @param requiredRoles one or more acceptable roles
     * @return true if authorized
     */
    public boolean hasRole(String token, UserRole... requiredRoles) {
        return validateToken(token)
                .map(user -> {
                    List<UserRole> userRoles = user.getRoles();
                    if (userRoles == null) {
                        return false;
                    }
                    for (UserRole r : requiredRoles) {
                        if (userRoles.contains(r)) {
                            return true;
                        }
                    }
                    return false;
                })
                .orElse(false);
    }

    private boolean isExpired(Session session) {
        if (timeoutMinutes <= 0) {
            return false;
        }
        Instant expiry = session.lastActivity().plusSeconds((long) timeoutMinutes * 60);
        return Instant.now().isAfter(expiry);
    }

    /**
     * Internal session record. Package-private for testability.
     */
    static final class Session {

        private final String token;
        private final UserDto user;
        private volatile Instant lastActivity;

        Session(String token, UserDto user, Instant lastActivity) {
            this.token = token;
            this.user = user;
            this.lastActivity = lastActivity;
        }

        String token() { return token; }

        UserDto user() { return user; }

        Instant lastActivity() { return lastActivity; }

        /** Updates last activity to now. */
        void touch() { this.lastActivity = Instant.now(); }
    }
}
