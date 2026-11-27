package com.labhub.server.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.labhub.common.dto.UserDto;
import com.labhub.common.protocol.Request;
import com.labhub.common.protocol.Response;
import com.labhub.server.service.AuthService;

/**
 * Handles authentication-related requests: LOGIN, LOGOUT, CHANGE_PASSWORD.
 */
public class AuthHandler extends BaseHandler {

    private final AuthService authService;

    public AuthHandler(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public Response handle(Request request, UserDto currentUser) {
        return switch (request.getType()) {
            case LOGIN         -> handleLogin(request);
            case LOGOUT        -> handleLogout(request);
            case CHANGE_PASSWORD -> handleChangePassword(request, currentUser);
            default -> Response.error(request.getRequestId(), "INVALID_REQUEST",
                    "AuthHandler does not handle " + request.getType());
        };
    }

    private Response handleLogin(Request request) {
        JsonNode payload = request.getPayload();
        String username = textOrNull(payload, "username");
        String password = textOrNull(payload, "password");

        if (username == null || password == null) {
            return Response.error(request.getRequestId(), "VALIDATION_ERROR",
                    "Username and password are required.");
        }

        AuthService.LoginResult result = authService.login(username, password);
        if (!result.isSuccess()) {
            return Response.error(request.getRequestId(), "UNAUTHORIZED", result.getErrorMessage());
        }

        // Build response payload: token + user DTO
        ObjectNode responsePayload = MAPPER.createObjectNode();
        responsePayload.put("token", result.getToken());
        responsePayload.set("user", toJsonNode(result.getUser()));

        return Response.ok(request.getRequestId(),
                "Welcome, " + result.getUser().getFullName() + ".",
                responsePayload);
    }

    private Response handleLogout(Request request) {
        authService.logout(request.getToken());
        return Response.ok(request.getRequestId(), "You have been logged out.");
    }

    private Response handleChangePassword(Request request, UserDto currentUser) {
        JsonNode payload = request.getPayload();
        if (payload == null) {
            return missingPayload(request.getRequestId());
        }
        String currentPassword = textOrNull(payload, "currentPassword");
        String newPassword     = textOrNull(payload, "newPassword");

        if (currentPassword == null || newPassword == null) {
            return Response.error(request.getRequestId(), "VALIDATION_ERROR",
                    "Both currentPassword and newPassword are required.");
        }

        AuthService.ChangePasswordResult result = authService.changePassword(
                currentUser.getId(), currentPassword, newPassword);

        if (!result.isSuccess()) {
            return Response.error(request.getRequestId(), "VALIDATION_ERROR", result.getErrorMessage());
        }
        return Response.ok(request.getRequestId(), "Password changed successfully.");
    }
}
