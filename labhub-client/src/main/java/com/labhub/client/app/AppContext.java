package com.labhub.client.app;

import com.labhub.common.dto.UserDto;
import com.labhub.common.enums.UserRole;
import com.labhub.client.network.ApiClient;
import com.labhub.client.network.ServerConnection;

/**
 * Application-wide singleton holding the current session state.
 *
 * <p>Holds the authenticated user and the shared API client so all controllers
 * can access them without passing them through FXML constructors.
 *
 * <p>Thread safety: setters are called from the JavaFX Application Thread.
 * Reads from background tasks are safe for immutable fields (user DTO).
 */
public final class AppContext {

    private static final AppContext INSTANCE = new AppContext();

    private UserDto currentUser;
    private ApiClient apiClient;
    private ServerConnection connection;

    private AppContext() { }

    /**
     * Returns the singleton application context.
     *
     * @return application context
     */
    public static AppContext get() {
        return INSTANCE;
    }

    // ── Session ───────────────────────────────────────────────────────────

    /**
     * Returns the authenticated user, or null if not logged in.
     *
     * @return current user DTO
     */
    public UserDto getCurrentUser() { return currentUser; }

    /**
     * Sets the authenticated user after a successful login.
     *
     * @param user authenticated user DTO
     */
    public void setCurrentUser(UserDto user) { this.currentUser = user; }

    /** @return true if a user is logged in */
    public boolean isLoggedIn() { return currentUser != null; }

    /**
     * Clears all session state (called on logout).
     */
    public void clearSession() {
        currentUser = null;
    }

    // ── Authorization helpers ─────────────────────────────────────────────

    /**
     * Returns true if the current user has the ADMIN role.
     *
     * @return true if admin
     */
    public boolean isAdmin() {
        return hasRole(UserRole.ADMIN);
    }

    /**
     * Returns true if the current user is a lab manager (or admin).
     *
     * @return true if lab manager or admin
     */
    public boolean isManager() {
        return hasRole(UserRole.ADMIN) || hasRole(UserRole.LAB_MANAGER);
    }

    /**
     * Returns true if the current user has the specified role.
     *
     * @param role role to check
     * @return true if user has the role
     */
    public boolean hasRole(UserRole role) {
        return currentUser != null && currentUser.getRoles() != null
                && currentUser.getRoles().contains(role);
    }

    // ── Networking ────────────────────────────────────────────────────────

    /**
     * Returns the shared API client.
     *
     * @return API client
     */
    public ApiClient getApiClient() { return apiClient; }

    /**
     * Sets the API client after connecting to the server.
     *
     * @param client API client
     */
    public void setApiClient(ApiClient client) { this.apiClient = client; }

    /**
     * Returns the raw server connection (used during connect/disconnect).
     *
     * @return server connection
     */
    public ServerConnection getConnection() { return connection; }

    /**
     * Sets the server connection.
     *
     * @param connection server connection
     */
    public void setConnection(ServerConnection connection) { this.connection = connection; }
}
