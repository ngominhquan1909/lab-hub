package com.labhub.server.service;

import com.labhub.common.dto.BookingDto;
import com.labhub.common.dto.LabDto;
import com.labhub.common.dto.PagedResult;
import com.labhub.common.dto.UserDto;
import com.labhub.common.enums.BookingStatus;
import com.labhub.common.enums.LabStatus;
import com.labhub.common.enums.UserRole;
import com.labhub.server.repository.BookingRepository;
import com.labhub.server.repository.DashboardRepository;
import com.labhub.server.repository.LabRepository;
import com.labhub.server.config.ServerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Business logic for lab booking operations.
 *
 * <p>Business rules enforced:
 * <ul>
 *   <li>Only ACTIVE labs can be booked.</li>
 *   <li>Start time must be in the future.</li>
 *   <li>End time must be after start time.</li>
 *   <li>Duration must be at least {@code booking.min.duration.minutes}.</li>
 *   <li>Bookings cannot be made more than {@code booking.max.advance.days} days in advance.</li>
 *   <li>Attendee count must not exceed lab capacity.</li>
 *   <li>Conflicting approved bookings are rejected.</li>
 *   <li>Only the requester, a lab manager, or admin can cancel.</li>
 *   <li>Approval/rejection is restricted to LAB_MANAGER and ADMIN roles.</li>
 * </ul>
 */
public class BookingService {

    private static final Logger LOG = LoggerFactory.getLogger(BookingService.class);

    private final BookingRepository bookingRepository;
    private final LabRepository labRepository;
    private final DashboardRepository dashboardRepository;
    private final ServerConfig config;

    public BookingService(BookingRepository bookingRepository,
                          LabRepository labRepository,
                          DashboardRepository dashboardRepository,
                          ServerConfig config) {
        this.bookingRepository = bookingRepository;
        this.labRepository = labRepository;
        this.dashboardRepository = dashboardRepository;
        this.config = config;
    }

    /**
     * Returns paginated bookings. Regular users see only their own; managers see all.
     *
     * @param currentUser authenticated user (determines scope)
     * @param labId       optional lab filter
     * @param status      optional status filter
     * @param page        1-based page
     * @param pageSize    items per page
     * @return paged bookings
     */
    public PagedResult<BookingDto> list(UserDto currentUser, Long labId,
                                        BookingStatus status, int page, int pageSize) {
        Long requesterId = null;
        // Students and lecturers can only see their own bookings
        if (!currentUser.hasRole(UserRole.ADMIN) && !currentUser.hasRole(UserRole.LAB_MANAGER)) {
            requesterId = currentUser.getId();
        }
        return bookingRepository.findAll(requesterId, labId, status, page, pageSize);
    }

    /**
     * Returns a single booking by ID. Access is restricted to the requester, managers, admins.
     *
     * @param id          booking ID
     * @param currentUser authenticated user
     * @return booking or empty if not found / not accessible
     */
    public Optional<BookingDto> getById(long id, UserDto currentUser) {
        Optional<BookingDto> booking = bookingRepository.findById(id);
        if (booking.isEmpty()) {
            return Optional.empty();
        }
        // Restrict access for non-managers
        boolean isOwner = booking.get().getRequesterId().equals(currentUser.getId());
        boolean isManager = currentUser.hasRole(UserRole.ADMIN)
                || currentUser.hasRole(UserRole.LAB_MANAGER);
        if (!isOwner && !isManager) {
            return Optional.empty();
        }
        return booking;
    }

