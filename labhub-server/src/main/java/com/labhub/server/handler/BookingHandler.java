package com.labhub.server.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.labhub.common.dto.BookingDto;
import com.labhub.common.dto.PagedResult;
import com.labhub.common.dto.UserDto;
import com.labhub.common.enums.BookingStatus;
import com.labhub.common.enums.UserRole;
import com.labhub.common.protocol.Request;
import com.labhub.common.protocol.Response;
import com.labhub.server.repository.BookingRepository;
import com.labhub.server.service.BookingService;
import com.labhub.server.service.ServiceResult;

import java.util.Optional;

/**
 * Handles all booking-related requests (LIST, GET, CREATE, APPROVE, REJECT, CANCEL, SCHEDULE).
 */
public class BookingHandler extends BaseHandler {

    private final BookingService bookingService;
    private final BookingRepository bookingRepository;

    public BookingHandler(BookingService bookingService, BookingRepository bookingRepository) {
        this.bookingService = bookingService;
        this.bookingRepository = bookingRepository;
    }

    @Override
    public Response handle(Request request, UserDto currentUser) {
        return switch (request.getType()) {
            case BOOKING_LIST     -> handleList(request, currentUser);
            case BOOKING_GET      -> handleGet(request, currentUser);
            case BOOKING_CREATE   -> handleCreate(request, currentUser);
            case BOOKING_APPROVE  -> handleApprove(request, currentUser);
            case BOOKING_REJECT   -> handleReject(request, currentUser);
            case BOOKING_CANCEL   -> handleCancel(request, currentUser);
            case BOOKING_SCHEDULE -> handleSchedule(request, currentUser);
            default -> Response.error(request.getRequestId(), "INVALID_REQUEST",
                    "BookingHandler does not handle " + request.getType());
        };
    }

    private Response handleList(Request request, UserDto currentUser) {
        JsonNode payload = request.getPayload();
        Long labId = payload != null && payload.has("labId") ? payload.get("labId").asLong() : null;
        BookingStatus status = null;
        if (payload != null && payload.has("status")) {
            try {
                status = BookingStatus.valueOf(payload.get("status").asText());
            } catch (IllegalArgumentException ignored) { }
        }
        int page     = intOrDefault(payload, "page",     1);
        int pageSize = intOrDefault(payload, "pageSize", 20);

        PagedResult<BookingDto> result = bookingService.list(currentUser, labId, status, page, pageSize);
        return Response.ok(request.getRequestId(), "Bookings loaded.", toJsonNode(result));
    }

    private Response handleGet(Request request, UserDto currentUser) {
        long id = longOrDefault(request.getPayload(), "id", 0);
        if (id <= 0) {
            return Response.error(request.getRequestId(), "VALIDATION_ERROR", "Booking ID is required.");
        }
        Optional<BookingDto> booking = bookingService.getById(id, currentUser);
        return booking
                .map(b -> Response.ok(request.getRequestId(), "OK", toJsonNode(b)))
                .orElseGet(() -> Response.error(request.getRequestId(), "NOT_FOUND",
                        "Booking not found."));
    }

    private Response handleCreate(Request request, UserDto currentUser) {
        BookingDto dto = parsePayload(request, BookingDto.class);
        if (dto == null) {
            return missingPayload(request.getRequestId());
        }
        ServiceResult<BookingDto> result = bookingService.create(dto, currentUser);
        return result.isSuccess()
                ? Response.ok(request.getRequestId(), "Booking submitted successfully.",
                    toJsonNode(result.getData()))
                : Response.error(request.getRequestId(), "VALIDATION_ERROR", result.getErrorMessage());
    }

    private Response handleApprove(Request request, UserDto currentUser) {
        Response authError = requireRole(currentUser, request.getRequestId(),
                UserRole.ADMIN, UserRole.LAB_MANAGER);
        if (authError != null) {
            return authError;
        }
        JsonNode payload = request.getPayload();
        long id   = longOrDefault(payload, "id", 0);
        String note = textOrNull(payload, "note");
        if (id <= 0) {
            return Response.error(request.getRequestId(), "VALIDATION_ERROR", "Booking ID is required.");
        }
        ServiceResult<BookingDto> result = bookingService.approve(id, currentUser, note);
        return result.isSuccess()
                ? Response.ok(request.getRequestId(), "Booking approved.", toJsonNode(result.getData()))
                : Response.error(request.getRequestId(), "CONFLICT", result.getErrorMessage());
    }

    private Response handleReject(Request request, UserDto currentUser) {
        Response authError = requireRole(currentUser, request.getRequestId(),
                UserRole.ADMIN, UserRole.LAB_MANAGER);
        if (authError != null) {
            return authError;
        }
        JsonNode payload = request.getPayload();
        long id   = longOrDefault(payload, "id", 0);
        String note = textOrNull(payload, "note");
        if (id <= 0) {
            return Response.error(request.getRequestId(), "VALIDATION_ERROR", "Booking ID is required.");
        }
        ServiceResult<BookingDto> result = bookingService.reject(id, currentUser, note);
        return result.isSuccess()
                ? Response.ok(request.getRequestId(), "Booking rejected.", toJsonNode(result.getData()))
                : Response.error(request.getRequestId(), "VALIDATION_ERROR", result.getErrorMessage());
    }

    private Response handleCancel(Request request, UserDto currentUser) {
        long id = longOrDefault(request.getPayload(), "id", 0);
        if (id <= 0) {
            return Response.error(request.getRequestId(), "VALIDATION_ERROR", "Booking ID is required.");
        }
        ServiceResult<Void> result = bookingService.cancel(id, currentUser);
        return result.isSuccess()
                ? Response.ok(request.getRequestId(), "Booking cancelled.")
                : Response.error(request.getRequestId(), "FORBIDDEN", result.getErrorMessage());
    }

    private Response handleSchedule(Request request, UserDto currentUser) {
        // Returns approved bookings for a date range (for the schedule/calendar view)
        JsonNode payload = request.getPayload();
        // Default: today + 7 days
        java.time.LocalDateTime from = java.time.LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        java.time.LocalDateTime to   = from.plusDays(7);
        long labId = longOrDefault(payload, "labId", 0);

        java.util.List<BookingDto> schedule = bookingRepository.findSchedule(labId, from, to);
        return Response.ok(request.getRequestId(), "Schedule loaded.", toJsonNode(schedule));
    }
}
