package com.labhub.client.network;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.labhub.common.dto.*;
import com.labhub.common.enums.BorrowStatus;
import com.labhub.common.enums.BookingStatus;
import com.labhub.common.enums.EquipmentCondition;
import com.labhub.common.protocol.JsonMapper;
import com.labhub.common.protocol.Request;
import com.labhub.common.protocol.RequestType;
import com.labhub.common.protocol.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * High-level client API facade.
 *
 * <p>Each method builds a typed {@link Request}, delegates to {@link ServerConnection},
 * and deserializes the {@link Response} payload into the expected Java type.
 *
 * <p>All methods throw {@link ApiException} on communication errors or server-returned
 * error responses, so callers can handle them uniformly.
 *
 * <p>All calls must be made from a background thread (never the JavaFX Application Thread).
 * Use {@code javafx.concurrent.Task} in controllers.
 */
public class ApiClient {

    private static final Logger LOG = LoggerFactory.getLogger(ApiClient.class);
    private static final ObjectMapper MAPPER = JsonMapper.get();

    private final ServerConnection connection;
    private String sessionToken;    // null before login

    /**
     * @param connection an open server connection
     */
    public ApiClient(ServerConnection connection) {
        this.connection = connection;
    }

    // ── Auth ──────────────────────────────────────────────────────────────

    /**
     * Authenticates with the server. Stores the token on success.
     *
     * @param username login username
     * @param password plaintext password (sent over the socket; use TLS in production)
     * @return authenticated user DTO
     * @throws ApiException on failure
     */
    public UserDto login(String username, String password) throws ApiException {
        JsonNode payload = MAPPER.createObjectNode()
                .put("username", username)
                .put("password", password);
        Response response = send(RequestType.LOGIN, payload);
        UserDto user = fromPayload(response, "user", UserDto.class);
        sessionToken = response.getPayload().get("token").asText();
        return user;
    }

    /**
     * Logs out and clears the session token.
     *
     * @throws ApiException on failure
     */
    public void logout() throws ApiException {
        send(RequestType.LOGOUT, null);
        sessionToken = null;
    }

    // ── Dashboard ─────────────────────────────────────────────────────────

    /**
     * Returns current dashboard statistics.
     *
     * @return stats DTO
     * @throws ApiException on failure
     */
    public DashboardStatsDto getDashboardStats() throws ApiException {
        Response r = send(RequestType.DASHBOARD_STATS, null);
        return fromPayload(r, DashboardStatsDto.class);
    }

    // ── Labs ──────────────────────────────────────────────────────────────

    /**
     * Returns a paginated list of labs.
     *
     * @param search   optional search string
     * @param page     1-based page
     * @param pageSize items per page
     * @return paged lab list
     * @throws ApiException on failure
     */
    public PagedResult<LabDto> getLabs(String search, int page, int pageSize) throws ApiException {
        JsonNode payload = MAPPER.createObjectNode()
                .put("search",   search != null ? search : "")
                .put("page",     page)
                .put("pageSize", pageSize);
        Response r = send(RequestType.LAB_LIST, payload);
        return pagedResult(r, LabDto.class);
    }

    /**
     * Creates a new lab.
     *
     * @param dto lab data
     * @return created lab DTO with assigned ID
     * @throws ApiException on failure
     */
    public LabDto createLab(LabDto dto) throws ApiException {
        Response r = send(RequestType.LAB_CREATE, MAPPER.valueToTree(dto));
        return fromPayload(r, LabDto.class);
    }

    /**
     * Updates an existing lab.
     *
     * @param dto lab data with valid ID
     * @return updated lab DTO
     * @throws ApiException on failure
     */
    public LabDto updateLab(LabDto dto) throws ApiException {
        Response r = send(RequestType.LAB_UPDATE, MAPPER.valueToTree(dto));
        return fromPayload(r, LabDto.class);
    }

    // ── Equipment ─────────────────────────────────────────────────────────

    /**
     * Returns a paginated list of equipment.
     *
     * @param search   optional search
     * @param labId    optional lab filter (null = all)
     * @param page     page
     * @param pageSize page size
     * @return paged equipment
     * @throws ApiException on failure
     */
    public PagedResult<EquipmentDto> getEquipment(String search, Long labId,
                                                   int page, int pageSize) throws ApiException {
        com.fasterxml.jackson.databind.node.ObjectNode payload = MAPPER.createObjectNode()
                .put("page", page)
                .put("pageSize", pageSize);
        if (search != null && !search.isBlank()) {
            payload.put("search", search);
        }
        if (labId != null) {
            payload.put("labId", labId);
        }
        Response r = send(RequestType.EQUIPMENT_LIST, payload);
        return pagedResult(r, EquipmentDto.class);
    }