    /**
     * Creates a new booking request after validating all business rules.
     *
     * @param dto         booking data from the client
     * @param currentUser authenticated requester
     * @return service result
     */
    public ServiceResult<BookingDto> create(BookingDto dto, UserDto currentUser) {
        // Validate basic fields
        if (dto.getLabId() == null) {
            return ServiceResult.failure("A lab must be selected.");
        }
        if (dto.getStartTime() == null || dto.getEndTime() == null) {
            return ServiceResult.failure("Start time and end time are required.");
        }
        if (dto.getPurpose() == null || dto.getPurpose().isBlank()) {
            return ServiceResult.failure("Purpose is required.");
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start = dto.getStartTime();
        LocalDateTime end = dto.getEndTime();

        if (!start.isAfter(now)) {
            return ServiceResult.failure("Start time must be in the future.");
        }
        if (!end.isAfter(start)) {
            return ServiceResult.failure("End time must be after start time.");
        }

        long durationMinutes = java.time.Duration.between(start, end).toMinutes();
        if (durationMinutes < config.getBookingMinDurationMinutes()) {
            return ServiceResult.failure(
                    "Minimum booking duration is " + config.getBookingMinDurationMinutes() + " minutes.");
        }
        if (start.isAfter(now.plusDays(config.getBookingMaxAdvanceDays()))) {
            return ServiceResult.failure(
                    "Bookings cannot be made more than " + config.getBookingMaxAdvanceDays()
                    + " days in advance.");
        }

        // Lab validation
        Optional<LabDto> maybelab = labRepository.findById(dto.getLabId());
        if (maybelab.isEmpty()) {
            return ServiceResult.failure("Selected lab does not exist.");
        }
        LabDto lab = maybelab.get();
        if (lab.getStatus() != LabStatus.ACTIVE) {
            return ServiceResult.failure("Lab '" + lab.getName() + "' is not currently available for booking.");
        }
        if (dto.getAttendeeCount() != null && dto.getAttendeeCount() > lab.getCapacity()) {
            return ServiceResult.failure(
                    "Attendee count (" + dto.getAttendeeCount()
                    + ") exceeds lab capacity (" + lab.getCapacity() + ").");
        }

        // Conflict detection
        if (bookingRepository.hasConflict(dto.getLabId(), start, end, 0)) {
            return ServiceResult.failure(
                    "This time slot conflicts with an existing approved booking for "
                    + lab.getName() + ". Please choose a different time.");
        }

        // Persist
        dto.setRequesterId(currentUser.getId());
        try {
            long newId = bookingRepository.create(dto);
            BookingDto created = bookingRepository.findById(newId).orElseThrow();
            dashboardRepository.log(currentUser.getId(), "BOOKING_CREATED",
                    "BOOKING", newId, "New booking for " + lab.getName()
                    + " from " + start + " to " + end);
            return ServiceResult.success(created);
        } catch (SQLException e) {
            LOG.error("Failed to create booking: {}", e.getMessage(), e);
            return ServiceResult.failure("Failed to save booking. Please try again.");
        }
    }

    /**
     * Approves a pending booking.
     *
     * @param bookingId   booking ID
     * @param currentUser manager performing the approval
     * @param note        optional approval note
     * @return service result
     */
    public ServiceResult<BookingDto> approve(long bookingId, UserDto currentUser, String note) {
        Optional<BookingDto> maybeBooking = bookingRepository.findById(bookingId);
        if (maybeBooking.isEmpty()) {
            return ServiceResult.failure("Booking not found.");
        }
        BookingDto booking = maybeBooking.get();
        if (booking.getStatus() != BookingStatus.PENDING) {
            return ServiceResult.failure("Only PENDING bookings can be approved.");
        }

        // Re-check conflicts at approval time (another booking may have snuck in)
        if (bookingRepository.hasConflict(booking.getLabId(),
                booking.getStartTime(), booking.getEndTime(), bookingId)) {
            return ServiceResult.failure(
                    "Cannot approve: a conflicting booking has been approved for this slot.");
        }

        try {
            bookingRepository.approve(bookingId, currentUser.getId(), note);
            dashboardRepository.log(currentUser.getId(), "BOOKING_APPROVED",
                    "BOOKING", bookingId, "Approved by " + currentUser.getFullName());
            return ServiceResult.success(bookingRepository.findById(bookingId).orElseThrow());
        } catch (SQLException e) {
            LOG.error("Failed to approve booking {}: {}", bookingId, e.getMessage(), e);
            return ServiceResult.failure("Failed to approve booking.");
        }
    }

    /**
     * Rejects a pending booking.
     *
     * @param bookingId   booking ID
     * @param currentUser manager performing the rejection
     * @param note        rejection reason (required)
     * @return service result
     */
    public ServiceResult<BookingDto> reject(long bookingId, UserDto currentUser, String note) {
        if (note == null || note.isBlank()) {
            return ServiceResult.failure("A rejection reason is required.");
        }
        Optional<BookingDto> maybeBooking = bookingRepository.findById(bookingId);
        if (maybeBooking.isEmpty()) {
            return ServiceResult.failure("Booking not found.");
        }
        if (maybeBooking.get().getStatus() != BookingStatus.PENDING) {
            return ServiceResult.failure("Only PENDING bookings can be rejected.");
        }
        try {
            bookingRepository.reject(bookingId, currentUser.getId(), note);
            dashboardRepository.log(currentUser.getId(), "BOOKING_REJECTED",
                    "BOOKING", bookingId, "Rejected: " + note);
            return ServiceResult.success(bookingRepository.findById(bookingId).orElseThrow());
        } catch (SQLException e) {
            LOG.error("Failed to reject booking {}: {}", bookingId, e.getMessage(), e);
            return ServiceResult.failure("Failed to reject booking.");
        }
    }

    /**
     * Cancels a booking. The requester may cancel their own; managers can cancel any.
     *
     * @param bookingId   booking ID
     * @param currentUser user performing the cancellation
     * @return service result
     */
    public ServiceResult<Void> cancel(long bookingId, UserDto currentUser) {
        Optional<BookingDto> maybeBooking = bookingRepository.findById(bookingId);
        if (maybeBooking.isEmpty()) {
            return ServiceResult.failure("Booking not found.");
        }
        BookingDto booking = maybeBooking.get();
        boolean isOwner = booking.getRequesterId().equals(currentUser.getId());
        boolean isManager = currentUser.hasRole(UserRole.ADMIN)
                || currentUser.hasRole(UserRole.LAB_MANAGER);
        if (!isOwner && !isManager) {
            return ServiceResult.failure("You do not have permission to cancel this booking.");
        }
        if (booking.getStatus() == BookingStatus.REJECTED
                || booking.getStatus() == BookingStatus.CANCELLED
                || booking.getStatus() == BookingStatus.COMPLETED) {
            return ServiceResult.failure("This booking cannot be cancelled in its current state.");
        }
        try {
            bookingRepository.cancel(bookingId);
            dashboardRepository.log(currentUser.getId(), "BOOKING_CANCELLED",
                    "BOOKING", bookingId, "Cancelled by " + currentUser.getUsername());
            return ServiceResult.success(null);
        } catch (SQLException e) {
            LOG.error("Failed to cancel booking {}: {}", bookingId, e.getMessage(), e);
            return ServiceResult.failure("Failed to cancel booking.");
        }
    }
}
