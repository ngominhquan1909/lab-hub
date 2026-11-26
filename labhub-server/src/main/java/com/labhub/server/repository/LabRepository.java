package com.labhub.server.repository;

import com.labhub.common.dto.LabDto;
import com.labhub.common.dto.PagedResult;
import com.labhub.common.enums.LabStatus;
import com.labhub.server.config.DatabaseManager;
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
 * Data access layer for laboratory records.
 */
public class LabRepository {

    private static final Logger LOG = LoggerFactory.getLogger(LabRepository.class);

    private final DatabaseManager db;

    /**
     * @param db database manager
     */
    public LabRepository(DatabaseManager db) {
        this.db = db;
    }

    // ── Queries ───────────────────────────────────────────────────────────

    /**
     * Finds a lab by its internal ID.
     *
     * @param id lab ID
     * @return lab DTO or empty
     */
    public Optional<LabDto> findById(long id) {
        String sql = baseSql() + " WHERE l.id = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapLab(rs));
                }
            }
        } catch (SQLException e) {
            LOG.error("Error finding lab {}: {}", id, e.getMessage(), e);
        }
        return Optional.empty();
    }

    /**
     * Finds a lab by its unique code.
     *
     * @param labCode e.g. "CS-LAB-A"
     * @return lab DTO or empty
     */
    public Optional<LabDto> findByCode(String labCode) {
        String sql = baseSql() + " WHERE l.lab_code = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, labCode);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapLab(rs));
                }
            }
        } catch (SQLException e) {
            LOG.error("Error finding lab by code '{}': {}", labCode, e.getMessage(), e);
        }
        return Optional.empty();
    }

    /**
     * Returns a paginated list of labs with optional status and search filters.
     *
     * @param search filter on name, code, building (null = no filter)
     * @param status filter on status (null = all statuses)
     * @param page   1-based page number
     * @param size   items per page
     * @return paged labs
     */
    public PagedResult<LabDto> findAll(String search, LabStatus status, int page, int size) {
        StringBuilder where = new StringBuilder();
        List<Object> params = new ArrayList<>();

        if (search != null && !search.isBlank()) {
            where.append(" WHERE (l.name LIKE ? OR l.lab_code LIKE ? OR l.building LIKE ?)");
            String like = "%" + search + "%";
            params.add(like);
            params.add(like);
            params.add(like);
        }
        if (status != null) {
            where.append(where.isEmpty() ? " WHERE" : " AND");
            where.append(" l.status = ?");
            params.add(status.name());
        }

        String countSql = "SELECT COUNT(*) FROM labs l" + where;
        String dataSql  = baseSql() + where + " ORDER BY l.name ASC LIMIT ? OFFSET ?";

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

            List<LabDto> items = new ArrayList<>();
            int offset = (page - 1) * size;
            try (PreparedStatement ps = conn.prepareStatement(dataSql)) {
                int idx = 1;
                for (Object p : params) {
                    ps.setObject(idx++, p);
                }
                ps.setInt(idx++, size);
                ps.setInt(idx,   offset);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        items.add(mapLab(rs));
                    }
                }
            }
            return new PagedResult<>(items, page, size, total);
        } catch (SQLException e) {
            LOG.error("Error listing labs: {}", e.getMessage(), e);
            return new PagedResult<>(List.of(), page, size, 0);
        }
    }

    /**
     * Returns all ACTIVE labs (used for dropdowns).
     *
     * @return list of active lab DTOs
     */
    public List<LabDto> findAllActive() {
        String sql = baseSql() + " WHERE l.status = 'ACTIVE' ORDER BY l.name ASC";
        List<LabDto> result = new ArrayList<>();
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(mapLab(rs));
            }
        } catch (SQLException e) {
            LOG.error("Error listing active labs: {}", e.getMessage(), e);
        }
        return result;
    }

    // ── Mutations ─────────────────────────────────────────────────────────

    /**
     * Creates a new lab record.
     *
     * @param dto lab data (id is ignored)
     * @return generated lab ID
     * @throws SQLException on constraint violations
     */
    public long create(LabDto dto) throws SQLException {
        String sql = "INSERT INTO labs (lab_code, name, building, floor, capacity, room_type, description, status) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, dto.getLabCode().toUpperCase());
            ps.setString(2, dto.getName());
            ps.setString(3, dto.getBuilding());
            ps.setInt(4, dto.getFloor() != null ? dto.getFloor() : 1);
            ps.setInt(5, dto.getCapacity());
            ps.setString(6, dto.getRoomType());
            ps.setString(7, dto.getDescription());
            ps.setString(8, LabStatus.ACTIVE.name());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                long id = keys.getLong(1);
                LOG.info("Created lab id={} code='{}'", id, dto.getLabCode());
                return id;
            }
        }
    }

    /**
     * Updates an existing lab's mutable fields.
     *
     * @param id  lab ID
     * @param dto updated data
     * @throws SQLException on DB error
     */
    public void update(long id, LabDto dto) throws SQLException {
        String sql = "UPDATE labs SET name = ?, building = ?, floor = ?, capacity = ?, "
                + "room_type = ?, description = ? WHERE id = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, dto.getName());
            ps.setString(2, dto.getBuilding());
            ps.setInt(3, dto.getFloor() != null ? dto.getFloor() : 1);
            ps.setInt(4, dto.getCapacity());
            ps.setString(5, dto.getRoomType());
            ps.setString(6, dto.getDescription());
            ps.setLong(7, id);
            ps.executeUpdate();
            LOG.info("Updated lab id={}", id);
        }
    }

    /**
     * Changes a lab's operational status.
     *
     * @param id     lab ID
     * @param status new status
     * @throws SQLException on DB error
     */
    public void setStatus(long id, LabStatus status) throws SQLException {
        String sql = "UPDATE labs SET status = ? WHERE id = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setLong(2, id);
            ps.executeUpdate();
        }
    }

    // ── Internal ──────────────────────────────────────────────────────────

    private String baseSql() {
        return "SELECT l.id, l.lab_code, l.name, l.building, l.floor, l.capacity, "
                + "l.room_type, l.description, l.status, l.created_at, l.updated_at, "
                + "(SELECT COUNT(*) FROM equipment e WHERE e.lab_id = l.id AND e.is_active = 1) AS equipment_count "
                + "FROM labs l";
    }

    private LabDto mapLab(ResultSet rs) throws SQLException {
        LabDto dto = new LabDto();
        dto.setId(rs.getLong("id"));
        dto.setLabCode(rs.getString("lab_code"));
        dto.setName(rs.getString("name"));
        dto.setBuilding(rs.getString("building"));
        dto.setFloor(rs.getInt("floor"));
        dto.setCapacity(rs.getInt("capacity"));
        dto.setRoomType(rs.getString("room_type"));
        dto.setDescription(rs.getString("description"));
        dto.setEquipmentCount(rs.getInt("equipment_count"));
        try {
            dto.setStatus(LabStatus.valueOf(rs.getString("status")));
        } catch (IllegalArgumentException e) {
            dto.setStatus(LabStatus.INACTIVE);
        }
        dto.setCreatedAt(toLocalDateTime(rs.getTimestamp("created_at")));
        dto.setUpdatedAt(toLocalDateTime(rs.getTimestamp("updated_at")));
        return dto;
    }

    private LocalDateTime toLocalDateTime(Timestamp ts) {
        return ts != null ? ts.toLocalDateTime() : null;
    }
}
