package com.labhub.client.network;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.labhub.common.protocol.JsonMapper;
import com.labhub.common.protocol.Request;
import com.labhub.common.protocol.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Manages a single persistent TCP connection to the LabHub server.
 *
 * <p>Protocol: newline-delimited JSON — one JSON object per line, UTF-8.
 *
 * <p>Thread safety: {@link #send(Request)} acquires a lock so multiple JavaFX
 * background tasks can share the connection safely. The lock ensures that one
 * complete request→response cycle completes before another starts on the same
 * socket. For true multiplexing, each call could open its own connection, but
 * this is unnecessary given the interaction patterns of a desktop app.
 *
 * <p>All network calls must happen off the JavaFX Application Thread.
 * Use {@code Task<Response>} wrappers in controllers.
 */
public class ServerConnection implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(ServerConnection.class);
    private static final ObjectMapper MAPPER = JsonMapper.get();

    private final String host;
    private final int port;
    private final int connectTimeoutMs;
    private final int readTimeoutMs;

    private Socket socket;
    private BufferedReader reader;
    private BufferedWriter writer;
    private final ReentrantLock lock = new ReentrantLock();
    private volatile boolean connected = false;

    /**
     * @param host             server hostname
     * @param port             server port
     * @param connectTimeoutMs connection timeout in milliseconds
     * @param readTimeoutMs    read timeout in milliseconds
     */
    public ServerConnection(String host, int port, int connectTimeoutMs, int readTimeoutMs) {
        this.host = host;
        this.port = port;
        this.connectTimeoutMs = connectTimeoutMs;
        this.readTimeoutMs = readTimeoutMs;
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────

    /**
     * Opens the TCP connection to the server.
     *
     * @throws IOException if the connection cannot be established
     */
    public void connect() throws IOException {
        socket = new Socket();
        socket.connect(new InetSocketAddress(host, port), connectTimeoutMs);
        socket.setSoTimeout(readTimeoutMs);
        reader = new BufferedReader(
                new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        writer = new BufferedWriter(
                new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
        connected = true;
        LOG.info("Connected to LabHub server at {}:{}", host, port);
    }

    /**
     * Closes the connection and releases resources.
     */
    @Override
    public void close() {
        connected = false;
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            LOG.debug("Error closing socket: {}", e.getMessage());
        }
        LOG.info("Disconnected from server.");
    }

    /** @return true if the connection is currently open */
    public boolean isConnected() {
        return connected && socket != null && !socket.isClosed();
    }

    // ── Request/response ──────────────────────────────────────────────────

    /**
     * Sends a request to the server and waits for the response.
     *
     * <p>This method blocks until the response is received or a timeout occurs.
     * It MUST be called from a background thread, never from the JavaFX Application Thread.
     *
     * @param request the request to send
     * @return the server's response
     * @throws IOException      if the connection fails or times out
     * @throws IllegalStateException if the connection is not open
     */
    public Response send(Request request) throws IOException {
        if (!isConnected()) {
            throw new IllegalStateException("Not connected to server. Call connect() first.");
        }
        lock.lock();
        try {
            // Serialize request to JSON on one line + newline terminator
            String requestJson = MAPPER.writeValueAsString(request);
            writer.write(requestJson);
            writer.newLine();
            writer.flush();
            LOG.debug("→ {}", request);

            // Read one line = one complete JSON response
            String responseLine = reader.readLine();
            if (responseLine == null) {
                connected = false;
                throw new IOException("Server closed the connection.");
            }
            Response response = MAPPER.readValue(responseLine, Response.class);
            LOG.debug("← {}", response);
            return response;
        } finally {
            lock.unlock();
        }
    }

    // ── Accessors ─────────────────────────────────────────────────────────

    /** @return server hostname */
    public String getHost() { return host; }

    /** @return server port */
    public int getPort() { return port; }
}
