package com.labhub.server.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.labhub.common.dto.PagedResult;
import com.labhub.common.dto.UserDto;
import com.labhub.common.enums.UserRole;
import com.labhub.common.protocol.Request;
import com.labhub.common.protocol.Response;
import com.labhub.server.domain.User;
import com.labhub.server.repository.DashboardRepository;
import com.labhub.server.repository.UserRepository;
import com.labhub.server.security.PasswordHasher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Handles USER_LIST, USER_GET, USER_CREATE, USER_UPDATE, USER_DISABLE, USER_ENABLE.
 * All operations require ADMIN role.
 */
public class UserHandler extends BaseHandler {

    private static final Logger LOG = LoggerFactory.getLogger(UserHandler.class);

    private final UserRepository userRepository;
    private final DashboardRepository dashboardRepository;

    /**
     * @param userRepository      user data access
     * @param dashboardRepository audit logging
     */
    public UserHandler(UserRepository userRepository, DashboardRepository dashboardRepository) {
        this.userRepository = userRepository;
        this.dashboardRepository = dashboardRepository;
    }

    @Override
    public Response handle(Request request, UserDto currentUser) {
        // All user management requires ADMIN
        Response authError = requireRole(currentUser, request.getRequestId(), UserRole.ADMIN);
        if (authError != null) {
            return authError;
        }
        return switch (request.getType()) {
            case USER_LIST    -> handleList(request);
            case USER_GET     -> handleGet(request);
            case USER_CREATE  -> handleCreate(request, currentUser);
            case USER_UPDATE  -> handleUpdate(request, currentUser);
            case USER_DISABLE -> handleSetActive(request, currentUser, false);
            case USER_ENABLE  -> handleSetActive(request, currentUser, true);
            default -> Response.error(request.getRequestId(), "INVALID_REQUEST",
                    "UserHandler does not handle " + request.getType());
        };
    }

    // ── Handlers ──────────────────────────────────────────────────────────

    private Response handleList(Request request) {
        int page     = intOrDefault(request.getPayload(), "page",     1);
        int pageSize = intOrDefault(request.getPayload(), "pageSize", 20);
        String search = textOrNull(request.getPayload(), "search");
        PagedResult<UserDto> result = userRepository.findAll(search, page, pageSize);
        return Response.ok(request.getRequestId(), "Users loaded.", toJsonNode(result));
    }

    private Response handleGet(Request request) {
        long id = longOrDefault(request.getPayload(), "id", 0);
        if (id <= 0) {
            return Response.error(request.getRequestId(), "VALIDATION_ERROR", "User ID required.");
        }
        Optional<User> user = userRepository.findById(id);
        return user.map(u -> Response.ok(request.getRequestId(), "OK", toJsonNode(u.toDto())))
                   .orElseGet(() -> Response.error(request.getRequestId(), "NOT_FOUND", "User not found."));
    }

    private Response handleCreate(Request request, UserDto currentUser) {
        JsonNode p = request.getPayload();
        if (p == null) {
            return missingPayload(request.getRequestId());
        }
        String username  = textOrNull(p, "username");
        String email     = textOrNull(p, "email");
        String fullName  = textOrNull(p, "fullName");
        String password  = textOrNull(p, "password");
        String phone     = textOrNull(p, "phoneNumber");

        // Basic field validation
        if (username == null || email == null || fullName == null || password == null) {
            return Response.error(request.getRequestId(), "VALIDATION_ERROR",
                    "Username, email, full name, and password are required.");
        }
        if (password.length() < 8) {
            return Response.error(request.getRequestId(), "VALIDATION_ERROR",
                    "Password must be at least 8 characters.");
        }
        if (!email.contains("@")) {
            return Response.error(request.getRequestId(), "VALIDATION_ERROR",
                    "Invalid email address.");
        }

        // Parse roles
        List<UserRole> roles = parseRoles(p);
        if (roles.isEmpty()) {
            roles.add(UserRole.STUDENT); // default role
        }

        User user = new User();
        user.setUsername(username.toLowerCase().trim());
        user.setEmail(email.toLowerCase().trim());
        user.setFullName(fullName.trim());
        user.setPhoneNumber(phone);
        user.setPasswordHash(PasswordHasher.hash(password));

        try {
            long newId = userRepository.create(user, roles);
            dashboardRepository.log(currentUser.getId(), "USER_CREATED", "USER", newId,
                    "Created user: " + username + " with roles " + roles);
            return Response.ok(request.getRequestId(), "User account created.",
                    toJsonNode(userRepository.findById(newId).orElseThrow().toDto()));
        } catch (SQLException e) {
            if (e.getMessage() != null && e.getMessage().contains("Duplicate")) {
                return Response.error(request.getRequestId(), "CONFLICT",
                        "Username or email already exists.");
            }
            LOG.error("Failed to create user: {}", e.getMessage(), e);
            return Response.error(request.getRequestId(), "DATABASE_ERROR", "Failed to create user.");
        }
    }

    private Response handleUpdate(Request request, UserDto currentUser) {
        JsonNode p = request.getPayload();
        if (p == null || !p.has("id")) {
            return missingPayload(request.getRequestId());
        }
        long id = p.get("id").asLong();
        String email    = textOrNull(p, "email");
        String fullName = textOrNull(p, "fullName");
        String phone    = textOrNull(p, "phoneNumber");
        List<UserRole> roles = parseRoles(p);

        if (userRepository.findById(id).isEmpty()) {
            return Response.error(request.getRequestId(), "NOT_FOUND", "User not found.");
        }
        try {
            userRepository.update(id, email, fullName, phone, roles.isEmpty() ? null : roles);
            dashboardRepository.log(currentUser.getId(), "USER_UPDATED", "USER", id, null);
            return Response.ok(request.getRequestId(), "User updated.",
                    toJsonNode(userRepository.findById(id).orElseThrow().toDto()));
        } catch (SQLException e) {
            LOG.error("Failed to update user {}: {}", id, e.getMessage(), e);
            return Response.error(request.getRequestId(), "DATABASE_ERROR", "Failed to update user.");
        }
    }

    private Response handleSetActive(Request request, UserDto currentUser, boolean active) {
        long id = longOrDefault(request.getPayload(), "id", 0);
        if (id <= 0) {
            return Response.error(request.getRequestId(), "VALIDATION_ERROR", "User ID required.");
        }
        // Prevent self-disable
        if (id == currentUser.getId() && !active) {
            return Response.error(request.getRequestId(), "FORBIDDEN",
                    "You cannot disable your own account.");
        }
        try {
            userRepository.setActive(id, active);
            dashboardRepository.log(currentUser.getId(),
                    active ? "USER_ENABLED" : "USER_DISABLED", "USER", id, null);
            return Response.ok(request.getRequestId(),
                    "User account " + (active ? "enabled" : "disabled") + ".");
        } catch (SQLException e) {
            LOG.error("Failed to set active={} for user {}: {}", active, id, e.getMessage(), e);
            return Response.error(request.getRequestId(), "DATABASE_ERROR",
                    "Failed to update user status.");
        }
    }

    // ── Helper ────────────────────────────────────────────────────────────

    private List<UserRole> parseRoles(JsonNode payload) {
        List<UserRole> roles = new ArrayList<>();
        if (payload != null && payload.has("roles") && payload.get("roles").isArray()) {
            for (JsonNode roleNode : payload.get("roles")) {
                try {
                    roles.add(UserRole.valueOf(roleNode.asText()));
                } catch (IllegalArgumentException ignored) { }
            }
        }
        return roles;
    }
}