    /**
     * Creates a new equipment record.
     *
     * @param dto equipment data
     * @return created equipment DTO
     * @throws ApiException on failure
     */
    public EquipmentDto createEquipment(EquipmentDto dto) throws ApiException {
        Response r = send(RequestType.EQUIPMENT_CREATE, MAPPER.valueToTree(dto));
        return fromPayload(r, EquipmentDto.class);
    }

    /**
     * Updates an existing equipment record.
     *
     * @param dto updated data with valid ID
     * @return updated equipment DTO
     * @throws ApiException on failure
     */
    public EquipmentDto updateEquipment(EquipmentDto dto) throws ApiException {
        Response r = send(RequestType.EQUIPMENT_UPDATE, MAPPER.valueToTree(dto));
        return fromPayload(r, EquipmentDto.class);
    }

    /**
     * Returns all equipment categories.
     *
     * @return list of categories
     * @throws ApiException on failure
     */
    public List<CategoryDto> getCategories() throws ApiException {
        Response r = send(RequestType.CATEGORY_LIST, null);
        return listFromPayload(r, CategoryDto.class);
    }

    /**
     * Returns a combined, newest-first history of borrow loans and maintenance
     * tickets for a piece of equipment.
     *
     * @param equipmentId equipment ID
     * @return list of history entries, each with fields: type, date, status,
     *         personName, summary, and (depending on type) conditionAtCheckout,
     *         conditionAtReturn, or resolutionNotes
     * @throws ApiException on failure
     */
    public List<JsonNode> getEquipmentHistory(long equipmentId) throws ApiException {
        Response r = send(RequestType.EQUIPMENT_HISTORY, MAPPER.createObjectNode().put("id", equipmentId));
        List<JsonNode> entries = new java.util.ArrayList<>();
        if (r.getPayload() != null && r.getPayload().isArray()) {
            r.getPayload().forEach(entries::add);
        }
        return entries;
    }

    // ── Bookings ──────────────────────────────────────────────────────────

    /**
     * Returns paginated bookings with optional filters.
     *
     * @param status   optional status filter
     * @param labId    optional lab filter
     * @param page     page
     * @param pageSize page size
     * @return paged bookings
     * @throws ApiException on failure
     */
    public PagedResult<BookingDto> getBookings(BookingStatus status, Long labId,
                                                int page, int pageSize) throws ApiException {
        com.fasterxml.jackson.databind.node.ObjectNode payload = MAPPER.createObjectNode()
                .put("page", page)
                .put("pageSize", pageSize);
        if (status != null) {
            payload.put("status", status.name());
        }
        if (labId != null) {
            payload.put("labId", labId);
        }
        Response r = send(RequestType.BOOKING_LIST, payload);
        return pagedResult(r, BookingDto.class);
    }

    /**
     * Submits a new booking request.
     *
     * @param dto booking data
     * @return created booking DTO
     * @throws ApiException on failure
     */
    public BookingDto createBooking(BookingDto dto) throws ApiException {
        Response r = send(RequestType.BOOKING_CREATE, MAPPER.valueToTree(dto));
        return fromPayload(r, BookingDto.class);
    }

    /**
     * Approves a booking.
     *
     * @param bookingId booking ID
     * @param note      optional approval note
     * @return updated booking DTO
     * @throws ApiException on failure
     */
    public BookingDto approveBooking(long bookingId, String note) throws ApiException {
        com.fasterxml.jackson.databind.node.ObjectNode p = MAPPER.createObjectNode()
                .put("id", bookingId);
        if (note != null) {
            p.put("note", note);
        }
        Response r = send(RequestType.BOOKING_APPROVE, p);
        return fromPayload(r, BookingDto.class);
    }

    /**
     * Rejects a booking.
     *
     * @param bookingId booking ID
     * @param note      rejection reason (required)
     * @return updated booking DTO
     * @throws ApiException on failure
     */
    public BookingDto rejectBooking(long bookingId, String note) throws ApiException {
        com.fasterxml.jackson.databind.node.ObjectNode p = MAPPER.createObjectNode()
                .put("id", bookingId)
                .put("note", note != null ? note : "");
        Response r = send(RequestType.BOOKING_REJECT, p);
        return fromPayload(r, BookingDto.class);
    }

