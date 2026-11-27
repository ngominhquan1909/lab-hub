package com.labhub.server.network;

import com.labhub.server.config.ServerConfig;
import com.labhub.server.handler.RequestDispatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * TCP socket server. Accepts incoming client connections and dispatches each
 * to a {@link ClientHandler} running in a bounded thread pool.
 *
 * <p>Thread pool is bounded to {@code server.max.threads} to prevent resource
 * exhaustion under high load. Connections beyond the pool limit are queued by
 * the executor's internal work queue.
 *
 * <p>Shutdown: calling {@link #stop()} closes the server socket, causing the
 * accept loop to exit gracefully. The executor is then shut down with a
 * configurable grace period.
 */
public class SocketServer {

    private static final Logger LOG = LoggerFactory.getLogger(SocketServer.class);
    private static final int SHUTDOWN_GRACE_SECONDS = 10;

    private final ServerConfig config;
    private final RequestDispatcher dispatcher;
    private volatile boolean running = false;
    private ServerSocket serverSocket;
    private ExecutorService executor;

    public SocketServer(ServerConfig config, RequestDispatcher dispatcher) {
        this.config = config;
        this.dispatcher = dispatcher;
    }

    /**
     * Starts the server and blocks until {@link #stop()} is called.
     *
     * @throws IOException if the server socket cannot be opened
     */
    public void start() throws IOException {
        executor = Executors.newFixedThreadPool(config.getMaxThreads(),
                r -> {
                    Thread t = new Thread(r, "labhub-client-handler");
                    t.setDaemon(false);
                    return t;
                });

        serverSocket = new ServerSocket();
        serverSocket.setReuseAddress(true);
        serverSocket.bind(new java.net.InetSocketAddress(config.getServerHost(), config.getServerPort()));

        running = true;
        LOG.info("LabHub server listening on {}:{}", config.getServerHost(), config.getServerPort());

        acceptLoop();
    }

    /**
     * Signals the server to stop and waits for active connections to finish.
     */
    public void stop() {
        LOG.info("Shutting down LabHub server...");
        running = false;

        // Close server socket to unblock accept()
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                LOG.debug("Error closing server socket: {}", e.getMessage());
            }
        }

        // Graceful executor shutdown
        if (executor != null) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(SHUTDOWN_GRACE_SECONDS, TimeUnit.SECONDS)) {
                    LOG.warn("Forcing shutdown after {}s grace period.", SHUTDOWN_GRACE_SECONDS);
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        LOG.info("Server shutdown complete.");
    }

    private void acceptLoop() {
        while (running) {
            try {
                Socket clientSocket = serverSocket.accept();
                clientSocket.setSoTimeout(0); // no read timeout: connections are persistent
                executor.submit(new ClientHandler(clientSocket, dispatcher));
            } catch (SocketException e) {
                if (running) {
                    // Unexpected socket error while still running
                    LOG.error("Server socket error: {}", e.getMessage(), e);
                }
                // If !running, this is the expected close - exit the loop silently
            } catch (IOException e) {
                if (running) {
                    LOG.warn("Failed to accept client connection: {}", e.getMessage());
                }
            }
        }
    }
}
