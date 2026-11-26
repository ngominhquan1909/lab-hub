package com.labhub.server.handler;

import com.labhub.common.dto.UserDto;
import com.labhub.common.protocol.Request;
import com.labhub.common.protocol.Response;

/**
 * Contract for all server-side request handlers.
 *
 * <p>Each handler is responsible for:
 * <ul>
 *   <li>Deserializing its specific payload from {@link Request#getPayload()}.</li>
 *   <li>Performing authorization checks (role, ownership).</li>
 *   <li>Delegating business logic to the appropriate service.</li>
 *   <li>Returning a well-formed {@link Response}.</li>
 * </ul>
 *
 * <p>Handlers must be thread-safe — they may be invoked concurrently by multiple
 * client handler threads. Stateless handlers that delegate to thread-safe services
 * satisfy this automatically.
 */
@FunctionalInterface
public interface RequestHandler {

    /**
     * Processes the request and produces a response.
     *
     * @param request     the incoming request; never null
     * @param currentUser the authenticated user; null only for the LOGIN request
     * @return a fully-formed response; never null
     */
    Response handle(Request request, UserDto currentUser);
}
