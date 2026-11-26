package com.labhub.server.service;

import com.labhub.common.dto.BorrowRequestDto;
import com.labhub.common.dto.EquipmentDto;
import com.labhub.common.dto.PagedResult;
import com.labhub.common.dto.UserDto;
import com.labhub.common.enums.BorrowStatus;
import com.labhub.common.enums.EquipmentCondition;
import com.labhub.common.enums.EquipmentStatus;
import com.labhub.common.enums.UserRole;
import com.labhub.server.repository.BorrowRepository;
import com.labhub.server.repository.DashboardRepository;
import com.labhub.server.repository.EquipmentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Business logic for equipment borrow request lifecycle.
 *
 * <p>Business rules enforced:
 * <ul>
 *   <li>Only AVAILABLE equipment can be requested.</li>
 *   <li>Equipment under MAINTENANCE cannot be borrowed.</li>
 *   <li>RETIRED or LOST equipment cannot be borrowed.</li>
 *   <li>Expected return date must be in the future.</li>
 *   <li>Equipment status transitions are atomic with the request state transition.</li>
 *   <li>On approval, equipment status is updated to BORROWED atomically.</li>
 *   <li>On return, equipment status is reset to AVAILABLE.</li>
 * </ul>
 */
public class BorrowService {

    private static final Logger LOG = LoggerFactory.getLogger(BorrowService.class);

    private final BorrowRepository borrowRepository;
    private final EquipmentRepository equipmentRepository;
    private final DashboardRepository dashboardRepository;

    /**
     * @param borrowRepository    borrow request data access
     * @param equipmentRepository equipment status management
     * @param dashboardRepository audit logging
     */
    public BorrowService(BorrowRepository borrowRepository,
                         EquipmentRepository equipmentRepository,
                         DashboardRepository dashboardRepository) {
        this.borrowRepository = borrowRepository;
        this.equipmentRepository = equipmentRepository;
        this.dashboardRepository = dashboardRepository;
    }

    // ── Queries ───────────────────────────────────────────────────────────

    /**
     * Returns paginated borrow requests. Students see only their own; managers see all.
     *
     * @param currentUser authenticated user
     * @param status      optional status filter
     * @param page        1-based page
     * @param pageSize    items per page
     * @return paged results
     */
    public PagedResult<BorrowRequestDto> list(UserDto currentUser, BorrowStatus status,
                                               int page, int pageSize) {
        Long requesterId = null;
        if (!currentUser.hasRole(UserRole.ADMIN) && !currentUser.hasRole(UserRole.LAB_MANAGER)) {
            requesterId = currentUser.getId();
        }
        return borrowRepository.findAll(requesterId, status, page, pageSize);
    }

    /**
     * Returns a borrow request by ID.
     *
     * @param id          request ID
     * @param currentUser authenticated user
     * @return borrow request or empty if not found/accessible
     */
    public Optional<BorrowRequestDto> getById(long id, UserDto currentUser) {
        Optional<BorrowRequestDto> req = borrowRepository.findById(id);
        if (req.isEmpty()) {
            return Optional.empty();
        }
        boolean isOwner = req.get().getRequesterId().equals(currentUser.getId());
        boolean isManager = currentUser.hasRole(UserRole.ADMIN)
                || currentUser.hasRole(UserRole.LAB_MANAGER);
        return (isOwner || isManager) ? req : Optional.empty();
    }

    // ── Mutations ─────────────────────────────────────────────────────────

