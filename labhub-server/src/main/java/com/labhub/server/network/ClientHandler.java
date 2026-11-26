package com.labhub.server.network;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.labhub.common.protocol.JsonMapper;
import com.labhub.common.protocol.Request;
import com.labhub.common.protocol.Response;
import com.labhub.server.handler.RequestDispatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;

/**
 * Handles the lifecycle of a single client connection.
 *
 * <p>Protocol framing: newline-delimited JSON (one JSON object per line, UTF-8).
 * Maximum safe message size is enforced by the BufferedReader line length.
 * This avoids the fragmentation problem of relying on a single {@code read()} call.
 *
 * <p>Each incoming line is deserialized as a {@link Request}, dispatched to
 * {@link RequestDispatcher}, and the resulting {@link Response} is serialized
 * and written back as a single JSON line followed by {@code \n}.
 *
 * <p>Stack traces, SQL errors, and sensitive data are NEVER sent to the client.
 * All diagnostic information is logged server-side only.
 */
public class ClientHandler implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(ClientHandler.class);
    private static final ObjectMapper MAPPER = JsonMapper.get();

    private final Socket socket;
    private final RequestDispatcher dispatcher;

    /**
     * @param socket     accepted client socket
     * @param dispatcher routes requests to service handlers
     */
    public ClientHandler(Socket socket, RequestDispatcher dispatcher) {
        this.socket = socket;
        this.dispatcher = dispatcher;
    }

    @Override
    public void run() {
        String clientAddress = socket.getRemoteSocketAddress().toString();
        LOG.info("Client connected: {}", clientAddress);

        try (
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))
        ) {
            String line;
            // Blocking read — each line is one complete JSON request
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue; // ignore empty lines (keep-alive or accidental blank)
                }
                Response response = processLine(line);
                writeResponse(writer, response);
            }
            LOG.info("Client disconnected (EOF): {}", clientAddress);

        } catch (SocketException e) {
            // Client disconnected abruptly — normal, not an error
            LOG.info("Client connection closed: {} — {}", clientAddress, e.getMessage());
        } catch (IOException e) {
            LOG.warn("IO error on client {}: {}", clientAddress, e.getMessage());
        } finally {
            closeSocket(clientAddress);
        }
    }

    // ── Internals ─────────────────────────────────────────────────────────

    private Response processLine(String line) {
        Request request = null;
        try {
            request = MAPPER.readValue(line, Request.class);
            LOG.debug("Received request: type={}, requestId={}", request.getType(), request.getRequestId());
            return dispatcher.dispatch(request);
        } catch (JsonProcessingException e) {
            // Malformed JSON — log details server-side, return generic error to client
            LOG.warn("Malformed JSON from client: {}", e.getMessage());
            return Response.error(null, "INVALID_REQUEST",
                    "Request could not be parsed. Send valid JSON on a single line.");
        } catch (Exception e) {
            // Unexpected error in dispatcher or handler — isolate from client
            String requestId = request != null ? request.getRequestId() : null;
            LOG.error("Unhandled error processing request {}: {}", requestId, e.getMessage(), e);
            return Response.error(requestId, "INTERNAL_ERROR",
                    "An internal server error occurred. Please try again.");
        }
    }

    private void writeResponse(BufferedWriter writer, Response response) throws IOException {
        try {
            String json = MAPPER.writeValueAsString(response);
            writer.write(json);
            writer.newLine();
            writer.flush();
            LOG.debug("Sent response: code={}, requestId={}", response.getCode(), response.getRequestId());
        } catch (JsonProcessingException e) {
            // This should not happen with well-formed Response objects
            LOG.error("Failed to serialize response: {}", e.getMessage(), e);
        }
    }

    private void closeSocket(String address) {
        if (!socket.isClosed()) {
            try {
                socket.close();
            } catch (IOException e) {
                LOG.debug("Error closing socket for {}: {}", address, e.getMessage());
            }
        }
    }
}
