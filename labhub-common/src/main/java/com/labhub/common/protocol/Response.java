package com.labhub.common.protocol;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Envelope for every server-to-client message.
 *
 * <p>Wire format: a single JSON object on one UTF-8 line, terminated by {@code \n}.
 *
 * <pre>
 * {
 *   "requestId": "e2d9d9c3-...",
 *   "success":   true,
 *   "code":      "OK",
 *   "message":   "Equipment loaded.",
 *   "payload":   { ... }
 * }
 * </pre>
 *
 * <p>Error codes include: OK, VALIDATION_ERROR, UNAUTHORIZED, FORBIDDEN, NOT_FOUND,
 * CONFLICT, DATABASE_ERROR, INTERNAL_ERROR.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Response {

    /** Echoed from the corresponding {@link Request#getRequestId()}. */
    private String requestId;

    /** True if the request was processed successfully. */
    private boolean success;

    /** Machine-readable result code. Always present. */
    private String code;

    /** Human-readable message safe to display in the UI. Never contains stack traces or SQL. */
    private String message;

    /**
     * Optional response body; structure depends on request type.
     * Null for responses that carry no data (e.g., LOGOUT, CHANGE_PASSWORD success).
     */
    private JsonNode payload;

    /**
     * Creates a successful response without a payload.
     *
     * @param requestId correlation ID
     * @param message   human-readable success message
     * @return response
     */
    public static Response ok(String requestId, String message) {
        Response r = new Response();
        r.requestId = requestId;
        r.success = true;
        r.code = "OK";
        r.message = message;
        return r;
    }

    /**
     * Creates a successful response with a payload.
     *
     * @param requestId correlation ID
     * @param message   human-readable success message
     * @param payload   serialized response body
     * @return response
     */
    public static Response ok(String requestId, String message, JsonNode payload) {
        Response r = ok(requestId, message);
        r.payload = payload;
        return r;
    }

    /**
     * Creates an error response.
     *
     * @param requestId correlation ID (may be null if request was malformed)
     * @param code      machine-readable error code
     * @param message   human-readable error description safe for the client to display
     * @return response
     */
    public static Response error(String requestId, String code, String message) {
        Response r = new Response();
        r.requestId = requestId;
        r.success = false;
        r.code = code;
        r.message = message;
        return r;
    }

    public String getRequestId() { return requestId; }

    public void setRequestId(String requestId) { this.requestId = requestId; }

    public boolean isSuccess() { return success; }

    public void setSuccess(boolean success) { this.success = success; }

    public String getCode() { return code; }

    public void setCode(String code) { this.code = code; }

    public String getMessage() { return message; }

    public void setMessage(String message) { this.message = message; }

    public JsonNode getPayload() { return payload; }

    public void setPayload(JsonNode payload) { this.payload = payload; }

    @Override
    public String toString() {
        return "Response{requestId='" + requestId + "', success=" + success + ", code='" + code + "'}";
    }
}
