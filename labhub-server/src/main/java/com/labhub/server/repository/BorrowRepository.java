package com.labhub.server.repository;

import com.labhub.common.dto.BorrowRequestDto;
import com.labhub.common.dto.BorrowRequestDto.BorrowItemDto;
import com.labhub.common.dto.PagedResult;
import com.labhub.common.enums.BorrowStatus;
import com.labhub.common.enums.EquipmentCondition;
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
 * Data access layer for equipment borrow requests and their line items.
 *
 * <p>Multi-item requests are stored across two tables (borrow_requests + borrow_items).
 * Mutations that change both tables use transactions.
 */
public class BorrowRepository {

    private static final Logger LOG = LoggerFactory.getLogger(BorrowRepository.class);

    private final DatabaseManager db;

    /**
     * @param db database manager
     */
    public BorrowRepository(DatabaseManager db) {
        this.db = db;
    }

    // ── Queries ───────────────────────────────────────────────────────────

    /**
     * Finds a borrow request by its ID, including all line items.
     *
     * @param id request ID
     * @return borrow request DTO with items, or empty
     */
    public Optional<BorrowRequestDto> findById(long id) {
        String sql = headerSql() + " WHERE br.id = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    BorrowRequestDto dto = mapHeader(rs);
                    dto.setItems(findItems(conn, id));
                    return Optional.of(dto);
                }
            }
        } catch (SQLException e) {
            LOG.error("Error finding borrow request {}: {}", id, e.getMessage(), e);
        }
        return Optional.empty();
    }

    /**
     * Returns paginated borrow requests with optional filters.
     *
     * @param requesterId filter by requester (null = all)
     * @param status      filter by status (null = all)
     * @param page        1-based page
     * @param pageSize    items per page
     * @return paged results (items list omitted for performance; use findById for detail)
     */
    public PagedResult<BorrowRequestDto> findAll(Long requesterId, BorrowStatus status,
                                                  int page, int pageSize) {
        StringBuilder where = new StringBuilder(" WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (requesterId != null) {
            where.append(" AND br.requester_id = ?");
            params.add(requesterId);
        }
        if (status != null) {
            where.append(" AND br.status = ?");
            params.add(status.name());
        }

        String countSql = "SELECT COUNT(*) FROM borrow_requests br" + where;
        String dataSql  = headerSql() + where + " ORDER BY br.created_at DESC LIMIT ? OFFSET ?";

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

            List<BorrowRequestDto> items = new ArrayList<>();
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
                        BorrowRequestDto dto = mapHeader(rs);
                        // Load items for each row (N+1 acceptable for page sizes ≤25)
                        dto.setItems(findItems(conn, dto.getId()));
                        items.add(dto);
                    }
                }
            }
            return new PagedResult<>(items, page, pageSize, total);
        } catch (SQLException e) {
            LOG.error("Error listing borrow requests: {}", e.getMessage(), e);
            return new PagedResult<>(List.of(), page, pageSize, 0);
        }
    }

    /**
     * Returns all ACTIVE borrow requests that are past their expected return date.
     * Used by the overdue-detection scheduler.
     *
     * @return overdue borrow request DTOs
     */
    public List<BorrowRequestDto> findOverdue() {
        String sql = headerSql()
                + " WHERE br.status = 'ACTIVE' AND br.expected_return_at < UTC_TIMESTAMP()";
        List<BorrowRequestDto> result = new ArrayList<>();
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(mapHeader(rs));
            }
        } catch (SQLException e) {
            LOG.error("Error finding overdue borrows: {}", e.getMessage(), e);
        }
        return result;
    }

    // ── Mutations ─────────────────────────────────────────────────────────

    /**
     * Creates a new borrow request with its equipment line items in a transaction.
     * Equipment IDs in the item list must all be in AVAILABLE status; this is
     * validated by the service layer before calling this method.
     *
     * @param dto       request header data
     * @param equipmentIds list of equipment IDs to borrow
     * @return generated request ID
     * @throws SQLException on constraint violation or DB error
     */
    public long create(BorrowRequestDto dto, List<Long> equipmentIds) throws SQLException {
        String headerSql = "INSERT INTO borrow_requests (requester_id, purpose, expected_return_at) "
                + "VALUES (?, ?, ?)";
        String itemSql = "INSERT INTO borrow_items (borrow_request_id, equipment_id) VALUES (?, ?)";

        try (Connection conn = db.getConnection()) {
            conn.setAutoCommit(false);
            try {
                long requestId;
                try (PreparedStatement ps = conn.prepareStatement(
                        headerSql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setLong(1, dto.getRequesterId());
                    ps.setString(2, dto.getPurpose());
                    ps.setTimestamp(3, Timestamp.valueOf(dto.getExpectedReturnAt()));
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        keys.next();
                        requestId = keys.getLong(1);
                    }
                }
                try (PreparedStatement ps = conn.prepareStatement(itemSql)) {
                    for (Long eqId : equipmentIds) {
                        ps.setLong(1, requestId);
                        ps.setLong(2, eqId);
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
                conn.commit();
                LOG.info("Created borrow request id={}, items={}", requestId, equipmentIds.size());
                return requestId;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    /**
     * Records the approval of a borrow request.
     *
     * @param requestId   borrow request ID
     * @param processedBy approver user ID
     * @param note        optional approval note
     * @throws SQLException on DB error
     */
    public void approve(long requestId, long processedBy, String note) throws SQLException {
        String sql = "UPDATE borrow_requests SET status = 'APPROVED', processed_by_id = ?, "
                + "decision_note = ? WHERE id = ? AND status = 'PENDING'";
        executeDecision(sql, processedBy, note, requestId);
    }

    /**
     * Records the rejection of a borrow request.
     *
     * @param requestId   borrow request ID
     * @param processedBy rejector user ID
     * @param note        rejection reason
     * @throws SQLException on DB error
     */
    public void reject(long requestId, long processedBy, String note) throws SQLException {
        String sql = "UPDATE borrow_requests SET status = 'REJECTED', processed_by_id = ?, "
                + "decision_note = ? WHERE id = ? AND status = 'PENDING'";
        executeDecision(sql, processedBy, note, requestId);
    }

    /**
     * Cancels a PENDING or APPROVED borrow request (before checkout).
     *
     * @param requestId   borrow request ID
     * @param processedBy user performing the cancellation
     * @throws SQLException on DB error, or if the request is not in a cancellable state
     */
    public void cancel(long requestId, long processedBy) throws SQLException {
        String sql = "UPDATE borrow_requests SET status = 'CANCELLED', processed_by_id = ? "
                + "WHERE id = ? AND status IN ('PENDING', 'APPROVED')";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, processedBy);
            ps.setLong(2, requestId);
            int rows = ps.executeUpdate();
            if (rows == 0) {
                throw new SQLException("Borrow request " + requestId + " is not in PENDING state.");
            }
        }
    }

    /**
     * Records the physical equipment checkout (APPROVED → ACTIVE).
     * Also records condition at checkout for each item.
     *
     * @param requestId          borrow request ID
     * @param processedBy        manager performing checkout
     * @param itemConditions     map of equipmentId → condition at checkout
     * @throws SQLException on DB error
     */
    public void checkout(long requestId, long processedBy,
                         java.util.Map<Long, EquipmentCondition> itemConditions) throws SQLException {
        String reqSql = "UPDATE borrow_requests SET status = 'ACTIVE', checked_out_at = UTC_TIMESTAMP(), "
                + "processed_by_id = ? WHERE id = ? AND status = 'APPROVED'";
        String itemSql = "UPDATE borrow_items SET condition_at_checkout = ? "
                + "WHERE borrow_request_id = ? AND equipment_id = ?";
        try (Connection conn = db.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement ps = conn.prepareStatement(reqSql)) {
                    ps.setLong(1, processedBy);
                    ps.setLong(2, requestId);
                    int rows = ps.executeUpdate();
                    if (rows == 0) {
                        throw new SQLException("Borrow request " + requestId
                                + " is not in APPROVED state.");
                    }
                }
                if (itemConditions != null) {
                    try (PreparedStatement ps = conn.prepareStatement(itemSql)) {
                        for (var entry : itemConditions.entrySet()) {
                            ps.setString(1, entry.getValue().name());
                            ps.setLong(2, requestId);
                            ps.setLong(3, entry.getKey());
                            ps.addBatch();
                        }
                        ps.executeBatch();
                    }
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    /**
     * Records the return of all items in a borrow request (ACTIVE/OVERDUE → RETURNED).
     * Also records return conditions for each item.
     *
     * @param requestId      borrow request ID
     * @param processedBy    manager processing the return
     * @param itemConditions map of equipmentId → condition at return
     * @throws SQLException on DB error
     */
    public void returnRequest(long requestId, long processedBy,
                               java.util.Map<Long, EquipmentCondition> itemConditions) throws SQLException {
        String reqSql = "UPDATE borrow_requests SET status = 'RETURNED', "
                + "actual_return_at = UTC_TIMESTAMP(), processed_by_id = ? "
                + "WHERE id = ? AND status IN ('ACTIVE', 'OVERDUE')";
        String itemSql = "UPDATE borrow_items SET condition_at_return = ? "
                + "WHERE borrow_request_id = ? AND equipment_id = ?";
        try (Connection conn = db.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement ps = conn.prepareStatement(reqSql)) {
                    ps.setLong(1, processedBy);
                    ps.setLong(2, requestId);
                    int rows = ps.executeUpdate();
                    if (rows == 0) {
                        throw new SQLException("Borrow request " + requestId
                                + " cannot be returned in its current state.");
                    }
                }
                if (itemConditions != null) {
                    try (PreparedStatement ps = conn.prepareStatement(itemSql)) {
                        for (var entry : itemConditions.entrySet()) {
                            ps.setString(1, entry.getValue().name());
                            ps.setLong(2, requestId);
                            ps.setLong(3, entry.getKey());
                            ps.addBatch();
                        }
                        ps.executeBatch();
                    }
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    /**
     * Marks overdue active requests as OVERDUE status.
     *
     * @return number of records updated
     * @throws SQLException on DB error
     */
    public int markOverdue() throws SQLException {
        String sql = "UPDATE borrow_requests SET status = 'OVERDUE' "
                + "WHERE status = 'ACTIVE' AND expected_return_at < UTC_TIMESTAMP()";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            return ps.executeUpdate();
        }
    }

    // ── Internal ──────────────────────────────────────────────────────────

    private List<BorrowItemDto> findItems(Connection conn, long requestId) throws SQLException {
        String sql = "SELECT bi.id, bi.equipment_id, e.name AS equipment_name, e.asset_code, "
                + "bi.condition_at_checkout, bi.condition_at_return, bi.notes "
                + "FROM borrow_items bi JOIN equipment e ON bi.equipment_id = e.id "
                + "WHERE bi.borrow_request_id = ?";
        List<BorrowItemDto> items = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, requestId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    BorrowItemDto item = new BorrowItemDto();
                    item.setId(rs.getLong("id"));
                    item.setEquipmentId(rs.getLong("equipment_id"));
                    item.setEquipmentName(rs.getString("equipment_name"));
                    item.setAssetCode(rs.getString("asset_code"));
                    item.setNotes(rs.getString("notes"));
                    String cco = rs.getString("condition_at_checkout");
                    if (cco != null) {
                        try {
                            item.setConditionAtCheckout(EquipmentCondition.valueOf(cco));
                        } catch (IllegalArgumentException ignored) { }
                    }
                    String cret = rs.getString("condition_at_return");
                    if (cret != null) {
                        try {
                            item.setConditionAtReturn(EquipmentCondition.valueOf(cret));
                        } catch (IllegalArgumentException ignored) { }
                    }
                    items.add(item);
                }
            }
        }
        return items;
    }

    private void executeDecision(String sql, long processedBy, String note,
                                  long requestId) throws SQLException {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, processedBy);
            ps.setString(2, note);
            ps.setLong(3, requestId);
            int rows = ps.executeUpdate();
            if (rows == 0) {
                throw new SQLException("Request " + requestId
                        + " is not in the expected state for this action.");
            }
        }
    }

    private String headerSql() {
        return "SELECT br.id, br.requester_id, ru.full_name AS requester_name, "
                + "br.purpose, br.expected_return_at, br.checked_out_at, br.actual_return_at, "
                + "br.processed_by_id, pu.full_name AS processed_by_name, br.status, "
                + "br.decision_note, br.created_at, br.updated_at "
                + "FROM borrow_requests br "
                + "JOIN users ru ON br.requester_id = ru.id "
                + "LEFT JOIN users pu ON br.processed_by_id = pu.id";
    }

    private BorrowRequestDto mapHeader(ResultSet rs) throws SQLException {
        BorrowRequestDto dto = new BorrowRequestDto();
        dto.setId(rs.getLong("id"));
        dto.setRequesterId(rs.getLong("requester_id"));
        dto.setRequesterName(rs.getString("requester_name"));
        dto.setPurpose(rs.getString("purpose"));
        dto.setExpectedReturnAt(toLocalDateTime(rs.getTimestamp("expected_return_at")));
        dto.setCheckedOutAt(toLocalDateTime(rs.getTimestamp("checked_out_at")));
        dto.setActualReturnAt(toLocalDateTime(rs.getTimestamp("actual_return_at")));
        long procId = rs.getLong("processed_by_id");
        if (!rs.wasNull()) {
            dto.setProcessedById(procId);
            dto.setProcessedByName(rs.getString("processed_by_name"));
        }
        try {
            dto.setStatus(BorrowStatus.valueOf(rs.getString("status")));
        } catch (IllegalArgumentException e) {
            dto.setStatus(BorrowStatus.PENDING);
        }
        dto.setDecisionNote(rs.getString("decision_note"));
        dto.setCreatedAt(toLocalDateTime(rs.getTimestamp("created_at")));
        dto.setUpdatedAt(toLocalDateTime(rs.getTimestamp("updated_at")));
        return dto;
    }

    private LocalDateTime toLocalDateTime(Timestamp ts) {
        return ts != null ? ts.toLocalDateTime() : null;
    }
}
