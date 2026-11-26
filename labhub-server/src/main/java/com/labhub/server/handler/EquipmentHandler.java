package com.labhub.server.handler;

import com.labhub.common.dto.CategoryDto;
import com.labhub.common.dto.EquipmentDto;
import com.labhub.common.dto.PagedResult;
import com.labhub.common.dto.UserDto;
import com.labhub.common.enums.EquipmentStatus;
import com.labhub.common.enums.UserRole;
import com.labhub.common.protocol.Request;
import com.labhub.common.protocol.Response;
import com.labhub.server.repository.DashboardRepository;
import com.labhub.server.repository.EquipmentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Handles EQUIPMENT_LIST, EQUIPMENT_GET, EQUIPMENT_CREATE, EQUIPMENT_UPDATE,
 * EQUIPMENT_DEACTIVATE, CATEGORY_LIST, CATEGORY_CREATE.
 */
public class EquipmentHandler extends BaseHandler {

    private static final Logger LOG = LoggerFactory.getLogger(EquipmentHandler.class);

    private final EquipmentRepository equipmentRepository;
    private final DashboardRepository dashboardRepository;
    private final com.labhub.server.config.DatabaseManager db;

    /**
     * @param equipmentRepository equipment data access
     * @param dashboardRepository audit logging
     * @param db                  raw DB access for category operations
     */
    public EquipmentHandler(EquipmentRepository equipmentRepository,
                             DashboardRepository dashboardRepository,
                             com.labhub.server.config.DatabaseManager db) {
        this.equipmentRepository = equipmentRepository;
        this.dashboardRepository = dashboardRepository;
        this.db = db;
    }

    @Override
    public Response handle(Request request, UserDto currentUser) {
        return switch (request.getType()) {
            case EQUIPMENT_LIST       -> handleList(request, currentUser);
            case EQUIPMENT_GET        -> handleGet(request, currentUser);
            case EQUIPMENT_CREATE     -> handleCreate(request, currentUser);
            case EQUIPMENT_UPDATE     -> handleUpdate(request, currentUser);
            case EQUIPMENT_DEACTIVATE -> handleDeactivate(request, currentUser);
            case CATEGORY_LIST        -> handleCategoryList(request);
            case CATEGORY_CREATE      -> handleCategoryCreate(request, currentUser);
            default -> Response.error(request.getRequestId(), "INVALID_REQUEST",
                    "EquipmentHandler does not handle " + request.getType());
        };
    }

    // ── Handlers ──────────────────────────────────────────────────────────

    private Response handleList(Request request, UserDto currentUser) {
        int page     = intOrDefault(request.getPayload(), "page",     1);
        int pageSize = intOrDefault(request.getPayload(), "pageSize", 25);
        String search = textOrNull(request.getPayload(), "search");

        Long labId = null;
        if (request.getPayload() != null && request.getPayload().has("labId")) {
            labId = request.getPayload().get("labId").asLong();
        }
        EquipmentStatus status = null;
        if (request.getPayload() != null && request.getPayload().has("status")) {
            try {
                status = EquipmentStatus.valueOf(request.getPayload().get("status").asText());
            } catch (IllegalArgumentException ignored) { }
        }
        PagedResult<EquipmentDto> result = equipmentRepository.findAll(search, labId, status, page, pageSize);
        return Response.ok(request.getRequestId(), "Equipment loaded.", toJsonNode(result));
    }

    private Response handleGet(Request request, UserDto currentUser) {
        long id = longOrDefault(request.getPayload(), "id", 0);
        if (id <= 0) {
            return Response.error(request.getRequestId(), "VALIDATION_ERROR", "Equipment ID required.");
        }
        Optional<EquipmentDto> eq = equipmentRepository.findById(id);
        return eq.map(e -> Response.ok(request.getRequestId(), "OK", toJsonNode(e)))
                 .orElseGet(() -> Response.error(request.getRequestId(), "NOT_FOUND",
                         "Equipment not found."));
    }

    private Response handleCreate(Request request, UserDto currentUser) {
        Response authError = requireRole(currentUser, request.getRequestId(),
                UserRole.ADMIN, UserRole.LAB_MANAGER);
        if (authError != null) {
            return authError;
        }
        EquipmentDto dto = parsePayload(request, EquipmentDto.class);
        if (dto == null) {
            return missingPayload(request.getRequestId());
        }
        if (dto.getAssetCode() == null || dto.getName() == null || dto.getCategoryId() == null) {
            return Response.error(request.getRequestId(), "VALIDATION_ERROR",
                    "Asset code, name, and category are required.");
        }
        try {
            long newId = equipmentRepository.create(dto);
            dashboardRepository.log(currentUser.getId(), "EQUIPMENT_CREATED", "EQUIPMENT", newId,
                    "Created: " + dto.getName() + " (" + dto.getAssetCode() + ")");
            return Response.ok(request.getRequestId(), "Equipment added.",
                    toJsonNode(equipmentRepository.findById(newId).orElseThrow()));
        } catch (SQLException e) {
            if (e.getMessage() != null && e.getMessage().contains("Duplicate")) {
                return Response.error(request.getRequestId(), "CONFLICT",
                        "Asset code '" + dto.getAssetCode() + "' already exists.");
            }
            LOG.error("Failed to create equipment: {}", e.getMessage(), e);
            return Response.error(request.getRequestId(), "DATABASE_ERROR", "Failed to add equipment.");
        }
    }

