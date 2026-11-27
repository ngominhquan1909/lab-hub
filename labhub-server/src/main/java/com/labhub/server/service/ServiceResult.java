package com.labhub.server.service;

/**
 * Generic result wrapper for service operations.
 *
 * <p>Avoids using exceptions for expected business-logic failures (validation errors,
 * state conflicts, etc.). Unexpected infrastructure failures (DB down, etc.) are still
 * propagated as exceptions from repositories.
 *
 * @param <T> type of the successful result payload; may be Void
 */
public final class ServiceResult<T> {

    private final boolean success;
    private final T data;
    private final String errorMessage;

    private ServiceResult(boolean success, T data, String errorMessage) {
        this.success = success;
        this.data = data;
        this.errorMessage = errorMessage;
    }

    /**
     * Creates a successful result with data.
     *
     * @param data the result payload
     * @param <T>  payload type
     * @return success result
     */
    public static <T> ServiceResult<T> success(T data) {
        return new ServiceResult<>(true, data, null);
    }

    /**
     * Creates a failure result with an error message.
     *
     * @param message human-readable error description
     * @param <T>     payload type
     * @return failure result
     */
    public static <T> ServiceResult<T> failure(String message) {
        return new ServiceResult<>(false, null, message);
    }

    /**
     * Returns true if the operation succeeded.
     *
     * @return true if successful
     */
    public boolean isSuccess() { return success; }

    /**
     * Returns the result payload. Non-null only on success.
     *
     * @return result data
     */
    public T getData() { return data; }

    /**
     * Returns the error message. Non-null only on failure.
     *
     * @return error message
     */
    public String getErrorMessage() { return errorMessage; }
}
