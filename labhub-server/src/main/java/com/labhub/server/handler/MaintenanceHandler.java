package com.labhub.server.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.labhub.common.dto.MaintenanceTicketDto;
import com.labhub.common.dto.PagedResult;
import com.labhub.common.dto.UserDto;
import com.labhub.common.enums.EquipmentStatus;
import com.labhub.common.enums.MaintenancePriority;
import com.labhub.common.enums.MaintenanceStatus;
import com.labhub.common.enums.UserRole;
import com.labhub.common.protocol.Request;
import com.labhub.common.protocol.Response;
import com.labhub.server.repository.DashboardRepository;
import com.labhub.server.repository.EquipmentRepository;
import com.labhub.server.repository.MaintenanceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.Optional;

/**
 * Handles maintenance ticket operations:
 * LIST, GET, CREATE, UPDATE_STATUS, ASSIGN, RESOLVE, CLOSE.
 *
 * <p>When a ticket is created, the equipment status is set to MAINTENANCE.
 * When a ticket is CLOSED, equipment status returns to AVAILABLE.
 */
public class MaintenanceHandler extends BaseHandler {

    private static final Logger LOG = LoggerFactory.getLogger(MaintenanceHandler.class);

    private final MaintenanceRepository maintenanceRepository;
    private final EquipmentRepository equipmentRepository;
    private final DashboardRepository dashboardRepository;

    /**
     * @param maintenanceRepository ticket data access
     * @param equipmentRepository   equipment status transitions
     * @param dashboardRepository   audit logging
     */
    public MaintenanceHandler(MaintenanceRepository maintenanceRepository,
                               EquipmentRepository equipmentRepository,
                               DashboardRepository dashboardRepository) {
        this.maintenanceRepository = maintenanceRepository;
        this.equipmentRepository = equipmentRepository;
        this.dashboardRepository = dashboardRepository;
    }

    @Override
    public Response handle(Request request, UserDto currentUser) {
        return switch (request.getType()) {
            case MAINTENANCE_LIST          -> handleList(request, currentUser);
            case MAINTENANCE_GET           -> handleGet(request, currentUser);
            case MAINTENANCE_CREATE        -> handleCreate(request, currentUser);
            case MAINTENANCE_UPDATE_STATUS -> handleUpdateStatus(request, currentUser);
            case MAINTENANCE_ASSIGN        -> handleAssign(request, currentUser);
            case MAINTENANCE_RESOLVE       -> handleResolve(request, currentUser);
            case MAINTENANCE_CLOSE         -> handleClose(request, currentUser);
            default -> Response.error(request.getRequestId(), "INVALID_REQUEST",
                    "MaintenanceHandler does not handle " + request.getType());
        };
    }

    // ── Handlers ──────────────────────────────────────────────────────────

    private Response handleList(Request request, UserDto currentUser) {
        int page     = intOrDefault(request.getPayload(), "page",     1);
        int pageSize = intOrDefault(request.getPayload(), "pageSize", 20);

        MaintenanceStatus status = null;
        MaintenancePriority priority = null;
        JsonNode p = request.getPayload();
        if (p != null) {
            if (p.has("status")) {
                try { status = MaintenanceStatus.valueOf(p.get("status").asText()); }
                catch (IllegalArgumentException ignored) { }
            }
            if (p.has("priority")) {
                try { priority = MaintenancePriority.valueOf(p.get("priority").asText()); }
                catch (IllegalArgumentException ignored) { }
            }
        }
        PagedResult<MaintenanceTicketDto> result =
                maintenanceRepository.findAll(null, status, priority, page, pageSize);
        return Response.ok(request.getRequestId(), "Maintenance tickets loaded.", toJsonNode(result));
    }

