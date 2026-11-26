package com.labhub.server.repository;

import com.labhub.common.dto.AuditLogDto;
import com.labhub.common.dto.DashboardStatsDto;
import com.labhub.common.dto.PagedResult;
import com.labhub.server.config.DatabaseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Data access layer for dashboard statistics and audit log queries.
 *
 * <p>All stat queries run in a single connection to avoid multiple pool checkouts.
 * Values are always read from the database — none are hard-coded.
 */
public class DashboardRepository {

    private static final Logger LOG = LoggerFactory.getLogger(DashboardRepository.class);

    private final DatabaseManager db;

    /**
     * @param db database manager
     */
    public DashboardRepository(DatabaseManager db) {
        this.db = db;
    }

    // ── Dashboard stats ───────────────────────────────────────────────────

    /**
     * Builds and returns current operational statistics.
     * All values come from live DB queries.
     *
     * @return dashboard statistics DTO
     */
    public DashboardStatsDto getStats() {
        DashboardStatsDto stats = new DashboardStatsDto();
        try (Connection conn = db.getConnection()) {
            stats.setTotalActiveLabs(querySingleInt(conn,
                    "SELECT COUNT(*) FROM labs WHERE status = 'ACTIVE'"));

            stats.setTotalEquipment(querySingleInt(conn,
                    "SELECT COUNT(*) FROM equipment WHERE is_active = 1"));

            stats.setAvailableEquipment(querySingleInt(conn,
                    "SELECT COUNT(*) FROM equipment WHERE is_active = 1 AND status = 'AVAILABLE'"));

            stats.setBorrowedEquipment(querySingleInt(conn,
                    "SELECT COUNT(*) FROM equipment WHERE is_active = 1 AND status = 'BORROWED'"));

            stats.setMaintenanceEquipment(querySingleInt(conn,
                    "SELECT COUNT(*) FROM equipment WHERE is_active = 1 AND status = 'MAINTENANCE'"));

            stats.setPendingBookings(querySingleInt(conn,
                    "SELECT COUNT(*) FROM bookings WHERE status = 'PENDING'"));

            stats.setPendingBorrowRequests(querySingleInt(conn,
                    "SELECT COUNT(*) FROM borrow_requests WHERE status = 'PENDING'"));

            stats.setTodayBookings(querySingleInt(conn,
                    "SELECT COUNT(*) FROM bookings WHERE status = 'APPROVED' "
                    + "AND DATE(start_time) = CURDATE()"));

            stats.setOverdueItems(querySingleInt(conn,
                    "SELECT COUNT(*) FROM borrow_requests "
                    + "WHERE status IN ('ACTIVE','OVERDUE') "
                    + "AND expected_return_at < UTC_TIMESTAMP()"));

            stats.setOpenMaintenanceTickets(querySingleInt(conn,
                    "SELECT COUNT(*) FROM maintenance_tickets "
                    + "WHERE status IN ('OPEN','ASSIGNED','IN_PROGRESS')"));

        } catch (SQLException e) {
            LOG.error("Error fetching dashboard stats: {}", e.getMessage(), e);
        }
        return stats;
    }

    // ── Audit log ─────────────────────────────────────────────────────────

    /**
     * Returns paginated audit log entries, newest first.
     *
     * @param actorId    filter by actor (null = all)
     * @param action     filter by action string (null = all)
     * @param page       1-based page
     * @param pageSize   items per page
     * @return paged audit entries
     */
    public PagedResult<AuditLogDto> findAuditLogs(Long actorId, String action,
                                                   int page, int pageSize) {
        StringBuilder where = new StringBuilder(" WHERE 1=1");
        List<Object> params = new ArrayList<>();
        if (actorId != null) {
            where.append(" AND al.actor_id = ?");
            params.add(actorId);
        }
        if (action != null && !action.isBlank()) {
            where.append(" AND al.action LIKE ?");
            params.add("%" + action + "%");
        }

        String countSql = "SELECT COUNT(*) FROM audit_logs al" + where;
        String dataSql  = "SELECT al.id, al.actor_id, u.full_name AS actor_name, "
                + "al.action, al.target_type, al.target_id, al.details, al.timestamp "
                + "FROM audit_logs al LEFT JOIN users u ON al.actor_id = u.id"
                + where + " ORDER BY al.timestamp DESC LIMIT ? OFFSET ?";

        try (Connection conn = db.getConnection()) {
            long total = 0;
            try (PreparedStatement ps = conn.prepareStatement(countSql)) {
                for (int i = 0; i < params.size(); i++) {
                    ps.setObject(i + 1, params.get(i));
                }
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        total = rs.getLong(1);
                    }
                }
            }

            List<AuditLogDto> items = new ArrayList<>();
            int offset = (page - 1) * pageSize;
            try (PreparedStatement ps = conn.prepareStatement(dataSql)) {
                int idx = 1;
                for (Object p : params) {
                    ps.setObject(idx++, p);
                }
                ps.setInt(idx++, pageSize);
                ps.setInt(idx,   offset);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        items.add(mapAudit(rs));
                    }
                }
            }
            return new PagedResult<>(items, page, pageSize, total);
        } catch (SQLException e) {
            LOG.error("Error fetching audit logs: {}", e.getMessage(), e);
            return new PagedResult<>(List.of(), page, pageSize, 0);
        }
    }

    /**
     * Records an audit log entry. Never fails silently — warnings are logged.
     *
     * @param actorId    ID of the user performing the action (null for system)
     * @param action     action identifier, e.g. "BOOKING_APPROVED"
     * @param targetType entity type, e.g. "BOOKING"
     * @param targetId   entity ID
     * @param details    concise description of what changed (no secrets)
     */
    public void log(Long actorId, String action, String targetType,
                    Long targetId, String details) {
        String sql = "INSERT INTO audit_logs (actor_id, action, target_type, target_id, details) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (actorId != null) {
                ps.setLong(1, actorId);
            } else {
                ps.setNull(1, java.sql.Types.BIGINT);
            }
            ps.setString(2, action);
            ps.setString(3, targetType);
            if (targetId != null) {
                ps.setLong(4, targetId);
            } else {
                ps.setNull(4, java.sql.Types.BIGINT);
            }
            ps.setString(5, details);
            ps.executeUpdate();
        } catch (SQLException e) {
            LOG.warn("Failed to write audit log entry [{}]: {}", action, e.getMessage());
        }
    }

    // ── Internal ──────────────────────────────────────────────────────────

    private int querySingleInt(Connection conn, String sql) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private AuditLogDto mapAudit(ResultSet rs) throws SQLException {
        AuditLogDto dto = new AuditLogDto();
        dto.setId(rs.getLong("id"));
        long actorId = rs.getLong("actor_id");
        if (!rs.wasNull()) {
            dto.setActorId(actorId);
        }
        dto.setActorName(rs.getString("actor_name"));
        dto.setAction(rs.getString("action"));
        dto.setTargetType(rs.getString("target_type"));
        long targetId = rs.getLong("target_id");
        if (!rs.wasNull()) {
            dto.setTargetId(targetId);
        }
        dto.setDetails(rs.getString("details"));
        dto.setTimestamp(toLocalDateTime(rs.getTimestamp("timestamp")));
        return dto;
    }

    private LocalDateTime toLocalDateTime(Timestamp ts) {
        return ts != null ? ts.toLocalDateTime() : null;
    }
}
