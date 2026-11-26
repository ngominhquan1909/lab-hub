package com.labhub.server.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.labhub.common.dto.BorrowRequestDto;
import com.labhub.common.dto.PagedResult;
import com.labhub.common.dto.UserDto;
import com.labhub.common.enums.BorrowStatus;
import com.labhub.common.enums.EquipmentCondition;
import com.labhub.common.enums.UserRole;
import com.labhub.common.protocol.Request;
import com.labhub.common.protocol.Response;
import com.labhub.server.service.BorrowService;
import com.labhub.server.service.ServiceResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Handles all borrow request operations:
 * LIST, GET, CREATE, APPROVE, REJECT, CHECKOUT, RETURN, CANCEL.
 */
public class BorrowHandler extends BaseHandler {

    private final BorrowService borrowService;

    /**
     * @param borrowService borrow business logic
     */
    public BorrowHandler(BorrowService borrowService) {
        this.borrowService = borrowService;
    }

    @Override
    public Response handle(Request request, UserDto currentUser) {
        return switch (request.getType()) {
            case BORROW_LIST     -> handleList(request, currentUser);
            case BORROW_GET      -> handleGet(request, currentUser);
            case BORROW_CREATE   -> handleCreate(request, currentUser);
            case BORROW_APPROVE  -> handleApprove(request, currentUser);
            case BORROW_REJECT   -> handleReject(request, currentUser);
            case BORROW_CHECKOUT -> handleCheckout(request, currentUser);
            case BORROW_RETURN   -> handleReturn(request, currentUser);
            case BORROW_CANCEL   -> handleCancel(request, currentUser);
            default -> Response.error(request.getRequestId(), "INVALID_REQUEST",
                    "BorrowHandler does not handle " + request.getType());
        };
    }

    // ── Handlers ──────────────────────────────────────────────────────────

    private Response handleList(Request request, UserDto currentUser) {
        JsonNode p = request.getPayload();
        BorrowStatus status = null;
        if (p != null && p.has("status")) {
            try {
                status = BorrowStatus.valueOf(p.get("status").asText());
            } catch (IllegalArgumentException ignored) { }
        }
        int page     = intOrDefault(p, "page",     1);
        int pageSize = intOrDefault(p, "pageSize", 20);

        PagedResult<BorrowRequestDto> result = borrowService.list(currentUser, status, page, pageSize);
        return Response.ok(request.getRequestId(), "Borrow requests loaded.", toJsonNode(result));
    }

    private Response handleGet(Request request, UserDto currentUser) {
        long id = longOrDefault(request.getPayload(), "id", 0);
        if (id <= 0) {
            return Response.error(request.getRequestId(), "VALIDATION_ERROR", "Request ID required.");
        }
        Optional<BorrowRequestDto> req = borrowService.getById(id, currentUser);
        return req.map(r -> Response.ok(request.getRequestId(), "OK", toJsonNode(r)))
                  .orElseGet(() -> Response.error(request.getRequestId(), "NOT_FOUND",
                          "Borrow request not found."));
    }

    private Response handleCreate(Request request, UserDto currentUser) {
        JsonNode p = request.getPayload();
        if (p == null) {
            return missingPayload(request.getRequestId());
        }
        BorrowRequestDto dto = new BorrowRequestDto();
        dto.setPurpose(textOrNull(p, "purpose"));

        // expectedReturnAt as ISO-8601 string
        String returnAtStr = textOrNull(p, "expectedReturnAt");
        if (returnAtStr != null) {
            try {
                dto.setExpectedReturnAt(java.time.LocalDateTime.parse(returnAtStr));
            } catch (java.time.format.DateTimeParseException e) {
                return Response.error(request.getRequestId(), "VALIDATION_ERROR",
                        "Invalid expectedReturnAt format. Use ISO-8601 (YYYY-MM-DDTHH:mm:ss).");
            }
        }

        // Parse equipment ID list
        List<Long> equipmentIds = new ArrayList<>();
        if (p.has("equipmentIds") && p.get("equipmentIds").isArray()) {
            for (JsonNode idNode : p.get("equipmentIds")) {
                equipmentIds.add(idNode.asLong());
            }
        }

        ServiceResult<BorrowRequestDto> result = borrowService.create(dto, equipmentIds, currentUser);
        return result.isSuccess()
                ? Response.ok(request.getRequestId(), "Borrow request submitted.",
                    toJsonNode(result.getData()))
                : Response.error(request.getRequestId(), "VALIDATION_ERROR", result.getErrorMessage());
    }