    /**
     * Cancels a booking.
     *
     * @param bookingId booking ID
     * @throws ApiException on failure
     */
    public void cancelBooking(long bookingId) throws ApiException {
        send(RequestType.BOOKING_CANCEL, MAPPER.createObjectNode().put("id", bookingId));
    }

    // ── Borrow requests ───────────────────────────────────────────────────

    /**
     * Returns paginated borrow requests.
     *
     * @param status   optional status filter
     * @param page     page
     * @param pageSize page size
     * @return paged borrow requests
     * @throws ApiException on failure
     */
    public PagedResult<BorrowRequestDto> getBorrowRequests(BorrowStatus status,
                                                            int page, int pageSize) throws ApiException {
        com.fasterxml.jackson.databind.node.ObjectNode payload = MAPPER.createObjectNode()
                .put("page", page)
                .put("pageSize", pageSize);
        if (status != null) {
            payload.put("status", status.name());
        }
        Response r = send(RequestType.BORROW_LIST, payload);
        return pagedResult(r, BorrowRequestDto.class);
    }

    /**
     * Submits a new borrow request.
     *
     * @param dto         borrow request header
     * @param equipmentIds IDs of equipment to borrow
     * @return created borrow request DTO
     * @throws ApiException on failure
     */
    public BorrowRequestDto createBorrowRequest(BorrowRequestDto dto,
                                                 List<Long> equipmentIds) throws ApiException {
        com.fasterxml.jackson.databind.node.ObjectNode payload = MAPPER.createObjectNode()
                .put("purpose", dto.getPurpose())
                .put("expectedReturnAt",
                        dto.getExpectedReturnAt() != null
                                ? dto.getExpectedReturnAt().toString() : null);
        com.fasterxml.jackson.databind.node.ArrayNode ids = payload.putArray("equipmentIds");
        if (equipmentIds != null) {
            equipmentIds.forEach(ids::add);
        }
        Response r = send(RequestType.BORROW_CREATE, payload);
        return fromPayload(r, BorrowRequestDto.class);
    }

    /**
     * Approves a borrow request.
     *
     * @param requestId request ID
     * @param note      optional note
     * @return updated borrow request DTO
     * @throws ApiException on failure
     */
    public BorrowRequestDto approveBorrowRequest(long requestId, String note) throws ApiException {
        com.fasterxml.jackson.databind.node.ObjectNode p = MAPPER.createObjectNode()
                .put("id", requestId);
        if (note != null) {
            p.put("note", note);
        }
        Response r = send(RequestType.BORROW_APPROVE, p);
        return fromPayload(r, BorrowRequestDto.class);
    }

    /**
     * Rejects a borrow request.
     *
     * @param requestId request ID
     * @param note      rejection reason
     * @return updated borrow request DTO
     * @throws ApiException on failure
     */
    public BorrowRequestDto rejectBorrowRequest(long requestId, String note) throws ApiException {
        Response r = send(RequestType.BORROW_REJECT,
                MAPPER.createObjectNode().put("id", requestId).put("note", note != null ? note : ""));
        return fromPayload(r, BorrowRequestDto.class);
    }

    /**
     * Records the physical checkout of an approved borrow request (hand-off to the borrower).
     *
     * @param requestId      borrow request ID (must be APPROVED)
     * @param itemConditions equipment condition recorded at checkout, keyed by equipment ID
     * @return updated borrow request DTO
     * @throws ApiException on failure
     */
    public BorrowRequestDto checkoutBorrowRequest(long requestId,
                                                   Map<Long, EquipmentCondition> itemConditions) throws ApiException {
        com.fasterxml.jackson.databind.node.ObjectNode payload = MAPPER.createObjectNode()
                .put("id", requestId);
        if (itemConditions != null) {
            com.fasterxml.jackson.databind.node.ObjectNode condNode =
                    payload.putObject("itemConditions");
            itemConditions.forEach((k, v) -> condNode.put(String.valueOf(k), v.name()));
        }
        Response r = send(RequestType.BORROW_CHECKOUT, payload);
        return fromPayload(r, BorrowRequestDto.class);
    }

    /**
     * Cancels a PENDING or APPROVED borrow request.
     *
     * @param requestId borrow request ID
     * @return updated borrow request DTO
     * @throws ApiException on failure
     */
    public BorrowRequestDto cancelBorrowRequest(long requestId) throws ApiException {
        Response r = send(RequestType.BORROW_CANCEL, MAPPER.createObjectNode().put("id", requestId));
        return fromPayload(r, BorrowRequestDto.class);
    }