    private Response handleGet(Request request, UserDto currentUser) {
        long id = longOrDefault(request.getPayload(), "id", 0);
        if (id <= 0) {
            return Response.error(request.getRequestId(), "VALIDATION_ERROR", "Ticket ID required.");
        }
        Optional<MaintenanceTicketDto> ticket = maintenanceRepository.findById(id);
        return ticket.map(t -> Response.ok(request.getRequestId(), "OK", toJsonNode(t)))
                     .orElseGet(() -> Response.error(request.getRequestId(), "NOT_FOUND",
                             "Maintenance ticket not found."));
    }

    private Response handleCreate(Request request, UserDto currentUser) {
        JsonNode p = request.getPayload();
        if (p == null) {
            return missingPayload(request.getRequestId());
        }
        long equipmentId = longOrDefault(p, "equipmentId", 0);
        String description = textOrNull(p, "description");
        if (equipmentId <= 0 || description == null) {
            return Response.error(request.getRequestId(), "VALIDATION_ERROR",
                    "Equipment ID and description are required.");
        }

        MaintenanceTicketDto dto = new MaintenanceTicketDto();
        dto.setEquipmentId(equipmentId);
        dto.setReporterId(currentUser.getId());
        dto.setDescription(description);
        if (p.has("priority")) {
            try {
                dto.setPriority(MaintenancePriority.valueOf(p.get("priority").asText()));
            } catch (IllegalArgumentException e) {
                dto.setPriority(MaintenancePriority.MEDIUM);
            }
        }

        try {
            long ticketId = maintenanceRepository.create(dto);
            // Set equipment to MAINTENANCE status
            equipmentRepository.setStatus(equipmentId, EquipmentStatus.MAINTENANCE);
            dashboardRepository.log(currentUser.getId(), "MAINTENANCE_CREATED",
                    "EQUIPMENT", equipmentId,
                    "Maintenance ticket #" + ticketId + " filed: " + description);
            return Response.ok(request.getRequestId(), "Maintenance ticket created.",
                    toJsonNode(maintenanceRepository.findById(ticketId).orElseThrow()));
        } catch (SQLException e) {
            LOG.error("Failed to create maintenance ticket: {}", e.getMessage(), e);
            return Response.error(request.getRequestId(), "DATABASE_ERROR",
                    "Failed to create maintenance ticket.");
        }
    }

    private Response handleUpdateStatus(Request request, UserDto currentUser) {
        Response authError = requireRole(currentUser, request.getRequestId(),
                UserRole.ADMIN, UserRole.LAB_MANAGER);
        if (authError != null) {
            return authError;
        }
        JsonNode p = request.getPayload();
        long id = longOrDefault(p, "id", 0);
        String statusStr = textOrNull(p, "status");
        if (id <= 0 || statusStr == null) {
            return Response.error(request.getRequestId(), "VALIDATION_ERROR",
                    "Ticket ID and status are required.");
        }
        MaintenanceStatus newStatus;
        try {
            newStatus = MaintenanceStatus.valueOf(statusStr);
        } catch (IllegalArgumentException e) {
            return Response.error(request.getRequestId(), "VALIDATION_ERROR",
                    "Invalid status: " + statusStr);
        }
        try {
            maintenanceRepository.updateStatus(id, newStatus);
            dashboardRepository.log(currentUser.getId(), "MAINTENANCE_STATUS_CHANGED",
                    "MAINTENANCE_TICKET", id, "Status → " + newStatus.name());
            return Response.ok(request.getRequestId(), "Status updated.",
                    toJsonNode(maintenanceRepository.findById(id).orElseThrow()));
        } catch (SQLException e) {
            LOG.error("Failed to update maintenance status: {}", e.getMessage(), e);
            return Response.error(request.getRequestId(), "DATABASE_ERROR", "Failed to update status.");
        }
    }

