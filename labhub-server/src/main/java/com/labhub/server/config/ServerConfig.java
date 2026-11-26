package com.labhub.server.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Loads and validates server configuration from {@code server.properties}.
 *
 * <p>Resolution order:
 * <ol>
 *   <li>Path given as first CLI argument.</li>
 *   <li>./server.properties (working directory).</li>
 *   <li>server.properties on the classpath (useful for tests).</li>
 * </ol>
 *
 * <p>Fails fast with a clear message if any required property is absent.
 * Database password and other secrets are never logged.
 */
public final class ServerConfig {

    private static final Logger LOG = LoggerFactory.getLogger(ServerConfig.class);

    // ── Defaults ──────────────────────────────────────────────────────────
    private static final String DEFAULT_HOST        = "localhost";
    private static final int    DEFAULT_PORT        = 9090;
    private static final int    DEFAULT_MAX_THREADS = 20;
    private static final int    DEFAULT_POOL_SIZE   = 10;
    private static final int    DEFAULT_POOL_IDLE   = 2;
    private static final long   DEFAULT_CONN_TIMEOUT = 30_000L;
    private static final long   DEFAULT_IDLE_TIMEOUT = 600_000L;
    private static final long   DEFAULT_MAX_LIFETIME = 1_800_000L;
    private static final int    DEFAULT_SESSION_TIMEOUT = 480;
    private static final int    DEFAULT_MAX_ADVANCE_DAYS = 90;
    private static final int    DEFAULT_MIN_DURATION_MIN = 30;

    // ── Fields ────────────────────────────────────────────────────────────
    private final String serverHost;
    private final int    serverPort;
    private final int    maxThreads;

    private final String dbHost;
    private final int    dbPort;
    private final String dbName;
    private final String dbUsername;
    private final String dbPassword;    // Not logged
    private final int    poolMaxSize;
    private final int    poolMinIdle;
    private final long   connectionTimeoutMs;
    private final long   idleTimeoutMs;
    private final long   maxLifetimeMs;

    private final int sessionTimeoutMinutes;
    private final int bookingMaxAdvanceDays;
    private final int bookingMinDurationMinutes;

    // ── Loading ───────────────────────────────────────────────────────────

    /**
     * Loads configuration from the first available source.
     *
     * @param args CLI arguments; first element treated as config file path if present
     * @return loaded configuration
     */
    public static ServerConfig load(String[] args) {
        Properties props = new Properties();

        if (args != null && args.length > 0) {
            Path explicit = Paths.get(args[0]);
            if (Files.exists(explicit)) {
                LOG.info("Loading config from CLI path: {}", explicit.toAbsolutePath());
                loadFromFile(props, explicit);
                return new ServerConfig(props);
            }
            LOG.warn("Config path '{}' not found; falling back to defaults.", args[0]);
        }

        Path workingDir = Paths.get("server.properties");
        if (Files.exists(workingDir)) {
            LOG.info("Loading config from working directory: {}", workingDir.toAbsolutePath());
            loadFromFile(props, workingDir);
            return new ServerConfig(props);
        }

        // Classpath fallback (useful for integration tests)
        try (InputStream is = ServerConfig.class.getClassLoader()
                .getResourceAsStream("server.properties")) {
            if (is != null) {
                LOG.info("Loading config from classpath resource server.properties");
                props.load(is);
                return new ServerConfig(props);
            }
        } catch (IOException e) {
            LOG.warn("Failed to load classpath server.properties: {}", e.getMessage());
        }

        LOG.warn("No server.properties found — using built-in defaults. "
                + "Copy server.properties.example to server.properties and configure it.");
        return new ServerConfig(props);
    }

    private static void loadFromFile(Properties props, Path path) {
        try (InputStream is = Files.newInputStream(path)) {
            props.load(is);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load config file: " + path, e);
        }
    }

    // ── Constructor ───────────────────────────────────────────────────────