    /**
     * Records equipment return.
     *
     * @param requestId      borrow request ID
     * @param itemConditions equipment condition at return, keyed by equipment ID
     * @return updated borrow request DTO
     * @throws ApiException on failure
     */
    public BorrowRequestDto returnEquipment(long requestId,
                                             Map<Long, EquipmentCondition> itemConditions) throws ApiException {
        com.fasterxml.jackson.databind.node.ObjectNode payload = MAPPER.createObjectNode()
                .put("id", requestId);
        if (itemConditions != null) {
            com.fasterxml.jackson.databind.node.ObjectNode condNode =
                    payload.putObject("itemConditions");
            itemConditions.forEach((k, v) -> condNode.put(String.valueOf(k), v.name()));
        }
        Response r = send(RequestType.BORROW_RETURN, payload);
        return fromPayload(r, BorrowRequestDto.class);
    }

    // ── Maintenance ───────────────────────────────────────────────────────

    /**
     * Returns paginated maintenance tickets.
     *
     * @param page     page
     * @param pageSize page size
     * @return paged tickets
     * @throws ApiException on failure
     */
    public PagedResult<MaintenanceTicketDto> getMaintenanceTickets(int page,
                                                                    int pageSize) throws ApiException {
        Response r = send(RequestType.MAINTENANCE_LIST,
                MAPPER.createObjectNode().put("page", page).put("pageSize", pageSize));
        return pagedResult(r, MaintenanceTicketDto.class);
    }

    /**
     * Files a new maintenance ticket.
     *
     * @param dto ticket data
     * @return created ticket DTO
     * @throws ApiException on failure
     */
    public MaintenanceTicketDto createMaintenanceTicket(MaintenanceTicketDto dto) throws ApiException {
        Response r = send(RequestType.MAINTENANCE_CREATE, MAPPER.valueToTree(dto));
        return fromPayload(r, MaintenanceTicketDto.class);
    }

    /**
     * Assigns a maintenance ticket to a user.
     *
     * @param ticketId   ticket ID
     * @param assigneeId user ID to assign the ticket to
     * @return updated ticket DTO
     * @throws ApiException on failure
     */
    public MaintenanceTicketDto assignMaintenanceTicket(long ticketId, long assigneeId) throws ApiException {
        Response r = send(RequestType.MAINTENANCE_ASSIGN,
                MAPPER.createObjectNode().put("id", ticketId).put("assigneeId", assigneeId));
        return fromPayload(r, MaintenanceTicketDto.class);
    }

    /**
     * Marks a maintenance ticket resolved.
     *
     * @param ticketId        ticket ID
     * @param resolutionNotes description of the fix (required)
     * @param repairCost      optional repair cost
     * @return updated ticket DTO
     * @throws ApiException on failure
     */
    public MaintenanceTicketDto resolveMaintenanceTicket(long ticketId, String resolutionNotes,
                                                          java.math.BigDecimal repairCost) throws ApiException {
        com.fasterxml.jackson.databind.node.ObjectNode p = MAPPER.createObjectNode()
                .put("id", ticketId)
                .put("resolutionNotes", resolutionNotes);
        if (repairCost != null) {
            p.put("repairCost", repairCost);
        }
        Response r = send(RequestType.MAINTENANCE_RESOLVE, p);
        return fromPayload(r, MaintenanceTicketDto.class);
    }

    /**
     * Closes a maintenance ticket; the equipment returns to AVAILABLE server-side.
     *
     * @param ticketId ticket ID
     * @throws ApiException on failure
     */
    public void closeMaintenanceTicket(long ticketId) throws ApiException {
        send(RequestType.MAINTENANCE_CLOSE, MAPPER.createObjectNode().put("id", ticketId));
    }

    // ── Users ─────────────────────────────────────────────────────────────

    /**
     * Returns paginated user accounts (admin only).
     *
     * @param search   optional search
     * @param page     page
     * @param pageSize page size
     * @return paged users
     * @throws ApiException on failure
     */
    public PagedResult<UserDto> getUsers(String search, int page, int pageSize) throws ApiException {
        com.fasterxml.jackson.databind.node.ObjectNode payload = MAPPER.createObjectNode()
                .put("page", page)
                .put("pageSize", pageSize);
        if (search != null && !search.isBlank()) {
            payload.put("search", search);
        }
        Response r = send(RequestType.USER_LIST, payload);
        return pagedResult(r, UserDto.class);
    }