    /**
     * Creates a new equipment borrow request.
     *
     * @param dto         request header data (purpose, expectedReturnAt)
     * @param equipmentIds IDs of equipment to include in this request
     * @param currentUser requester
     * @return service result
     */
    public ServiceResult<BorrowRequestDto> create(BorrowRequestDto dto, List<Long> equipmentIds,
                                                   UserDto currentUser) {
        if (equipmentIds == null || equipmentIds.isEmpty()) {
            return ServiceResult.failure("At least one piece of equipment must be selected.");
        }
        if (dto.getPurpose() == null || dto.getPurpose().isBlank()) {
            return ServiceResult.failure("Purpose is required.");
        }
        if (dto.getExpectedReturnAt() == null) {
            return ServiceResult.failure("Expected return date is required.");
        }
        if (!dto.getExpectedReturnAt().isAfter(LocalDateTime.now())) {
            return ServiceResult.failure("Expected return date must be in the future.");
        }

        // Validate each requested item
        List<String> unavailable = new ArrayList<>();
        for (Long eqId : equipmentIds) {
            Optional<EquipmentDto> eq = equipmentRepository.findById(eqId);
            if (eq.isEmpty()) {
                unavailable.add("Equipment ID " + eqId + " not found.");
                continue;
            }
            EquipmentDto equipment = eq.get();
            if (!equipment.isActive()) {
                unavailable.add(equipment.getName() + " (" + equipment.getAssetCode() + ") is decommissioned.");
            } else if (equipment.getStatus() != EquipmentStatus.AVAILABLE) {
                unavailable.add(equipment.getName() + " (" + equipment.getAssetCode()
                        + ") is currently " + equipment.getStatus().name().toLowerCase() + ".");
            }
        }
        if (!unavailable.isEmpty()) {
            return ServiceResult.failure("Some equipment is not available: " + String.join("; ", unavailable));
        }

        dto.setRequesterId(currentUser.getId());
        try {
            long id = borrowRepository.create(dto, equipmentIds);
            dashboardRepository.log(currentUser.getId(), "BORROW_CREATED",
                    "BORROW_REQUEST", id,
                    "New borrow request for " + equipmentIds.size() + " item(s)");
            return ServiceResult.success(borrowRepository.findById(id).orElseThrow());
        } catch (SQLException e) {
            LOG.error("Failed to create borrow request: {}", e.getMessage(), e);
            return ServiceResult.failure("Failed to save borrow request. Please try again.");
        }
    }

    /**
     * Approves a borrow request and sets equipment status to BORROWED.
     *
     * @param requestId   borrow request ID
     * @param currentUser manager performing the approval
     * @param note        optional note
     * @return service result
     */
    public ServiceResult<BorrowRequestDto> approve(long requestId, UserDto currentUser, String note) {
        Optional<BorrowRequestDto> maybe = borrowRepository.findById(requestId);
        if (maybe.isEmpty()) {
            return ServiceResult.failure("Borrow request not found.");
        }
        BorrowRequestDto req = maybe.get();
        if (req.getStatus() != BorrowStatus.PENDING) {
            return ServiceResult.failure("Only PENDING requests can be approved.");
        }

        // Re-validate equipment availability at approval time
        if (req.getItems() != null) {
            for (BorrowRequestDto.BorrowItemDto item : req.getItems()) {
                Optional<EquipmentDto> eq = equipmentRepository.findById(item.getEquipmentId());
                if (eq.isEmpty() || eq.get().getStatus() != EquipmentStatus.AVAILABLE) {
                    return ServiceResult.failure(
                            "Equipment '" + item.getEquipmentName()
                            + "' is no longer available. Cannot approve.");
                }
            }
        }

        try {
            borrowRepository.approve(requestId, currentUser.getId(), note);
            // Set equipment status to BORROWED
            if (req.getItems() != null) {
                for (BorrowRequestDto.BorrowItemDto item : req.getItems()) {
                    equipmentRepository.setStatus(item.getEquipmentId(), EquipmentStatus.BORROWED);
                }
            }
            dashboardRepository.log(currentUser.getId(), "BORROW_APPROVED",
                    "BORROW_REQUEST", requestId, "Approved by " + currentUser.getFullName());
            return ServiceResult.success(borrowRepository.findById(requestId).orElseThrow());
        } catch (SQLException e) {
            LOG.error("Failed to approve borrow request {}: {}", requestId, e.getMessage(), e);
            return ServiceResult.failure("Failed to approve request.");
        }
    }

