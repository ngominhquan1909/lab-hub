package com.labhub.server.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.labhub.common.dto.UserDto;
import com.labhub.common.enums.UserRole;
import com.labhub.common.protocol.JsonMapper;
import com.labhub.common.protocol.Request;
import com.labhub.common.protocol.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract base class providing shared utilities for concrete handler implementations.
 *
 * <p>All concrete handlers should extend this class rather than implementing
 * {@link RequestHandler} directly, to avoid duplicating boilerplate for payload
 * parsing and authorization checks.
 */
public abstract class BaseHandler implements RequestHandler {

    /** Shared Jackson mapper - thread-safe. */
    protected static final ObjectMapper MAPPER = JsonMapper.get();

    private static final Logger LOG = LoggerFactory.getLogger(BaseHandler.class);

    /**
     * Returns a FORBIDDEN response if {@code user} does not have at least one of {@code roles}.
     * Returns {@code null} if the user is authorized (caller should proceed).
     *
     * <p>Usage pattern:
     * <pre>{@code
     * Response authError = requireRole(currentUser, request.getRequestId(), ADMIN, LAB_MANAGER);
     * if (authError != null) return authError;
     * }</pre>
     *
     * @param user      authenticated user
     * @param requestId correlation id for the response
     * @param roles     required roles (at least one must be present)
     * @return error response or null
     */
    protected Response requireRole(UserDto user, String requestId, UserRole... roles) {
        if (user == null) {
            return Response.error(requestId, "UNAUTHORIZED", "Authentication required.");
        }
        if (!RequestDispatcher.hasAnyRole(user, roles)) {
            LOG.warn("Authorization denied: user '{}' attempted action requiring roles {}",
                    user.getUsername(), roles);
            return Response.error(requestId, "FORBIDDEN",
                    "You do not have permission to perform this action.");
        }
        return null;
    }

    /**
     * Deserializes the request payload into the given type.
     * Returns {@code null} and logs a warning if parsing fails.
     *
     * @param request   the request whose payload to parse
     * @param type      target class
     * @param <T>       payload type
     * @return deserialized payload or null on failure
     */
    protected <T> T parsePayload(Request request, Class<T> type) {
        JsonNode payload = request.getPayload();
        if (payload == null || payload.isNull()) {
            LOG.warn("Request {} has no payload; expected {}", request.getType(), type.getSimpleName());
            return null;
        }
        try {
            return MAPPER.treeToValue(payload, type);
        } catch (JsonProcessingException e) {
            LOG.warn("Failed to parse payload for {}: {}", request.getType(), e.getMessage());
            return null;
        }
    }

    /**
     * Builds a JSON payload node from an object for use in a {@link Response}.
     *
     * @param data the object to serialize
     * @return JsonNode representation; null node on serialization failure
     */
    protected JsonNode toJsonNode(Object data) {
        return MAPPER.valueToTree(data);
    }

    /**
     * Returns a VALIDATION_ERROR response for a missing or malformed request payload.
     *
     * @param requestId correlation id
     * @return validation error response
     */
    protected Response missingPayload(String requestId) {
        return Response.error(requestId, "VALIDATION_ERROR",
                "Request body is required but was missing or malformed.");
    }

    /**
     * Wraps a string field extraction with a null/empty check.
     *
     * @param node      JSON node
     * @param fieldName field name
     * @return trimmed string value or null
     */
    protected String textOrNull(JsonNode node, String fieldName) {
        if (node == null || !node.has(fieldName)) {
            return null;
        }
        JsonNode field = node.get(fieldName);
        if (field.isNull() || field.asText().isBlank()) {
            return null;
        }
        return field.asText().trim();
    }

    /**
     * Returns the integer value of a JSON field, or the default value if absent/null.
     *
     * @param node         JSON node
     * @param fieldName    field name
     * @param defaultValue fallback
     * @return integer value
     */
    protected int intOrDefault(JsonNode node, String fieldName, int defaultValue) {
        if (node == null || !node.has(fieldName) || node.get(fieldName).isNull()) {
            return defaultValue;
        }
        return node.get(fieldName).asInt(defaultValue);
    }

    /**
     * Returns the long value of a JSON field, or the default if absent/null.
     *
     * @param node         JSON node
     * @param fieldName    field name
     * @param defaultValue fallback
     * @return long value
     */
    protected long longOrDefault(JsonNode node, String fieldName, long defaultValue) {
        if (node == null || !node.has(fieldName) || node.get(fieldName).isNull()) {
            return defaultValue;
        }
        return node.get(fieldName).asLong(defaultValue);
    }
}