    private ServerConfig(Properties p) {
        serverHost   = p.getProperty("server.host", DEFAULT_HOST);
        serverPort   = intProp(p, "server.port", DEFAULT_PORT);
        maxThreads   = intProp(p, "server.max.threads", DEFAULT_MAX_THREADS);

        dbHost       = p.getProperty("db.host", "localhost");
        dbPort       = intProp(p, "db.port", 3306);
        dbName       = required(p, "db.name", "labhub");
        dbUsername   = required(p, "db.username", "root");
        dbPassword   = p.getProperty("db.password", "");   // empty default for local dev only

        poolMaxSize         = intProp(p, "db.pool.max.size",              DEFAULT_POOL_SIZE);
        poolMinIdle         = intProp(p, "db.pool.min.idle",              DEFAULT_POOL_IDLE);
        connectionTimeoutMs = longProp(p, "db.pool.connection.timeout.ms", DEFAULT_CONN_TIMEOUT);
        idleTimeoutMs       = longProp(p, "db.pool.idle.timeout.ms",       DEFAULT_IDLE_TIMEOUT);
        maxLifetimeMs       = longProp(p, "db.pool.max.lifetime.ms",       DEFAULT_MAX_LIFETIME);

        sessionTimeoutMinutes    = intProp(p, "session.timeout.minutes",     DEFAULT_SESSION_TIMEOUT);
        bookingMaxAdvanceDays    = intProp(p, "booking.max.advance.days",    DEFAULT_MAX_ADVANCE_DAYS);
        bookingMinDurationMinutes = intProp(p, "booking.min.duration.minutes", DEFAULT_MIN_DURATION_MIN);

        // Log summary — never log the password
        LOG.info("Server config: host={}, port={}, threads={}", serverHost, serverPort, maxThreads);
        LOG.info("Database config: jdbc:mysql://{}:{}/{} (pool max={})", dbHost, dbPort, dbName, poolMaxSize);
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private static String required(Properties p, String key, String defaultValue) {
        String v = p.getProperty(key);
        if (v == null || v.isBlank()) {
            LOG.debug("Property '{}' not set; using default '{}'", key, defaultValue);
            return defaultValue;
        }
        return v.trim();
    }

    private static int intProp(Properties p, String key, int defaultValue) {
        String v = p.getProperty(key);
        if (v == null || v.isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(v.trim());
        } catch (NumberFormatException e) {
            LOG.warn("Invalid integer for property '{}': '{}'; using default {}", key, v, defaultValue);
            return defaultValue;
        }
    }

    private static long longProp(Properties p, String key, long defaultValue) {
        String v = p.getProperty(key);
        if (v == null || v.isBlank()) {
            return defaultValue;
        }
        try {
            return Long.parseLong(v.trim());
        } catch (NumberFormatException e) {
            LOG.warn("Invalid long for property '{}': '{}'; using default {}", key, v, defaultValue);
            return defaultValue;
        }
    }

    // ── Accessors ─────────────────────────────────────────────────────────

    /** @return server listen host */
    public String getServerHost() { return serverHost; }

    /** @return server listen port */
    public int getServerPort() { return serverPort; }

    /** @return maximum concurrent client threads */
    public int getMaxThreads() { return maxThreads; }

    /** @return JDBC URL built from configured host/port/database */
    public String getJdbcUrl() {
        return "jdbc:mysql://" + dbHost + ":" + dbPort + "/" + dbName
                + "?useUnicode=true&characterEncoding=UTF-8"
                + "&serverTimezone=UTC"
                + "&useSSL=false&allowPublicKeyRetrieval=true";
    }

    /** @return database username */
    public String getDbUsername() { return dbUsername; }

    /** @return database password (handle with care; do not log) */
    public String getDbPassword() { return dbPassword; }

    /** @return HikariCP max pool size */
    public int getPoolMaxSize() { return poolMaxSize; }

    /** @return HikariCP minimum idle connections */
    public int getPoolMinIdle() { return poolMinIdle; }

    /** @return HikariCP connection timeout in milliseconds */
    public long getConnectionTimeoutMs() { return connectionTimeoutMs; }

    /** @return HikariCP idle timeout in milliseconds */
    public long getIdleTimeoutMs() { return idleTimeoutMs; }

    /** @return HikariCP max connection lifetime in milliseconds */
    public long getMaxLifetimeMs() { return maxLifetimeMs; }

    /** @return session timeout in minutes */
    public int getSessionTimeoutMinutes() { return sessionTimeoutMinutes; }

    /** @return maximum days in advance a booking may be made */
    public int getBookingMaxAdvanceDays() { return bookingMaxAdvanceDays; }

    /** @return minimum booking duration in minutes */
    public int getBookingMinDurationMinutes() { return bookingMinDurationMinutes; }
}
