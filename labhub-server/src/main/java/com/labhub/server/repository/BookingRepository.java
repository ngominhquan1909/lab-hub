package com.labhub.server.repository;

import com.labhub.common.dto.BookingDto;
import com.labhub.common.dto.PagedResult;
import com.labhub.common.enums.BookingStatus;
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
 * Data access layer for lab booking requests.
 *
 * <p>Conflict detection is implemented in SQL to ensure atomicity under concurrent requests.
 * Two bookings conflict when their time intervals overlap for the same lab and both are
 * in an APPROVED or PENDING state.
 */
public class BookingRepository {

    private static final Logger LOG = LoggerFactory.getLogger(BookingRepository.class);

    private final DatabaseManager db;

    public BookingRepository(DatabaseManager db) {
        this.db = db;
    }

    /**
     * Checks whether a proposed time slot conflicts with any existing APPROVED booking
     * for the given lab. PENDING and REJECTED bookings do not block slots.
     *
     * <p>Two intervals [s1, e1) and [s2, e2) overlap when: s1 < e2 AND s2 < e1.
     *
     * @param labId     lab ID
     * @param start     proposed start time
     * @param end       proposed end time
     * @param excludeId booking ID to exclude (for update/reschedule flows; use 0 to skip)
     * @return true if there is a conflict
     */
    public boolean hasConflict(long labId, LocalDateTime start, LocalDateTime end, long excludeId) {
        String sql = "SELECT COUNT(*) FROM bookings "
                + "WHERE lab_id = ? AND status = 'APPROVED' "
                + "AND start_time < ? AND end_time > ? "
                + "AND id != ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, labId);
            ps.setTimestamp(2, Timestamp.valueOf(end));
            ps.setTimestamp(3, Timestamp.valueOf(start));
            ps.setLong(4, excludeId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            LOG.error("Error checking booking conflict: {}", e.getMessage(), e);
            // Fail safe - treat as conflict to prevent double-booking
            return true;
        }
    }