    private Response handleAssign(Request request, UserDto currentUser) {
        Response authError = requireRole(currentUser, request.getRequestId(),
                UserRole.ADMIN, UserRole.LAB_MANAGER);
        if (authError != null) {
            return authError;
        }
        JsonNode p = request.getPayload();
        long ticketId  = longOrDefault(p, "id",         0);
        long assigneeId = longOrDefault(p, "assigneeId", 0);
        if (ticketId <= 0 || assigneeId <= 0) {
            return Response.error(request.getRequestId(), "VALIDATION_ERROR",
                    "Ticket ID and assignee ID are required.");
        }
        try {
            maintenanceRepository.assign(ticketId, assigneeId);
            dashboardRepository.log(currentUser.getId(), "MAINTENANCE_ASSIGNED",
                    "MAINTENANCE_TICKET", ticketId, "Assigned to user " + assigneeId);
            return Response.ok(request.getRequestId(), "Ticket assigned.",
                    toJsonNode(maintenanceRepository.findById(ticketId).orElseThrow()));
        } catch (SQLException e) {
            LOG.error("Failed to assign ticket: {}", e.getMessage(), e);
            return Response.error(request.getRequestId(), "DATABASE_ERROR", "Failed to assign ticket.");
        }
    }

    private Response handleResolve(Request request, UserDto currentUser) {
        Response authError = requireRole(currentUser, request.getRequestId(),
                UserRole.ADMIN, UserRole.LAB_MANAGER);
        if (authError != null) {
            return authError;
        }
        JsonNode p = request.getPayload();
        long id = longOrDefault(p, "id", 0);
        String notes = textOrNull(p, "resolutionNotes");
        BigDecimal cost = p != null && p.has("repairCost") && !p.get("repairCost").isNull()
                ? p.get("repairCost").decimalValue() : null;
        if (id <= 0 || notes == null) {
            return Response.error(request.getRequestId(), "VALIDATION_ERROR",
                    "Ticket ID and resolution notes are required.");
        }
        try {
            maintenanceRepository.resolve(id, notes, cost);
            dashboardRepository.log(currentUser.getId(), "MAINTENANCE_RESOLVED",
                    "MAINTENANCE_TICKET", id, "Resolved: " + notes);
            return Response.ok(request.getRequestId(), "Ticket resolved.",
                    toJsonNode(maintenanceRepository.findById(id).orElseThrow()));
        } catch (SQLException e) {
            LOG.error("Failed to resolve ticket {}: {}", id, e.getMessage(), e);
            return Response.error(request.getRequestId(), "CONFLICT", e.getMessage());
        }
    }

    private Response handleClose(Request request, UserDto currentUser) {
        Response authError = requireRole(currentUser, request.getRequestId(),
                UserRole.ADMIN, UserRole.LAB_MANAGER);
        if (authError != null) {
            return authError;
        }
        long id = longOrDefault(request.getPayload(), "id", 0);
        if (id <= 0) {
            return Response.error(request.getRequestId(), "VALIDATION_ERROR", "Ticket ID required.");
        }

        // Look up equipment ID before closing so we can restore its status
        Optional<MaintenanceTicketDto> maybeTicket = maintenanceRepository.findById(id);
        if (maybeTicket.isEmpty()) {
            return Response.error(request.getRequestId(), "NOT_FOUND", "Ticket not found.");
        }
        long equipmentId = maybeTicket.get().getEquipmentId();

        try {
            maintenanceRepository.close(id);
            // Equipment is repaired — restore to AVAILABLE
            equipmentRepository.setStatus(equipmentId, EquipmentStatus.AVAILABLE);
            dashboardRepository.log(currentUser.getId(), "MAINTENANCE_CLOSED",
                    "MAINTENANCE_TICKET", id, "Ticket closed; equipment restored to AVAILABLE.");
            return Response.ok(request.getRequestId(), "Ticket closed. Equipment restored to available.");
        } catch (SQLException e) {
            LOG.error("Failed to close ticket {}: {}", id, e.getMessage(), e);
            return Response.error(request.getRequestId(), "CONFLICT", e.getMessage());
        }
    }
}