    private Response handleUpdate(Request request, UserDto currentUser) {
        Response authError = requireRole(currentUser, request.getRequestId(),
                UserRole.ADMIN, UserRole.LAB_MANAGER);
        if (authError != null) {
            return authError;
        }
        EquipmentDto dto = parsePayload(request, EquipmentDto.class);
        if (dto == null || dto.getId() == null) {
            return missingPayload(request.getRequestId());
        }
        if (equipmentRepository.findById(dto.getId()).isEmpty()) {
            return Response.error(request.getRequestId(), "NOT_FOUND", "Equipment not found.");
        }
        try {
            equipmentRepository.update(dto.getId(), dto);
            dashboardRepository.log(currentUser.getId(), "EQUIPMENT_UPDATED",
                    "EQUIPMENT", dto.getId(), null);
            return Response.ok(request.getRequestId(), "Equipment updated.",
                    toJsonNode(equipmentRepository.findById(dto.getId()).orElseThrow()));
        } catch (SQLException e) {
            LOG.error("Failed to update equipment {}: {}", dto.getId(), e.getMessage(), e);
            return Response.error(request.getRequestId(), "DATABASE_ERROR", "Failed to update equipment.");
        }
    }

    private Response handleDeactivate(Request request, UserDto currentUser) {
        Response authError = requireRole(currentUser, request.getRequestId(),
                UserRole.ADMIN, UserRole.LAB_MANAGER);
        if (authError != null) {
            return authError;
        }
        long id = longOrDefault(request.getPayload(), "id", 0);
        if (id <= 0) {
            return Response.error(request.getRequestId(), "VALIDATION_ERROR", "Equipment ID required.");
        }
        try {
            equipmentRepository.deactivate(id);
            dashboardRepository.log(currentUser.getId(), "EQUIPMENT_DEACTIVATED",
                    "EQUIPMENT", id, null);
            return Response.ok(request.getRequestId(), "Equipment deactivated.");
        } catch (SQLException e) {
            LOG.error("Failed to deactivate equipment {}: {}", id, e.getMessage(), e);
            return Response.error(request.getRequestId(), "DATABASE_ERROR", "Failed to deactivate.");
        }
    }

    private Response handleCategoryList(Request request) {
        List<CategoryDto> categories = new ArrayList<>();
        String sql = "SELECT ec.id, ec.name, ec.description, "
                + "(SELECT COUNT(*) FROM equipment e WHERE e.category_id = ec.id AND e.is_active = 1) AS cnt "
                + "FROM equipment_categories ec ORDER BY ec.name";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                CategoryDto dto = new CategoryDto();
                dto.setId(rs.getLong("id"));
                dto.setName(rs.getString("name"));
                dto.setDescription(rs.getString("description"));
                dto.setEquipmentCount(rs.getInt("cnt"));
                categories.add(dto);
            }
        } catch (SQLException e) {
            LOG.error("Failed to load categories: {}", e.getMessage(), e);
            return Response.error(request.getRequestId(), "DATABASE_ERROR", "Failed to load categories.");
        }
        return Response.ok(request.getRequestId(), "Categories loaded.", toJsonNode(categories));
    }

    private Response handleCategoryCreate(Request request, UserDto currentUser) {
        Response authError = requireRole(currentUser, request.getRequestId(),
                UserRole.ADMIN, UserRole.LAB_MANAGER);
        if (authError != null) {
            return authError;
        }
        String name = textOrNull(request.getPayload(), "name");
        String desc = textOrNull(request.getPayload(), "description");
        if (name == null) {
            return Response.error(request.getRequestId(), "VALIDATION_ERROR", "Category name is required.");
        }
        String sql = "INSERT INTO equipment_categories (name, description) VALUES (?, ?)";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.setString(2, desc);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                long newId = keys.getLong(1);
                dashboardRepository.log(currentUser.getId(), "CATEGORY_CREATED",
                        "CATEGORY", newId, "Category: " + name);
                return Response.ok(request.getRequestId(), "Category created.");
            }
        } catch (SQLException e) {
            if (e.getMessage() != null && e.getMessage().contains("Duplicate")) {
                return Response.error(request.getRequestId(), "CONFLICT",
                        "Category '" + name + "' already exists.");
            }
            LOG.error("Failed to create category: {}", e.getMessage(), e);
            return Response.error(request.getRequestId(), "DATABASE_ERROR", "Failed to create category.");
        }
    }
}
