package com.labhub.server.repository;

import com.labhub.common.dto.PagedResult;
import com.labhub.common.dto.UserDto;
import com.labhub.common.enums.UserRole;
import com.labhub.server.config.DatabaseManager;
import com.labhub.server.domain.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data access layer for user accounts and their role assignments.
 *
 * <p>All SQL uses prepared statements to prevent injection.
 * Password hashes are stored and retrieved, but are never sent to the client
 * (that is enforced by {@link User#toDto()}).
 */
public class UserRepository {

    private static final Logger LOG = LoggerFactory.getLogger(UserRepository.class);

    private final DatabaseManager db;

    public UserRepository(DatabaseManager db) {
        this.db = db;
    }

    /**
     * Finds a user by username. Used during login.
     *
     * @param username the login username
     * @return the user domain object including password hash, or empty
     */
    public Optional<User> findByUsername(String username) {
        String sql = "SELECT id, username, email, full_name, phone_number, password_hash, "
                + "is_active, created_at, updated_at, last_login_at "
                + "FROM users WHERE username = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    User user = mapUser(rs);
                    user.setRoles(findRoles(conn, user.getId()));
                    return Optional.of(user);
                }
            }
        } catch (SQLException e) {
            LOG.error("Error finding user by username '{}': {}", username, e.getMessage(), e);
        }
        return Optional.empty();
    }

    /**
     * Finds a user by ID.
     *
     * @param id user ID
     * @return user or empty
     */
    public Optional<User> findById(long id) {
        String sql = "SELECT id, username, email, full_name, phone_number, password_hash, "
                + "is_active, created_at, updated_at, last_login_at "
                + "FROM users WHERE id = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    User user = mapUser(rs);
                    user.setRoles(findRoles(conn, user.getId()));
                    return Optional.of(user);
                }
            }
        } catch (SQLException e) {
            LOG.error("Error finding user by id {}: {}", id, e.getMessage(), e);
        }
        return Optional.empty();
    }

    /**
     * Returns a paginated list of users with optional search filter.
     *
     * @param search   filter string applied to username, email, full_name (null = no filter)
     * @param page     1-based page number
     * @param pageSize items per page
     * @return paged result of user DTOs (no password hash)
     */
    public PagedResult<UserDto> findAll(String search, int page, int pageSize) {
        String whereClause = (search != null && !search.isBlank())
                ? " WHERE (username LIKE ? OR email LIKE ? OR full_name LIKE ?)"
                : "";
        String countSql = "SELECT COUNT(*) FROM users" + whereClause;
        String dataSql  = "SELECT id, username, email, full_name, phone_number, password_hash, "
                + "is_active, created_at, updated_at, last_login_at "
                + "FROM users" + whereClause
                + " ORDER BY full_name ASC"
                + " LIMIT ? OFFSET ?";

        try (Connection conn = db.getConnection()) {
            long total = 0;
            try (PreparedStatement ps = conn.prepareStatement(countSql)) {
                if (search != null && !search.isBlank()) {
                    String like = "%" + search + "%";
                    ps.setString(1, like);
                    ps.setString(2, like);
                    ps.setString(3, like);
                }
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        total = rs.getLong(1);
                    }
                }
            }

            List<UserDto> items = new ArrayList<>();
            int offset = (page - 1) * pageSize;
            try (PreparedStatement ps = conn.prepareStatement(dataSql)) {
                int idx = 1;
                if (search != null && !search.isBlank()) {
                    String like = "%" + search + "%";
                    ps.setString(idx++, like);
                    ps.setString(idx++, like);
                    ps.setString(idx++, like);
                }
                ps.setInt(idx++, pageSize);
                ps.setInt(idx,   offset);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        User user = mapUser(rs);
                        user.setRoles(findRoles(conn, user.getId()));
                        items.add(user.toDto());
                    }
                }
            }
            return new PagedResult<>(items, page, pageSize, total);
        } catch (SQLException e) {
            LOG.error("Error listing users: {}", e.getMessage(), e);
            return new PagedResult<>(List.of(), page, pageSize, 0);
        }
    }

    /**
     * Creates a new user account and assigns roles within a single transaction.
     *
     * @param user   the user to persist (passwordHash must already be a BCrypt hash)
     * @param roles  roles to assign
     * @return the generated user ID
     * @throws SQLException if the insert fails (e.g. duplicate username/email)
     */
    public long create(User user, List<UserRole> roles) throws SQLException {
        String userSql = "INSERT INTO users (username, email, full_name, phone_number, password_hash, is_active) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = db.getConnection()) {
            conn.setAutoCommit(false);
            try {
                long userId;
                try (PreparedStatement ps = conn.prepareStatement(userSql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, user.getUsername());
                    ps.setString(2, user.getEmail());
                    ps.setString(3, user.getFullName());
                    ps.setString(4, user.getPhoneNumber());
                    ps.setString(5, user.getPasswordHash());
                    ps.setBoolean(6, true);
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        keys.next();
                        userId = keys.getLong(1);
                    }
                }
                assignRoles(conn, userId, roles);
                conn.commit();
                LOG.info("Created user id={} username='{}'", userId, user.getUsername());
                return userId;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    /**
     * Updates mutable user fields (not password, not username).
     *
     * @param id          user ID
     * @param email       new email
     * @param fullName    new full name
     * @param phoneNumber new phone number
     * @param roles       new role set
     * @throws SQLException on DB error
     */
    public void update(long id, String email, String fullName,
                       String phoneNumber, List<UserRole> roles) throws SQLException {
        String sql = "UPDATE users SET email = ?, full_name = ?, phone_number = ? WHERE id = ?";
        try (Connection conn = db.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, email);
                    ps.setString(2, fullName);
                    ps.setString(3, phoneNumber);
                    ps.setLong(4, id);
                    ps.executeUpdate();
                }
                // Replace roles: delete existing, re-insert
                deleteRoles(conn, id);
                assignRoles(conn, id, roles);
                conn.commit();
                LOG.info("Updated user id={}", id);
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    /**
     * Changes a user's password hash.
     *
     * @param id           user ID
     * @param newHashedPwd new BCrypt hash
     * @throws SQLException on DB error
     */
    public void updatePassword(long id, String newHashedPwd) throws SQLException {
        String sql = "UPDATE users SET password_hash = ? WHERE id = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newHashedPwd);
            ps.setLong(2, id);
            ps.executeUpdate();
        }
    }

    /**
     * Sets the is_active flag (enable/disable).
     *
     * @param id     user ID
     * @param active true to enable, false to disable
     * @throws SQLException on DB error
     */
    public void setActive(long id, boolean active) throws SQLException {
        String sql = "UPDATE users SET is_active = ? WHERE id = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBoolean(1, active);
            ps.setLong(2, id);
            ps.executeUpdate();
        }
    }

    /**
     * Records a successful login timestamp.
     *
     * @param id user ID
     */
    public void recordLogin(long id) {
        String sql = "UPDATE users SET last_login_at = UTC_TIMESTAMP() WHERE id = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            LOG.warn("Failed to record login for user {}: {}", id, e.getMessage());
        }
    }

    private List<UserRole> findRoles(Connection conn, long userId) throws SQLException {
        String sql = "SELECT r.name FROM roles r "
                + "JOIN user_roles ur ON r.id = ur.role_id "
                + "WHERE ur.user_id = ?";
        List<UserRole> roles = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    try {
                        roles.add(UserRole.valueOf(rs.getString("name")));
                    } catch (IllegalArgumentException e) {
                        LOG.warn("Unknown role in DB: {}", rs.getString("name"));
                    }
                }
            }
        }
        return roles;
    }

    private void assignRoles(Connection conn, long userId, List<UserRole> roles) throws SQLException {
        if (roles == null || roles.isEmpty()) {
            return;
        }
        String roleSql = "INSERT INTO user_roles (user_id, role_id) "
                + "SELECT ?, id FROM roles WHERE name = ?";
        try (PreparedStatement ps = conn.prepareStatement(roleSql)) {
            for (UserRole role : roles) {
                ps.setLong(1, userId);
                ps.setString(2, role.name());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private void deleteRoles(Connection conn, long userId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "DELETE FROM user_roles WHERE user_id = ?")) {
            ps.setLong(1, userId);
            ps.executeUpdate();
        }
    }

    private User mapUser(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getLong("id"));
        u.setUsername(rs.getString("username"));
        u.setEmail(rs.getString("email"));
        u.setFullName(rs.getString("full_name"));
        u.setPhoneNumber(rs.getString("phone_number"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setActive(rs.getBoolean("is_active"));
        u.setCreatedAt(toLocalDateTime(rs.getTimestamp("created_at")));
        u.setUpdatedAt(toLocalDateTime(rs.getTimestamp("updated_at")));
        u.setLastLoginAt(toLocalDateTime(rs.getTimestamp("last_login_at")));
        return u;
    }

    private LocalDateTime toLocalDateTime(Timestamp ts) {
        return ts != null ? ts.toLocalDateTime() : null;
    }
}
