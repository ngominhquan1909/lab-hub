package com.labhub.server.repository;

import com.labhub.common.dto.EquipmentDto;
import com.labhub.common.dto.PagedResult;
import com.labhub.common.enums.EquipmentCondition;
import com.labhub.common.enums.EquipmentStatus;
import com.labhub.server.config.DatabaseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
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
 * Data access layer for equipment records.
 */
public class EquipmentRepository {

    private static final Logger LOG = LoggerFactory.getLogger(EquipmentRepository.class);

    private final DatabaseManager db;

    /**
     * @param db database manager
     */
    public EquipmentRepository(DatabaseManager db) {
        this.db = db;
    }

    // ── Queries ───────────────────────────────────────────────────────────

    /**
     * Finds equipment by its internal ID.
     *
     * @param id equipment ID
     * @return equipment DTO or empty
     */
    public Optional<EquipmentDto> findById(long id) {
        String sql = baseSql() + " WHERE e.id = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapEquipment(rs));
                }
            }
        } catch (SQLException e) {
            LOG.error("Error finding equipment {}: {}", id, e.getMessage(), e);
        }
        return Optional.empty();
    }

    /**
     * Returns a paginated list of equipment with optional filters.
     *
     * @param search   filter on name, asset_code, serial_number (null = no filter)
     * @param labId    filter by lab (null = all labs)
     * @param status   filter by status (null = all statuses)
     * @param page     1-based page
     * @param pageSize items per page
     * @return paged equipment DTOs
     */
    public PagedResult<EquipmentDto> findAll(String search, Long labId,
                                              EquipmentStatus status,
                                              int page, int pageSize) {
        StringBuilder where = new StringBuilder(" WHERE e.is_active = 1");
        List<Object> params = new ArrayList<>();

        if (search != null && !search.isBlank()) {
            where.append(" AND (e.name LIKE ? OR e.asset_code LIKE ? OR e.serial_number LIKE ?)");
            String like = "%" + search + "%";
            params.add(like);
            params.add(like);
            params.add(like);
        }
        if (labId != null) {
            where.append(" AND e.lab_id = ?");
            params.add(labId);
        }
        if (status != null) {
            where.append(" AND e.status = ?");
            params.add(status.name());
        }

        String countSql = "SELECT COUNT(*) FROM equipment e" + where;
        String dataSql  = baseSql() + where + " ORDER BY e.name ASC LIMIT ? OFFSET ?";

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

            List<EquipmentDto> items = new ArrayList<>();
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
                        items.add(mapEquipment(rs));
                    }
                }
            }
            return new PagedResult<>(items, page, pageSize, total);
        } catch (SQLException e) {
            LOG.error("Error listing equipment: {}", e.getMessage(), e);
            return new PagedResult<>(List.of(), page, pageSize, 0);
        }
    }

    /**
     * Returns all AVAILABLE equipment in a specific lab.
     * Used for borrow request creation dropdowns.
     *
     * @param labId lab ID
     * @return available equipment list
     */
    public List<EquipmentDto> findAvailableByLab(long labId) {
        String sql = baseSql() + " WHERE e.lab_id = ? AND e.status = 'AVAILABLE' AND e.is_active = 1 ORDER BY e.name";
        List<EquipmentDto> result = new ArrayList<>();
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, labId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapEquipment(rs));
                }
            }
        } catch (SQLException e) {
            LOG.error("Error finding available equipment for lab {}: {}", labId, e.getMessage(), e);
        }
        return result;
    }

    // ── Mutations ─────────────────────────────────────────────────────────

    /**
     * Creates a new equipment record.
     *
     * @param dto equipment data
     * @return generated equipment ID
     * @throws SQLException on constraint violations
     */
    public long create(EquipmentDto dto) throws SQLException {
        String sql = "INSERT INTO equipment (asset_code, name, category_id, manufacturer, model, "
                + "serial_number, lab_id, acquisition_date, purchase_cost, condition_state, notes) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, dto.getAssetCode());
            ps.setString(2, dto.getName());
            ps.setLong(3, dto.getCategoryId());
            ps.setString(4, dto.getManufacturer());
            ps.setString(5, dto.getModel());
            ps.setString(6, dto.getSerialNumber());
            if (dto.getLabId() != null) {
                ps.setLong(7, dto.getLabId());
            } else {
                ps.setNull(7, Types.BIGINT);
            }
            if (dto.getAcquisitionDate() != null) {
                ps.setDate(8, Date.valueOf(dto.getAcquisitionDate()));
            } else {
                ps.setNull(8, Types.DATE);
            }
            if (dto.getPurchaseCost() != null) {
                ps.setBigDecimal(9, dto.getPurchaseCost());
            } else {
                ps.setNull(9, Types.DECIMAL);
            }
            ps.setString(10, dto.getCondition() != null
                    ? dto.getCondition().name() : EquipmentCondition.GOOD.name());
            ps.setString(11, dto.getNotes());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                long id = keys.getLong(1);
                LOG.info("Created equipment id={} assetCode='{}'", id, dto.getAssetCode());
                return id;
            }
        }
    }

    /**
     * Updates mutable equipment fields.
     *
     * @param id  equipment ID
     * @param dto updated data
     * @throws SQLException on DB error
     */
    public void update(long id, EquipmentDto dto) throws SQLException {
        String sql = "UPDATE equipment SET name = ?, category_id = ?, manufacturer = ?, model = ?, "
                + "serial_number = ?, lab_id = ?, acquisition_date = ?, purchase_cost = ?, "
                + "condition_state = ?, notes = ? WHERE id = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, dto.getName());
            ps.setLong(2, dto.getCategoryId());
            ps.setString(3, dto.getManufacturer());
            ps.setString(4, dto.getModel());
            ps.setString(5, dto.getSerialNumber());
            if (dto.getLabId() != null) {
                ps.setLong(6, dto.getLabId());
            } else {
                ps.setNull(6, Types.BIGINT);
            }
            if (dto.getAcquisitionDate() != null) {
                ps.setDate(7, Date.valueOf(dto.getAcquisitionDate()));
            } else {
                ps.setNull(7, Types.DATE);
            }
            if (dto.getPurchaseCost() != null) {
                ps.setBigDecimal(8, dto.getPurchaseCost());
            } else {
                ps.setNull(8, Types.DECIMAL);
            }
            ps.setString(9, dto.getCondition() != null ? dto.getCondition().name() : null);
            ps.setString(10, dto.getNotes());
            ps.setLong(11, id);
            ps.executeUpdate();
        }
    }

    /**
     * Updates the operational status of a piece of equipment.
     *
     * @param id     equipment ID
     * @param status new status
     * @throws SQLException on DB error
     */
    public void setStatus(long id, EquipmentStatus status) throws SQLException {
        String sql = "UPDATE equipment SET status = ? WHERE id = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setLong(2, id);
            ps.executeUpdate();
        }
    }

    /**
     * Soft-deletes equipment by setting is_active = 0.
     *
     * @param id equipment ID
     * @throws SQLException on DB error
     */
    public void deactivate(long id) throws SQLException {
        String sql = "UPDATE equipment SET is_active = 0 WHERE id = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }

    // ── Internal ──────────────────────────────────────────────────────────

    private String baseSql() {
        return "SELECT e.id, e.asset_code, e.name, e.category_id, "
                + "ec.name AS category_name, e.manufacturer, e.model, e.serial_number, "
                + "e.lab_id, l.name AS lab_name, e.acquisition_date, e.purchase_cost, "
                + "e.condition_state, e.status, e.notes, e.is_active, e.created_at, e.updated_at "
                + "FROM equipment e "
                + "LEFT JOIN equipment_categories ec ON e.category_id = ec.id "
                + "LEFT JOIN labs l ON e.lab_id = l.id";
    }

    private EquipmentDto mapEquipment(ResultSet rs) throws SQLException {
        EquipmentDto dto = new EquipmentDto();
        dto.setId(rs.getLong("id"));
        dto.setAssetCode(rs.getString("asset_code"));
        dto.setName(rs.getString("name"));
        dto.setCategoryId(rs.getLong("category_id"));
        dto.setCategoryName(rs.getString("category_name"));
        dto.setManufacturer(rs.getString("manufacturer"));
        dto.setModel(rs.getString("model"));
        dto.setSerialNumber(rs.getString("serial_number"));
        long labId = rs.getLong("lab_id");
        if (!rs.wasNull()) {
            dto.setLabId(labId);
        }
        dto.setLabName(rs.getString("lab_name"));
        java.sql.Date acqDate = rs.getDate("acquisition_date");
        if (acqDate != null) {
            dto.setAcquisitionDate(acqDate.toLocalDate());
        }
        BigDecimal cost = rs.getBigDecimal("purchase_cost");
        dto.setPurchaseCost(cost);
        try {
            dto.setCondition(EquipmentCondition.valueOf(rs.getString("condition_state")));
        } catch (IllegalArgumentException | NullPointerException e) {
            dto.setCondition(EquipmentCondition.GOOD);
        }
        try {
            dto.setStatus(EquipmentStatus.valueOf(rs.getString("status")));
        } catch (IllegalArgumentException | NullPointerException e) {
            dto.setStatus(EquipmentStatus.AVAILABLE);
        }
        dto.setNotes(rs.getString("notes"));
        dto.setActive(rs.getBoolean("is_active"));
        dto.setCreatedAt(toLocalDateTime(rs.getTimestamp("created_at")));
        dto.setUpdatedAt(toLocalDateTime(rs.getTimestamp("updated_at")));
        return dto;
    }

    private LocalDateTime toLocalDateTime(Timestamp ts) {
        return ts != null ? ts.toLocalDateTime() : null;
    }
}
