package com.labhub.client.network;

/**
 * Represents a failure from the LabHub API layer.
 *
 * <p>Thrown by {@link ApiClient} methods when:
 * <ul>
 *   <li>The server returns a non-success response (error code + message).</li>
 *   <li>A network/transport error prevents communication.</li>
 *   <li>The response payload cannot be deserialized into the expected type.</li>
 * </ul>
 *
 * <p>The {@link #getMessage()} is always safe to display to the user.
 * It is the message from the server's {@code Response.message} field,
 * which the server guarantees contains no stack traces or sensitive information.
 */
public class ApiException extends Exception {

    private final String code;

    /**
     * @param code    machine-readable error code (e.g. "UNAUTHORIZED", "CONFLICT")
     * @param message user-safe error description
     */
    public ApiException(String code, String message) {
        super(message);
        this.code = code;
    }

    /**
     * Returns the machine-readable error code.
     *
     * @return error code
     */
    public String getCode() {
        return code;
    }

    /** @return true if this is an authentication error */
    public boolean isUnauthorized() {
        return "UNAUTHORIZED".equals(code);
    }

    /** @return true if this is a permissions error */
    public boolean isForbidden() {
        return "FORBIDDEN".equals(code);
    }

    /** @return true if a network/transport error occurred */
    public boolean isNetworkError() {
        return "NETWORK_ERROR".equals(code);
    }

    /** @return true if a business-logic conflict occurred */
    public boolean isConflict() {
        return "CONFLICT".equals(code);
    }
}