    /**
     * Finds a booking by its ID.
     *
     * @param id booking ID
     * @return booking DTO or empty
     */
    public Optional<BookingDto> findById(long id) {
        String sql = baseSql() + " WHERE b.id = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapBooking(rs));
                }
            }
        } catch (SQLException e) {
            LOG.error("Error finding booking {}: {}", id, e.getMessage(), e);
        }
        return Optional.empty();
    }

    /**
     * Returns paginated bookings with optional filters.
     *
     * @param requesterId filter by requester (null = all)
     * @param labId       filter by lab (null = all)
     * @param status      filter by status (null = all)
     * @param page        1-based page
     * @param pageSize    items per page
     * @return paged bookings
     */
    public PagedResult<BookingDto> findAll(Long requesterId, Long labId,
                                            BookingStatus status,
                                            int page, int pageSize) {
        StringBuilder where = new StringBuilder(" WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (requesterId != null) {
            where.append(" AND b.requester_id = ?");
            params.add(requesterId);
        }
        if (labId != null) {
            where.append(" AND b.lab_id = ?");
            params.add(labId);
        }
        if (status != null) {
            where.append(" AND b.status = ?");
            params.add(status.name());
        }

        String countSql = "SELECT COUNT(*) FROM bookings b" + where;
        String dataSql  = baseSql() + where + " ORDER BY b.start_time DESC LIMIT ? OFFSET ?";

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

            List<BookingDto> items = new ArrayList<>();
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
                        items.add(mapBooking(rs));
                    }
                }
            }
            return new PagedResult<>(items, page, pageSize, total);
        } catch (SQLException e) {
            LOG.error("Error listing bookings: {}", e.getMessage(), e);
            return new PagedResult<>(List.of(), page, pageSize, 0);
        }
    }

    /**
     * Returns approved bookings for a lab within a date range (for schedule view).
     *
     * @param labId lab ID (0 = all labs)
     * @param from  range start
     * @param to    range end
     * @return list of approved bookings
     */
    public List<BookingDto> findSchedule(long labId, LocalDateTime from, LocalDateTime to) {
        StringBuilder sql = new StringBuilder(baseSql());
        sql.append(" WHERE b.status = 'APPROVED' AND b.start_time >= ? AND b.end_time <= ?");
        if (labId > 0) {
            sql.append(" AND b.lab_id = ?");
        }
        sql.append(" ORDER BY b.start_time ASC");

        List<BookingDto> result = new ArrayList<>();
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            ps.setTimestamp(1, Timestamp.valueOf(from));
            ps.setTimestamp(2, Timestamp.valueOf(to));
            if (labId > 0) {
                ps.setLong(3, labId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapBooking(rs));
                }
            }
        } catch (SQLException e) {
            LOG.error("Error fetching schedule: {}", e.getMessage(), e);
        }
        return result;
    }

    /**
     * Creates a new booking in PENDING status.
     *
     * @param dto booking data
     * @return generated booking ID
     * @throws SQLException on DB error
     */
    public long create(BookingDto dto) throws SQLException {
        String sql = "INSERT INTO bookings (lab_id, requester_id, start_time, end_time, purpose, attendee_count, notes) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, dto.getLabId());
            ps.setLong(2, dto.getRequesterId());
            ps.setTimestamp(3, Timestamp.valueOf(dto.getStartTime()));
            ps.setTimestamp(4, Timestamp.valueOf(dto.getEndTime()));
            ps.setString(5, dto.getPurpose());
            ps.setInt(6, dto.getAttendeeCount() != null ? dto.getAttendeeCount() : 1);
            ps.setString(7, dto.getNotes());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                long id = keys.getLong(1);
                LOG.info("Created booking id={} labId={}", id, dto.getLabId());
                return id;
            }
        }
    }

    /**
     * Records an approval decision on a booking.
     *
     * @param bookingId   booking ID
     * @param deciderId   ID of the approver
     * @param note        optional approval note
     * @throws SQLException on DB error
     */
    public void approve(long bookingId, long deciderId, String note) throws SQLException {
        updateDecision(bookingId, BookingStatus.APPROVED, deciderId, note);
    }

    /**
     * Records a rejection decision on a booking.
     *
     * @param bookingId booking ID
     * @param deciderId ID of the rejector
     * @param note      required rejection reason
     * @throws SQLException on DB error
     */
    public void reject(long bookingId, long deciderId, String note) throws SQLException {
        updateDecision(bookingId, BookingStatus.REJECTED, deciderId, note);
    }

    /**
     * Cancels a booking.
     *
     * @param bookingId booking ID
     * @throws SQLException on DB error
     */
    public void cancel(long bookingId) throws SQLException {
        String sql = "UPDATE bookings SET status = 'CANCELLED' WHERE id = ? AND status IN ('PENDING', 'APPROVED')";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, bookingId);
            int rows = ps.executeUpdate();
            if (rows == 0) {
                throw new SQLException("Booking " + bookingId + " cannot be cancelled in its current state.");
            }
        }
    }

    private void updateDecision(long bookingId, BookingStatus status,
                                long deciderId, String note) throws SQLException {
        String sql = "UPDATE bookings SET status = ?, decided_by_id = ?, decided_at = UTC_TIMESTAMP(), "
                + "decision_note = ? WHERE id = ? AND status = 'PENDING'";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setLong(2, deciderId);
            ps.setString(3, note);
            ps.setLong(4, bookingId);
            int rows = ps.executeUpdate();
            if (rows == 0) {
                throw new SQLException("Booking " + bookingId
                        + " is not in PENDING state; decision not applied.");
            }
        }
    }

    private String baseSql() {
        return "SELECT b.id, b.lab_id, l.name AS lab_name, l.lab_code, "
                + "b.requester_id, ru.full_name AS requester_name, "
                + "b.start_time, b.end_time, b.purpose, b.attendee_count, b.notes, b.status, "
                + "b.decided_by_id, du.full_name AS decided_by_name, b.decided_at, b.decision_note, "
                + "b.created_at, b.updated_at "
                + "FROM bookings b "
                + "JOIN labs l ON b.lab_id = l.id "
                + "JOIN users ru ON b.requester_id = ru.id "
                + "LEFT JOIN users du ON b.decided_by_id = du.id";
    }

    private BookingDto mapBooking(ResultSet rs) throws SQLException {
        BookingDto dto = new BookingDto();
        dto.setId(rs.getLong("id"));
        dto.setLabId(rs.getLong("lab_id"));
        dto.setLabName(rs.getString("lab_name"));
        dto.setLabCode(rs.getString("lab_code"));
        dto.setRequesterId(rs.getLong("requester_id"));
        dto.setRequesterName(rs.getString("requester_name"));
        dto.setStartTime(toLocalDateTime(rs.getTimestamp("start_time")));
        dto.setEndTime(toLocalDateTime(rs.getTimestamp("end_time")));
        dto.setPurpose(rs.getString("purpose"));
        dto.setAttendeeCount(rs.getInt("attendee_count"));
        dto.setNotes(rs.getString("notes"));
        try {
            dto.setStatus(BookingStatus.valueOf(rs.getString("status")));
        } catch (IllegalArgumentException e) {
            dto.setStatus(BookingStatus.PENDING);
        }
        long deciderId = rs.getLong("decided_by_id");
        if (!rs.wasNull()) {
            dto.setDecidedById(deciderId);
            dto.setDecidedByName(rs.getString("decided_by_name"));
        }
        dto.setDecidedAt(toLocalDateTime(rs.getTimestamp("decided_at")));
        dto.setDecisionNote(rs.getString("decision_note"));
        dto.setCreatedAt(toLocalDateTime(rs.getTimestamp("created_at")));
        dto.setUpdatedAt(toLocalDateTime(rs.getTimestamp("updated_at")));
        return dto;
    }

    private LocalDateTime toLocalDateTime(Timestamp ts) {
        return ts != null ? ts.toLocalDateTime() : null;
    }
}