    /**
     * Creates a new user account (admin only).
     *
     * @param username username
     * @param email    email address
     * @param fullName full display name
     * @param password initial plaintext password (hashed server-side)
     * @param phone    optional phone number
     * @param roles    roles to assign (defaults to STUDENT if empty)
     * @return created user DTO
     * @throws ApiException on failure
     */
    public UserDto createUser(String username, String email, String fullName, String password,
                               String phone, List<com.labhub.common.enums.UserRole> roles) throws ApiException {
        com.fasterxml.jackson.databind.node.ObjectNode payload = MAPPER.createObjectNode()
                .put("username", username)
                .put("email", email)
                .put("fullName", fullName)
                .put("password", password);
        if (phone != null && !phone.isBlank()) {
            payload.put("phoneNumber", phone);
        }
        com.fasterxml.jackson.databind.node.ArrayNode roleArray = payload.putArray("roles");
        if (roles != null) {
            roles.forEach(r -> roleArray.add(r.name()));
        }
        Response r = send(RequestType.USER_CREATE, payload);
        return fromPayload(r, UserDto.class);
    }

    // ── Audit ─────────────────────────────────────────────────────────────

    /**
     * Returns paginated audit log entries, newest first (admin/lab-manager only).
     *
     * @param page     page
     * @param pageSize page size
     * @return paged audit entries
     * @throws ApiException on failure
     */
    public PagedResult<AuditLogDto> getAuditLogs(int page, int pageSize) throws ApiException {
        Response r = send(RequestType.AUDIT_LIST,
                MAPPER.createObjectNode().put("page", page).put("pageSize", pageSize));
        return pagedResult(r, AuditLogDto.class);
    }

    // ── Internal ──────────────────────────────────────────────────────────

    /**
     * Sends a request and returns the response.
     * Throws {@link ApiException} for transport errors or server error responses.
     */
    private Response send(RequestType type, JsonNode payload) throws ApiException {
        Request request = new Request(UUID.randomUUID().toString(), type, sessionToken, payload);
        try {
            Response response = connection.send(request);
            if (!response.isSuccess()) {
                throw new ApiException(response.getCode(), response.getMessage());
            }
            return response;
        } catch (IOException e) {
            LOG.error("Network error sending {}: {}", type, e.getMessage(), e);
            throw new ApiException("NETWORK_ERROR",
                    "Cannot reach the server. Please check your connection and try again.");
        }
    }

    private <T> T fromPayload(Response response, Class<T> type) throws ApiException {
        try {
            return MAPPER.treeToValue(response.getPayload(), type);
        } catch (Exception e) {
            LOG.error("Failed to parse response payload as {}: {}", type.getSimpleName(), e.getMessage());
            throw new ApiException("PARSE_ERROR", "Received an unexpected response from the server.");
        }
    }

    private <T> T fromPayload(Response response, String field, Class<T> type) throws ApiException {
        try {
            return MAPPER.treeToValue(response.getPayload().get(field), type);
        } catch (Exception e) {
            LOG.error("Failed to parse response field '{}': {}", field, e.getMessage());
            throw new ApiException("PARSE_ERROR", "Received an unexpected response from the server.");
        }
    }

    private <T> PagedResult<T> pagedResult(Response response, Class<T> itemType) throws ApiException {
        try {
            JsonNode node = response.getPayload();
            List<T> items = MAPPER.readValue(
                    MAPPER.treeAsTokens(node.get("items")),
                    MAPPER.getTypeFactory().constructCollectionType(List.class, itemType));
            PagedResult<T> result = new PagedResult<>();
            result.setItems(items);
            result.setPage(node.path("page").asInt(1));
            result.setPageSize(node.path("pageSize").asInt(20));
            result.setTotalItems(node.path("totalItems").asLong(0));
            result.setTotalPages(node.path("totalPages").asInt(0));
            return result;
        } catch (Exception e) {
            LOG.error("Failed to parse paged result: {}", e.getMessage());
            throw new ApiException("PARSE_ERROR", "Failed to parse server response.");
        }
    }

    private <T> List<T> listFromPayload(Response response, Class<T> itemType) throws ApiException {
        try {
            return MAPPER.readValue(
                    MAPPER.treeAsTokens(response.getPayload()),
                    MAPPER.getTypeFactory().constructCollectionType(List.class, itemType));
        } catch (Exception e) {
            LOG.error("Failed to parse list payload: {}", e.getMessage());
            throw new ApiException("PARSE_ERROR", "Failed to parse server response.");
        }
    }
}
