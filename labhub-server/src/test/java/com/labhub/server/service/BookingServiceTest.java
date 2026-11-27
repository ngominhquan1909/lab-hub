package com.labhub.server.service;

import com.labhub.common.dto.BookingDto;
import com.labhub.common.dto.LabDto;
import com.labhub.common.dto.UserDto;
import com.labhub.common.enums.BookingStatus;
import com.labhub.common.enums.LabStatus;
import com.labhub.common.enums.UserRole;
import com.labhub.server.config.ServerConfig;
import com.labhub.server.repository.BookingRepository;
import com.labhub.server.repository.DashboardRepository;
import com.labhub.server.repository.LabRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link BookingService} business logic.
 * Uses Mockito-style stubs without the Mockito library - relies on anonymous classes
 * for compatibility with the project's dependency set.
 *
 * <p>Tests cover:
 * <ul>
 *   <li>Start time in the past is rejected.</li>
 *   <li>End time before start time is rejected.</li>
 *   <li>Duration below minimum is rejected.</li>
 *   <li>INACTIVE lab cannot be booked.</li>
 *   <li>Attendee count exceeding capacity is rejected.</li>
 *   <li>Conflicting approved booking is rejected.</li>
 *   <li>Valid booking is accepted.</li>
 *   <li>Approval of a non-PENDING booking is rejected.</li>
 *   <li>Rejection without a note is rejected.</li>
 *   <li>Owner can cancel their own booking.</li>
 *   <li>Non-owner, non-manager cannot cancel.</li>
 * </ul>
 */
class BookingServiceTest {

    // Fakes

    private static class FakeBookingRepository extends BookingRepository {
        private boolean conflictResult = false;
        private BookingDto storedBooking;
        private long nextId = 1L;

        FakeBookingRepository() {
            super(null); // no DB connection needed for unit tests
        }

        @Override
        public boolean hasConflict(long labId, LocalDateTime start, LocalDateTime end, long excludeId) {
            return conflictResult;
        }

        @Override
        public long create(BookingDto dto) {
            storedBooking = dto;
            storedBooking.setId(nextId);
            storedBooking.setStatus(BookingStatus.PENDING);
            return nextId++;
        }

        @Override
        public Optional<BookingDto> findById(long id) {
            return storedBooking != null && storedBooking.getId() == id
                    ? Optional.of(storedBooking) : Optional.empty();
        }

        @Override
        public void approve(long bookingId, long deciderId, String note) {
            if (storedBooking != null) storedBooking.setStatus(BookingStatus.APPROVED);
        }

        @Override
        public void reject(long bookingId, long deciderId, String note) {
            if (storedBooking != null) storedBooking.setStatus(BookingStatus.REJECTED);
        }

        @Override
        public void cancel(long bookingId) {
            if (storedBooking != null) storedBooking.setStatus(BookingStatus.CANCELLED);
        }
    }

    private static class FakeLabRepository extends LabRepository {
        private final LabDto lab;

        FakeLabRepository(LabDto lab) {
            super(null);
            this.lab = lab;
        }

        @Override
        public Optional<LabDto> findById(long id) {
            return Optional.of(lab);
        }
    }

    private static class FakeDashboardRepository extends DashboardRepository {
        FakeDashboardRepository() { super(null); }

        @Override
        public void log(Long actorId, String action, String targetType,
                        Long targetId, String details) {
            // no-op in tests
        }
    }

    private static class FakeServerConfig extends ServerConfig {
        FakeServerConfig() { super(new java.util.Properties()); }

        @Override
        public int getBookingMinDurationMinutes() { return 30; }

        @Override
        public int getBookingMaxAdvanceDays() { return 90; }
    }

    // Test setup

    private FakeBookingRepository bookingRepo;
    private LabDto activeLab;
    private BookingService service;
    private UserDto lecturer;

    @BeforeEach
    void setUp() {
        activeLab = new LabDto();
        activeLab.setId(1L);
        activeLab.setName("CS Lab A");
        activeLab.setStatus(LabStatus.ACTIVE);
        activeLab.setCapacity(40);

        bookingRepo = new FakeBookingRepository();
        service = new BookingService(bookingRepo,
                new FakeLabRepository(activeLab),
                new FakeDashboardRepository(),
                new FakeServerConfig());

        lecturer = new UserDto();
        lecturer.setId(10L);
        lecturer.setFullName("Prof. Test");
        lecturer.setRoles(List.of(UserRole.LECTURER));
    }

    // Validation tests

    @Test
    @DisplayName("Start time in the past -> rejected")
    void create_startTimeInPast_rejected() {
        BookingDto dto = booking(LocalDateTime.now().minusHours(1),
                LocalDateTime.now().plusHours(1));
        ServiceResult<BookingDto> result = service.create(dto, lecturer);
        assertFalse(result.isSuccess());
        assertTrue(result.getErrorMessage().contains("future"));
    }

    @Test
    @DisplayName("End time before start time -> rejected")
    void create_endBeforeStart_rejected() {
        BookingDto dto = booking(LocalDateTime.now().plusHours(2),
                LocalDateTime.now().plusHours(1));
        ServiceResult<BookingDto> result = service.create(dto, lecturer);
        assertFalse(result.isSuccess());
        assertTrue(result.getErrorMessage().contains("after start"));
    }