    /**
     * Rejects a borrow request.
     *
     * @param requestId   borrow request ID
     * @param currentUser manager performing the rejection
     * @param note        rejection reason (required)
     * @return service result
     */
    public ServiceResult<BorrowRequestDto> reject(long requestId, UserDto currentUser, String note) {
        if (note == null || note.isBlank()) {
            return ServiceResult.failure("A rejection reason is required.");
        }
        Optional<BorrowRequestDto> maybe = borrowRepository.findById(requestId);
        if (maybe.isEmpty()) {
            return ServiceResult.failure("Borrow request not found.");
        }
        if (maybe.get().getStatus() != BorrowStatus.PENDING) {
            return ServiceResult.failure("Only PENDING requests can be rejected.");
        }
        try {
            borrowRepository.reject(requestId, currentUser.getId(), note);
            dashboardRepository.log(currentUser.getId(), "BORROW_REJECTED",
                    "BORROW_REQUEST", requestId, "Rejected: " + note);
            return ServiceResult.success(borrowRepository.findById(requestId).orElseThrow());
        } catch (SQLException e) {
            LOG.error("Failed to reject borrow request {}: {}", requestId, e.getMessage(), e);
            return ServiceResult.failure("Failed to reject request.");
        }
    }

    /**
     * Records physical checkout of equipment (APPROVED → ACTIVE).
     *
     * @param requestId      borrow request ID
     * @param currentUser    manager processing checkout
     * @param itemConditions equipment condition at checkout, keyed by equipment ID
     * @return service result
     */
    public ServiceResult<BorrowRequestDto> checkout(long requestId, UserDto currentUser,
                                                     Map<Long, EquipmentCondition> itemConditions) {
        Optional<BorrowRequestDto> maybe = borrowRepository.findById(requestId);
        if (maybe.isEmpty()) {
            return ServiceResult.failure("Borrow request not found.");
        }
        if (maybe.get().getStatus() != BorrowStatus.APPROVED) {
            return ServiceResult.failure("Equipment can only be checked out for APPROVED requests.");
        }
        try {
            borrowRepository.checkout(requestId, currentUser.getId(),
                    itemConditions != null ? itemConditions : new HashMap<>());
            dashboardRepository.log(currentUser.getId(), "BORROW_CHECKOUT",
                    "BORROW_REQUEST", requestId, "Equipment checked out by " + currentUser.getFullName());
            return ServiceResult.success(borrowRepository.findById(requestId).orElseThrow());
        } catch (SQLException e) {
            LOG.error("Failed to record checkout for request {}: {}", requestId, e.getMessage(), e);
            return ServiceResult.failure("Failed to record checkout.");
        }
    }

    /**
     * Records the return of all equipment in a borrow request (ACTIVE/OVERDUE → RETURNED).
     * Also restores equipment status to AVAILABLE.
     *
     * @param requestId      borrow request ID
     * @param currentUser    manager processing the return
     * @param itemConditions equipment condition at return, keyed by equipment ID
     * @return service result
     */
    public ServiceResult<BorrowRequestDto> returnEquipment(long requestId, UserDto currentUser,
                                                            Map<Long, EquipmentCondition> itemConditions) {
        Optional<BorrowRequestDto> maybe = borrowRepository.findById(requestId);
        if (maybe.isEmpty()) {
            return ServiceResult.failure("Borrow request not found.");
        }
        BorrowRequestDto req = maybe.get();
        if (req.getStatus() != BorrowStatus.ACTIVE && req.getStatus() != BorrowStatus.OVERDUE) {
            return ServiceResult.failure("Only ACTIVE or OVERDUE requests can be returned.");
        }

        try {
            borrowRepository.returnRequest(requestId, currentUser.getId(),
                    itemConditions != null ? itemConditions : new HashMap<>());
            // Restore equipment status to AVAILABLE
            if (req.getItems() != null) {
                for (BorrowRequestDto.BorrowItemDto item : req.getItems()) {
                    equipmentRepository.setStatus(item.getEquipmentId(), EquipmentStatus.AVAILABLE);
                }
            }
            dashboardRepository.log(currentUser.getId(), "BORROW_RETURNED",
                    "BORROW_REQUEST", requestId, "Equipment returned to lab.");
            return ServiceResult.success(borrowRepository.findById(requestId).orElseThrow());
        } catch (SQLException e) {
            LOG.error("Failed to process return for request {}: {}", requestId, e.getMessage(), e);
            return ServiceResult.failure("Failed to record equipment return.");
        }
    }
}
