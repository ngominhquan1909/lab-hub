package com.labhub.server.repository;

import com.labhub.common.dto.MaintenanceTicketDto;
import com.labhub.common.dto.PagedResult;
import com.labhub.common.enums.MaintenancePriority;
import com.labhub.common.enums.MaintenanceStatus;
import com.labhub.server.config.DatabaseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data access layer for maintenance tickets.
 */
public class MaintenanceRepository {

    private static final Logger LOG = LoggerFactory.getLogger(MaintenanceRepository.class);

    private final DatabaseManager db;

    /**
     * @param db database manager
     */
    public MaintenanceRepository(DatabaseManager db) {
        this.db = db;
    }

    // ── Queries ───────────────────────────────────────────────────────────

    /**
     * Finds a maintenance ticket by its ID.
     *
     * @param id ticket ID
     * @return ticket DTO or empty
     */
    public Optional<MaintenanceTicketDto> findById(long id) {
        String sql = baseSql() + " WHERE mt.id = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapTicket(rs));
                }
            }
        } catch (SQLException e) {
            LOG.error("Error finding maintenance ticket {}: {}", id, e.getMessage(), e);
        }
        return Optional.empty();
    }

    /**
     * Returns paginated maintenance tickets with optional filters.
     *
     * @param equipmentId filter by equipment (null = all)
     * @param status      filter by status (null = all)
     * @param priority    filter by priority (null = all)
     * @param page        1-based page
     * @param pageSize    items per page
     * @return paged tickets
     */
    public PagedResult<MaintenanceTicketDto> findAll(Long equipmentId,
                                                      MaintenanceStatus status,
                                                      MaintenancePriority priority,
                                                      int page, int pageSize) {
        StringBuilder where = new StringBuilder(" WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (equipmentId != null) {
            where.append(" AND mt.equipment_id = ?");
            params.add(equipmentId);
        }
        if (status != null) {
            where.append(" AND mt.status = ?");
            params.add(status.name());
        }
        if (priority != null) {
            where.append(" AND mt.priority = ?");
            params.add(priority.name());
        }

        String countSql = "SELECT COUNT(*) FROM maintenance_tickets mt" + where;
        String dataSql  = baseSql() + where
                + " ORDER BY FIELD(mt.priority,'URGENT','HIGH','MEDIUM','LOW'), mt.opened_at DESC"
                + " LIMIT ? OFFSET ?";

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

            List<MaintenanceTicketDto> items = new ArrayList<>();
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
                        items.add(mapTicket(rs));
                    }
                }
            }
            return new PagedResult<>(items, page, pageSize, total);
        } catch (SQLException e) {
            LOG.error("Error listing maintenance tickets: {}", e.getMessage(), e);
            return new PagedResult<>(List.of(), page, pageSize, 0);
        }
    }

    // ── Mutations ─────────────────────────────────────────────────────────

    /**
     * Creates a new maintenance ticket in OPEN status.
     *
     * @param dto ticket data
     * @return generated ticket ID
     * @throws SQLException on DB error
     */
    public long create(MaintenanceTicketDto dto) throws SQLException {
        String sql = "INSERT INTO maintenance_tickets (equipment_id, reporter_id, description, priority) "
                + "VALUES (?, ?, ?, ?)";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, dto.getEquipmentId());
            ps.setLong(2, dto.getReporterId());
            ps.setString(3, dto.getDescription());
            ps.setString(4, dto.getPriority() != null
                    ? dto.getPriority().name() : MaintenancePriority.MEDIUM.name());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                long id = keys.getLong(1);
                LOG.info("Created maintenance ticket id={} equipmentId={}", id, dto.getEquipmentId());
                return id;
            }
        }
    }

    /**
     * Assigns a maintenance ticket to a user (transitions to ASSIGNED).
     *
     * @param ticketId   ticket ID
     * @param assigneeId user ID to assign to
     * @throws SQLException on DB error
     */
    public void assign(long ticketId, long assigneeId) throws SQLException {
        String sql = "UPDATE maintenance_tickets SET assignee_id = ?, status = 'ASSIGNED' "
                + "WHERE id = ? AND status IN ('OPEN', 'ASSIGNED')";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, assigneeId);
            ps.setLong(2, ticketId);
            ps.executeUpdate();
        }
    }

    /**
     * Updates the status of a ticket.
     *
     * @param ticketId ticket ID
     * @param status   new status
     * @throws SQLException on DB error
     */
    public void updateStatus(long ticketId, MaintenanceStatus status) throws SQLException {
        String sql = "UPDATE maintenance_tickets SET status = ? WHERE id = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setLong(2, ticketId);
            ps.executeUpdate();
        }
    }

    /**
     * Records the resolution of a ticket.
     *
     * @param ticketId        ticket ID
     * @param resolutionNotes how the issue was resolved
     * @param repairCost      repair cost (may be null)
     * @throws SQLException on DB error
     */
    public void resolve(long ticketId, String resolutionNotes, BigDecimal repairCost) throws SQLException {
        String sql = "UPDATE maintenance_tickets SET status = 'RESOLVED', "
                + "resolved_at = UTC_TIMESTAMP(), resolution_notes = ?, repair_cost = ? "
                + "WHERE id = ? AND status IN ('OPEN', 'ASSIGNED', 'IN_PROGRESS')";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, resolutionNotes);
            if (repairCost != null) {
                ps.setBigDecimal(2, repairCost);
            } else {
                ps.setNull(2, Types.DECIMAL);
            }
            ps.setLong(3, ticketId);
            int rows = ps.executeUpdate();
            if (rows == 0) {
                throw new SQLException("Ticket " + ticketId
                        + " cannot be resolved from its current state.");
            }
        }
    }

    /**
     * Closes a resolved ticket.
     *
     * @param ticketId ticket ID
     * @throws SQLException on DB error
     */
    public void close(long ticketId) throws SQLException {
        String sql = "UPDATE maintenance_tickets SET status = 'CLOSED' "
                + "WHERE id = ? AND status = 'RESOLVED'";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, ticketId);
            int rows = ps.executeUpdate();
            if (rows == 0) {
                throw new SQLException("Ticket " + ticketId + " must be RESOLVED before it can be closed.");
            }
        }
    }

    // ── Internal ──────────────────────────────────────────────────────────

    private String baseSql() {
        return "SELECT mt.id, mt.equipment_id, e.name AS equipment_name, e.asset_code, "
                + "mt.reporter_id, ru.full_name AS reporter_name, mt.description, mt.priority, "
                + "mt.assignee_id, au.full_name AS assignee_name, mt.status, "
                + "mt.opened_at, mt.updated_at, mt.resolved_at, mt.resolution_notes, mt.repair_cost "
                + "FROM maintenance_tickets mt "
                + "JOIN equipment e ON mt.equipment_id = e.id "
                + "JOIN users ru ON mt.reporter_id = ru.id "
                + "LEFT JOIN users au ON mt.assignee_id = au.id";
    }

    private MaintenanceTicketDto mapTicket(ResultSet rs) throws SQLException {
        MaintenanceTicketDto dto = new MaintenanceTicketDto();
        dto.setId(rs.getLong("id"));
        dto.setEquipmentId(rs.getLong("equipment_id"));
        dto.setEquipmentName(rs.getString("equipment_name"));
        dto.setAssetCode(rs.getString("asset_code"));
        dto.setReporterId(rs.getLong("reporter_id"));
        dto.setReporterName(rs.getString("reporter_name"));
        dto.setDescription(rs.getString("description"));
        try {
            dto.setPriority(MaintenancePriority.valueOf(rs.getString("priority")));
        } catch (IllegalArgumentException e) {
            dto.setPriority(MaintenancePriority.MEDIUM);
        }
        long assigneeId = rs.getLong("assignee_id");
        if (!rs.wasNull()) {
            dto.setAssigneeId(assigneeId);
            dto.setAssigneeName(rs.getString("assignee_name"));
        }
        try {
            dto.setStatus(MaintenanceStatus.valueOf(rs.getString("status")));
        } catch (IllegalArgumentException e) {
            dto.setStatus(MaintenanceStatus.OPEN);
        }
        dto.setOpenedAt(toLocalDateTime(rs.getTimestamp("opened_at")));
        dto.setUpdatedAt(toLocalDateTime(rs.getTimestamp("updated_at")));
        dto.setResolvedAt(toLocalDateTime(rs.getTimestamp("resolved_at")));
        dto.setResolutionNotes(rs.getString("resolution_notes"));
        dto.setRepairCost(rs.getBigDecimal("repair_cost"));
        return dto;
    }

    private LocalDateTime toLocalDateTime(Timestamp ts) {
        return ts != null ? ts.toLocalDateTime() : null;
    }
}