    @Test
    @DisplayName("Duration below minimum -> rejected")
    void create_durationBelowMinimum_rejected() {
        // 20 minutes, minimum is 30
        BookingDto dto = booking(LocalDateTime.now().plusHours(1),
                LocalDateTime.now().plusHours(1).plusMinutes(20));
        ServiceResult<BookingDto> result = service.create(dto, lecturer);
        assertFalse(result.isSuccess());
        assertTrue(result.getErrorMessage().contains("30"));
    }

    @Test
    @DisplayName("INACTIVE lab -> rejected")
    void create_inactiveLab_rejected() {
        activeLab.setStatus(LabStatus.INACTIVE);
        BookingDto dto = validBooking();
        ServiceResult<BookingDto> result = service.create(dto, lecturer);
        assertFalse(result.isSuccess());
        assertTrue(result.getErrorMessage().contains("not currently available"));
    }

    @Test
    @DisplayName("Attendee count exceeds capacity -> rejected")
    void create_attendeesExceedCapacity_rejected() {
        BookingDto dto = validBooking();
        dto.setAttendeeCount(100); // lab capacity is 40
        ServiceResult<BookingDto> result = service.create(dto, lecturer);
        assertFalse(result.isSuccess());
        assertTrue(result.getErrorMessage().contains("capacity"));
    }

    @Test
    @DisplayName("Conflicting approved booking -> rejected")
    void create_conflictingBooking_rejected() {
        bookingRepo.conflictResult = true;
        ServiceResult<BookingDto> result = service.create(validBooking(), lecturer);
        assertFalse(result.isSuccess());
        assertTrue(result.getErrorMessage().contains("conflicts"));
    }

    @Test
    @DisplayName("Valid booking with no conflict -> accepted")
    void create_validBooking_accepted() {
        bookingRepo.conflictResult = false;
        ServiceResult<BookingDto> result = service.create(validBooking(), lecturer);
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertEquals(BookingStatus.PENDING, result.getData().getStatus());
    }

    // Approval tests

    @Test
    @DisplayName("Approve PENDING booking -> success")
    void approve_pendingBooking_succeeds() {
        // Create then approve
        service.create(validBooking(), lecturer);
        UserDto manager = manager();
        ServiceResult<BookingDto> result = service.approve(1L, manager, null);
        assertTrue(result.isSuccess());
        assertEquals(BookingStatus.APPROVED, result.getData().getStatus());
    }

    @Test
    @DisplayName("Approve already-APPROVED booking -> rejected")
    void approve_alreadyApproved_rejected() {
        service.create(validBooking(), lecturer);
        UserDto manager = manager();
        service.approve(1L, manager, null);
        // Approve again
        ServiceResult<BookingDto> result = service.approve(1L, manager, null);
        assertFalse(result.isSuccess());
        assertTrue(result.getErrorMessage().contains("PENDING"));
    }

    // Rejection tests

    @Test
    @DisplayName("Reject without note -> validation error")
    void reject_missingNote_rejected() {
        service.create(validBooking(), lecturer);
        ServiceResult<BookingDto> result = service.reject(1L, manager(), null);
        assertFalse(result.isSuccess());
        assertTrue(result.getErrorMessage().contains("reason"));
    }

    @Test
    @DisplayName("Reject PENDING booking with note -> success")
    void reject_pendingWithNote_succeeds() {
        service.create(validBooking(), lecturer);
        ServiceResult<BookingDto> result = service.reject(1L, manager(), "Lab under maintenance.");
        assertTrue(result.isSuccess());
        assertEquals(BookingStatus.REJECTED, result.getData().getStatus());
    }

    // Cancellation tests

    @Test
    @DisplayName("Owner cancels own booking -> success")
    void cancel_byOwner_succeeds() {
        service.create(validBooking(), lecturer);
        ServiceResult<Void> result = service.cancel(1L, lecturer);
        assertTrue(result.isSuccess());
    }

    @Test
    @DisplayName("Non-owner student cannot cancel another user's booking")
    void cancel_byNonOwner_forbidden() {
        service.create(validBooking(), lecturer);
        UserDto otherStudent = new UserDto();
        otherStudent.setId(99L);
        otherStudent.setRoles(List.of(UserRole.STUDENT));
        ServiceResult<Void> result = service.cancel(1L, otherStudent);
        assertFalse(result.isSuccess());
        assertTrue(result.getErrorMessage().contains("permission"));
    }

    @Test
    @DisplayName("Manager cancels any booking -> success")
    void cancel_byManager_succeeds() {
        service.create(validBooking(), lecturer);
        ServiceResult<Void> result = service.cancel(1L, manager());
        assertTrue(result.isSuccess());
    }

    // Helpers

    private BookingDto booking(LocalDateTime start, LocalDateTime end) {
        BookingDto dto = new BookingDto();
        dto.setLabId(1L);
        dto.setStartTime(start);
        dto.setEndTime(end);
        dto.setPurpose("Test session");
        dto.setAttendeeCount(10);
        return dto;
    }

    private BookingDto validBooking() {
        return booking(LocalDateTime.now().plusDays(1).withHour(9).withMinute(0),
                       LocalDateTime.now().plusDays(1).withHour(11).withMinute(0));
    }

    private UserDto manager() {
        UserDto mgr = new UserDto();
        mgr.setId(5L);
        mgr.setFullName("Dr. Manager");
        mgr.setRoles(List.of(UserRole.LAB_MANAGER));
        return mgr;
    }
}
