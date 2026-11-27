package com.labhub.server.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Manages the HikariCP connection pool.
 *
 * <p>Use {@link #getConnection()} within a try-with-resources block so connections
 * are always returned to the pool:
 *
 * <pre>{@code
 * try (Connection conn = databaseManager.getConnection()) {
 *     // ... JDBC operations ...
 * }
 * }</pre>
 *
 * <p>Close the pool with {@link #close()} on application shutdown.
 */
public final class DatabaseManager implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(DatabaseManager.class);

    private final HikariDataSource dataSource;

    /**
     * Initializes the connection pool from the given configuration.
     *
     * @param config server configuration
     * @throws IllegalStateException if the pool cannot connect to the database
     */
    public DatabaseManager(ServerConfig config) {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(config.getJdbcUrl());
        hikariConfig.setUsername(config.getDbUsername());
        hikariConfig.setPassword(config.getDbPassword());
        hikariConfig.setMaximumPoolSize(config.getPoolMaxSize());
        hikariConfig.setMinimumIdle(config.getPoolMinIdle());
        hikariConfig.setConnectionTimeout(config.getConnectionTimeoutMs());
        hikariConfig.setIdleTimeout(config.getIdleTimeoutMs());
        hikariConfig.setMaxLifetime(config.getMaxLifetimeMs());
        hikariConfig.setPoolName("LabHubPool");

        // Validate the connection on acquisition and discard broken connections
        hikariConfig.setConnectionTestQuery("SELECT 1");
        hikariConfig.setConnectionInitSql("SET time_zone = '+00:00'");
        hikariConfig.setValidationTimeout(5_000L);

        // MySQL-specific settings
        hikariConfig.addDataSourceProperty("cachePrepStmts",          "true");
        hikariConfig.addDataSourceProperty("prepStmtCacheSize",        "250");
        hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit",    "2048");
        hikariConfig.addDataSourceProperty("useServerPrepStmts",       "true");

        LOG.info("Initializing database connection pool...");
        try {
            this.dataSource = new HikariDataSource(hikariConfig);
            // Verify at least one connection can be established
            try (Connection testConn = dataSource.getConnection()) {
                LOG.info("Database connection established: {}", testConn.getMetaData().getURL());
            }
        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Failed to connect to the database. Check server.properties and ensure MySQL is running. "
                    + "Error: " + e.getMessage(), e);
        }
    }

    /**
     * Returns a connection from the pool.
     * Always use in a try-with-resources block.
     *
     * @return a JDBC connection
     * @throws SQLException if no connection is available
     */
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    /**
     * Closes the connection pool and releases all resources.
     * Call this once during application shutdown.
     */
    @Override
    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            LOG.info("Closing database connection pool.");
            dataSource.close();
        }
    }

    public boolean isHealthy() {
        return dataSource != null && !dataSource.isClosed();
    }
}
