package com.labhub.server.handler;

import com.labhub.common.dto.LabDto;
import com.labhub.common.dto.PagedResult;
import com.labhub.common.dto.UserDto;
import com.labhub.common.enums.LabStatus;
import com.labhub.common.enums.UserRole;
import com.labhub.common.protocol.Request;
import com.labhub.common.protocol.Response;
import com.labhub.server.repository.DashboardRepository;
import com.labhub.server.repository.LabRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.Optional;

/**
 * Handles LAB_LIST, LAB_GET, LAB_CREATE, LAB_UPDATE, LAB_DEACTIVATE.
 */
public class LabHandler extends BaseHandler {

    private static final Logger LOG = LoggerFactory.getLogger(LabHandler.class);

    private final LabRepository labRepository;
    private final DashboardRepository dashboardRepository;

    public LabHandler(LabRepository labRepository, DashboardRepository dashboardRepository) {
        this.labRepository = labRepository;
        this.dashboardRepository = dashboardRepository;
    }

    @Override
    public Response handle(Request request, UserDto currentUser) {
        return switch (request.getType()) {
            case LAB_LIST       -> handleList(request, currentUser);
            case LAB_GET        -> handleGet(request, currentUser);
            case LAB_CREATE     -> handleCreate(request, currentUser);
            case LAB_UPDATE     -> handleUpdate(request, currentUser);
            case LAB_DEACTIVATE -> handleDeactivate(request, currentUser);
            default -> Response.error(request.getRequestId(), "INVALID_REQUEST",
                    "LabHandler does not handle " + request.getType());
        };
    }

    private Response handleList(Request request, UserDto currentUser) {
        int page     = intOrDefault(request.getPayload(), "page",     1);
        int pageSize = intOrDefault(request.getPayload(), "pageSize", 20);
        String search = textOrNull(request.getPayload(), "search");
        PagedResult<LabDto> result = labRepository.findAll(search, null, page, pageSize);
        return Response.ok(request.getRequestId(), "Labs loaded.", toJsonNode(result));
    }

    private Response handleGet(Request request, UserDto currentUser) {
        long id = longOrDefault(request.getPayload(), "id", 0);
        if (id <= 0) {
            return Response.error(request.getRequestId(), "VALIDATION_ERROR", "Lab ID required.");
        }
        Optional<LabDto> lab = labRepository.findById(id);
        return lab.map(l -> Response.ok(request.getRequestId(), "OK", toJsonNode(l)))
                  .orElseGet(() -> Response.error(request.getRequestId(), "NOT_FOUND", "Lab not found."));
    }

    private Response handleCreate(Request request, UserDto currentUser) {
        Response authError = requireRole(currentUser, request.getRequestId(),
                UserRole.ADMIN, UserRole.LAB_MANAGER);
        if (authError != null) {
            return authError;
        }
        LabDto dto = parsePayload(request, LabDto.class);
        if (dto == null) {
            return missingPayload(request.getRequestId());
        }
        if (dto.getLabCode() == null || dto.getName() == null || dto.getBuilding() == null) {
            return Response.error(request.getRequestId(), "VALIDATION_ERROR",
                    "Lab code, name, and building are required.");
        }
        if (dto.getCapacity() == null || dto.getCapacity() <= 0) {
            return Response.error(request.getRequestId(), "VALIDATION_ERROR",
                    "Capacity must be a positive number.");
        }
        try {
            long newId = labRepository.create(dto);
            dashboardRepository.log(currentUser.getId(), "LAB_CREATED", "LAB", newId,
                    "Created lab: " + dto.getName() + " (" + dto.getLabCode() + ")");
            return Response.ok(request.getRequestId(), "Lab created.",
                    toJsonNode(labRepository.findById(newId).orElseThrow()));
        } catch (SQLException e) {
            if (e.getMessage() != null && e.getMessage().contains("Duplicate")) {
                return Response.error(request.getRequestId(), "CONFLICT",
                        "Lab code '" + dto.getLabCode() + "' already exists.");
            }
            LOG.error("Failed to create lab: {}", e.getMessage(), e);
            return Response.error(request.getRequestId(), "DATABASE_ERROR", "Failed to create lab.");
        }
    }

    private Response handleUpdate(Request request, UserDto currentUser) {
        Response authError = requireRole(currentUser, request.getRequestId(),
                UserRole.ADMIN, UserRole.LAB_MANAGER);
        if (authError != null) {
            return authError;
        }
        LabDto dto = parsePayload(request, LabDto.class);
        if (dto == null || dto.getId() == null) {
            return missingPayload(request.getRequestId());
        }
        if (labRepository.findById(dto.getId()).isEmpty()) {
            return Response.error(request.getRequestId(), "NOT_FOUND", "Lab not found.");
        }
        try {
            labRepository.update(dto.getId(), dto);
            dashboardRepository.log(currentUser.getId(), "LAB_UPDATED", "LAB", dto.getId(), null);
            return Response.ok(request.getRequestId(), "Lab updated.",
                    toJsonNode(labRepository.findById(dto.getId()).orElseThrow()));
        } catch (SQLException e) {
            LOG.error("Failed to update lab {}: {}", dto.getId(), e.getMessage(), e);
            return Response.error(request.getRequestId(), "DATABASE_ERROR", "Failed to update lab.");
        }
    }

    private Response handleDeactivate(Request request, UserDto currentUser) {
        Response authError = requireRole(currentUser, request.getRequestId(), UserRole.ADMIN);
        if (authError != null) {
            return authError;
        }
        long id = longOrDefault(request.getPayload(), "id", 0);
        if (id <= 0) {
            return Response.error(request.getRequestId(), "VALIDATION_ERROR", "Lab ID required.");
        }
        try {
            labRepository.setStatus(id, LabStatus.INACTIVE);
            dashboardRepository.log(currentUser.getId(), "LAB_DEACTIVATED", "LAB", id, null);
            return Response.ok(request.getRequestId(), "Lab deactivated.");
        } catch (SQLException e) {
            LOG.error("Failed to deactivate lab {}: {}", id, e.getMessage(), e);
            return Response.error(request.getRequestId(), "DATABASE_ERROR", "Failed to deactivate lab.");
        }
    }
}
