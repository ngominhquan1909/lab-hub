package com.labhub.common.dto;

import com.labhub.common.enums.BorrowStatus;
import com.labhub.common.enums.EquipmentCondition;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Data transfer object representing an equipment borrowing request with its line items.
 */
public class BorrowRequestDto {

    private Long id;
    private Long requesterId;
    private String requesterName;
    private String purpose;
    private LocalDateTime expectedReturnAt;
    private LocalDateTime checkedOutAt;
    private LocalDateTime actualReturnAt;
    private Long processedById;
    private String processedByName;
    private BorrowStatus status;
    private String decisionNote;
    private List<BorrowItemDto> items;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** Required by Jackson. */
    public BorrowRequestDto() { }

    // ── Getters / setters ──────────────────────────────────────────────────

    /** @return internal borrow request ID */
    public Long getId() { return id; }

    /** @param id internal ID */
    public void setId(Long id) { this.id = id; }

    /** @return ID of the borrower */
    public Long getRequesterId() { return requesterId; }

    /** @param requesterId requester ID */
    public void setRequesterId(Long requesterId) { this.requesterId = requesterId; }

    /** @return borrower display name */
    public String getRequesterName() { return requesterName; }

    /** @param requesterName borrower display name */
    public void setRequesterName(String requesterName) { this.requesterName = requesterName; }

    /** @return stated purpose for borrowing */
    public String getPurpose() { return purpose; }

    /** @param purpose purpose */
    public void setPurpose(String purpose) { this.purpose = purpose; }

    /** @return expected return date/time */
    public LocalDateTime getExpectedReturnAt() { return expectedReturnAt; }

    /** @param expectedReturnAt expected return */
    public void setExpectedReturnAt(LocalDateTime expectedReturnAt) { this.expectedReturnAt = expectedReturnAt; }

    /** @return timestamp items were checked out */
    public LocalDateTime getCheckedOutAt() { return checkedOutAt; }

    /** @param checkedOutAt checkout timestamp */
    public void setCheckedOutAt(LocalDateTime checkedOutAt) { this.checkedOutAt = checkedOutAt; }

    /** @return timestamp items were returned */
    public LocalDateTime getActualReturnAt() { return actualReturnAt; }

    /** @param actualReturnAt return timestamp */
    public void setActualReturnAt(LocalDateTime actualReturnAt) { this.actualReturnAt = actualReturnAt; }

    /** @return ID of the manager who processed this request */
    public Long getProcessedById() { return processedById; }

    /** @param processedById processor ID */
    public void setProcessedById(Long processedById) { this.processedById = processedById; }

    /** @return processor display name */
    public String getProcessedByName() { return processedByName; }

    /** @param processedByName processor name */
    public void setProcessedByName(String processedByName) { this.processedByName = processedByName; }

    /** @return request status */
    public BorrowStatus getStatus() { return status; }

    /** @param status request status */
    public void setStatus(BorrowStatus status) { this.status = status; }

    /** @return optional decision note from manager */
    public String getDecisionNote() { return decisionNote; }

    /** @param decisionNote decision note */
    public void setDecisionNote(String decisionNote) { this.decisionNote = decisionNote; }

    /** @return individual equipment line items in this request */
    public List<BorrowItemDto> getItems() { return items; }

    /** @param items equipment line items */
    public void setItems(List<BorrowItemDto> items) { this.items = items; }

    /** @return creation timestamp */
    public LocalDateTime getCreatedAt() { return createdAt; }

    /** @param createdAt creation timestamp */
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    /** @return last update timestamp */
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    /** @param updatedAt update timestamp */
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    // ── Computed helpers ───────────────────────────────────────────────────

    /**
     * Returns true if this request is past its expected return date and not yet returned.
     * This is a client-side convenience; the authoritative overdue check is on the server.
     *
     * @return whether this request appears overdue
     */
    public boolean isOverdue() {
        return (status == BorrowStatus.ACTIVE || status == BorrowStatus.OVERDUE)
                && expectedReturnAt != null
                && LocalDateTime.now().isAfter(expectedReturnAt);
    }

    /**
     * Inner DTO for a single equipment line item within a borrow request.
     */
    public static class BorrowItemDto {

        private Long id;
        private Long equipmentId;
        private String equipmentName;
        private String assetCode;
        private EquipmentCondition conditionAtCheckout;
        private EquipmentCondition conditionAtReturn;
        private String notes;

        /** Required by Jackson. */
        public BorrowItemDto() { }

        /** @return item ID */
        public Long getId() { return id; }

        /** @param id item ID */
        public void setId(Long id) { this.id = id; }

        /** @return equipment ID */
        public Long getEquipmentId() { return equipmentId; }

        /** @param equipmentId equipment ID */
        public void setEquipmentId(Long equipmentId) { this.equipmentId = equipmentId; }

        /** @return equipment display name */
        public String getEquipmentName() { return equipmentName; }

        /** @param equipmentName equipment display name */
        public void setEquipmentName(String equipmentName) { this.equipmentName = equipmentName; }

        /** @return asset code */
        public String getAssetCode() { return assetCode; }

        /** @param assetCode asset code */
        public void setAssetCode(String assetCode) { this.assetCode = assetCode; }

        /** @return condition at time of checkout */
        public EquipmentCondition getConditionAtCheckout() { return conditionAtCheckout; }

        /** @param conditionAtCheckout checkout condition */
        public void setConditionAtCheckout(EquipmentCondition conditionAtCheckout) {
            this.conditionAtCheckout = conditionAtCheckout;
        }

        /** @return condition at time of return */
        public EquipmentCondition getConditionAtReturn() { return conditionAtReturn; }

        /** @param conditionAtReturn return condition */
        public void setConditionAtReturn(EquipmentCondition conditionAtReturn) {
            this.conditionAtReturn = conditionAtReturn;
        }

        /** @return notes recorded during return */
        public String getNotes() { return notes; }

        /** @param notes return notes */
        public void setNotes(String notes) { this.notes = notes; }
    }
}
