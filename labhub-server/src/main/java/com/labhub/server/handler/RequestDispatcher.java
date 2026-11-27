package com.labhub.server.handler;

import com.labhub.common.dto.UserDto;
import com.labhub.common.enums.UserRole;
import com.labhub.common.protocol.Request;
import com.labhub.common.protocol.RequestType;
import com.labhub.common.protocol.Response;
import com.labhub.server.security.SessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/**
 * Routes incoming {@link Request} objects to the appropriate {@link RequestHandler}.
 *
 * <p>All requests except {@code LOGIN} require a valid session token.
 * Authorization (role check) is delegated to each handler, but authentication
 * (token validity) is enforced here, centrally.
 *
 * <p>Handlers are registered at construction time and are immutable after that.
 */
public final class RequestDispatcher {

    private static final Logger LOG = LoggerFactory.getLogger(RequestDispatcher.class);

    private final Map<RequestType, RequestHandler> handlers = new EnumMap<>(RequestType.class);
    private final SessionManager sessionManager;

    public RequestDispatcher(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    /**
     * Registers a handler for a request type.
     * Registering the same type twice replaces the previous handler (last-write-wins).
     *
     * @param type    the request type this handler processes
     * @param handler the handler implementation
     */
    public void register(RequestType type, RequestHandler handler) {
        handlers.put(type, handler);
        LOG.debug("Registered handler for {}: {}", type, handler.getClass().getSimpleName());
    }

    /**
     * Validates the request, authenticates the session, and dispatches to the handler.
     *
     * @param request incoming request (never null)
     * @return response - always non-null
     */
    public Response dispatch(Request request) {
        // 1. Basic structural validation
        if (request.getType() == null) {
            return Response.error(request.getRequestId(), "INVALID_REQUEST",
                    "Request type is required.");
        }

        // 2. Authentication - LOGIN is the only unauthenticated endpoint
        UserDto currentUser = null;
        if (request.getType() != RequestType.LOGIN) {
            Optional<UserDto> sessionUser = sessionManager.validateToken(request.getToken());
            if (sessionUser.isEmpty()) {
                return Response.error(request.getRequestId(), "UNAUTHORIZED",
                        "Session expired or invalid. Please log in again.");
            }
            currentUser = sessionUser.get();
        }

        // 3. Find handler
        RequestHandler handler = handlers.get(request.getType());
        if (handler == null) {
            LOG.warn("No handler registered for request type: {}", request.getType());
            return Response.error(request.getRequestId(), "NOT_IMPLEMENTED",
                    "This feature is not yet available.");
        }

        // 4. Delegate - handler is responsible for its own authorization
        try {
            return handler.handle(request, currentUser);
        } catch (Exception e) {
            LOG.error("Handler {} threw an unexpected exception for request {}: {}",
                    handler.getClass().getSimpleName(), request.getRequestId(), e.getMessage(), e);
            return Response.error(request.getRequestId(), "INTERNAL_ERROR",
                    "An unexpected error occurred while processing your request.");
        }
    }

    /**
     * Returns true if the user has at least one of the specified roles.
     *
     * @param user  the authenticated user
     * @param roles acceptable roles
     * @return true if authorized
     */
    public static boolean hasAnyRole(UserDto user, UserRole... roles) {
        if (user == null || user.getRoles() == null) {
            return false;
        }
        for (UserRole role : roles) {
            if (user.getRoles().contains(role)) {
                return true;
            }
        }
        return false;
    }
}