    private Response handleApprove(Request request, UserDto currentUser) {
        Response authError = requireRole(currentUser, request.getRequestId(),
                UserRole.ADMIN, UserRole.LAB_MANAGER);
        if (authError != null) {
            return authError;
        }
        JsonNode p = request.getPayload();
        long id = longOrDefault(p, "id", 0);
        if (id <= 0) {
            return Response.error(request.getRequestId(), "VALIDATION_ERROR", "Request ID required.");
        }
        ServiceResult<BorrowRequestDto> result = borrowService.approve(
                id, currentUser, textOrNull(p, "note"));
        return result.isSuccess()
                ? Response.ok(request.getRequestId(), "Borrow request approved.",
                    toJsonNode(result.getData()))
                : Response.error(request.getRequestId(), "CONFLICT", result.getErrorMessage());
    }

    private Response handleReject(Request request, UserDto currentUser) {
        Response authError = requireRole(currentUser, request.getRequestId(),
                UserRole.ADMIN, UserRole.LAB_MANAGER);
        if (authError != null) {
            return authError;
        }
        JsonNode p = request.getPayload();
        long id = longOrDefault(p, "id", 0);
        ServiceResult<BorrowRequestDto> result = borrowService.reject(
                id, currentUser, textOrNull(p, "note"));
        return result.isSuccess()
                ? Response.ok(request.getRequestId(), "Borrow request rejected.",
                    toJsonNode(result.getData()))
                : Response.error(request.getRequestId(), "VALIDATION_ERROR", result.getErrorMessage());
    }

    private Response handleCheckout(Request request, UserDto currentUser) {
        Response authError = requireRole(currentUser, request.getRequestId(),
                UserRole.ADMIN, UserRole.LAB_MANAGER);
        if (authError != null) {
            return authError;
        }
        JsonNode p = request.getPayload();
        long id = longOrDefault(p, "id", 0);
        Map<Long, EquipmentCondition> conditions = parseConditions(p, "itemConditions");
        ServiceResult<BorrowRequestDto> result = borrowService.checkout(id, currentUser, conditions);
        return result.isSuccess()
                ? Response.ok(request.getRequestId(), "Checkout recorded.", toJsonNode(result.getData()))
                : Response.error(request.getRequestId(), "CONFLICT", result.getErrorMessage());
    }

    private Response handleReturn(Request request, UserDto currentUser) {
        Response authError = requireRole(currentUser, request.getRequestId(),
                UserRole.ADMIN, UserRole.LAB_MANAGER);
        if (authError != null) {
            return authError;
        }
        JsonNode p = request.getPayload();
        long id = longOrDefault(p, "id", 0);
        Map<Long, EquipmentCondition> conditions = parseConditions(p, "itemConditions");
        ServiceResult<BorrowRequestDto> result = borrowService.returnEquipment(id, currentUser, conditions);
        return result.isSuccess()
                ? Response.ok(request.getRequestId(), "Return recorded.", toJsonNode(result.getData()))
                : Response.error(request.getRequestId(), "CONFLICT", result.getErrorMessage());
    }

    private Response handleCancel(Request request, UserDto currentUser) {
        // Cancellation allowed by requester or manager — business logic enforces scope
        long id = longOrDefault(request.getPayload(), "id", 0);
        if (id <= 0) {
            return Response.error(request.getRequestId(), "VALIDATION_ERROR", "Request ID required.");
        }
        ServiceResult<BorrowRequestDto> result = borrowService.cancel(id, currentUser);
        return result.isSuccess()
                ? Response.ok(request.getRequestId(), "Borrow request cancelled.",
                    toJsonNode(result.getData()))
                : Response.error(request.getRequestId(), "CONFLICT", result.getErrorMessage());
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    /**
     * Parses a map of equipmentId → EquipmentCondition from a JSON object field.
     *
     * @param payload   parent node
     * @param fieldName field containing the map
     * @return parsed conditions or empty map
     */
    private Map<Long, EquipmentCondition> parseConditions(JsonNode payload, String fieldName) {
        Map<Long, EquipmentCondition> result = new HashMap<>();
        if (payload == null || !payload.has(fieldName)) {
            return result;
        }
        payload.get(fieldName).fields().forEachRemaining(entry -> {
            try {
                result.put(Long.parseLong(entry.getKey()),
                        EquipmentCondition.valueOf(entry.getValue().asText()));
            } catch (IllegalArgumentException ignored) { }
        });
        return result;
    }
}
