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

    public BorrowRequestDto() { }

    public Long getId() { return id; }

    public void setId(Long id) { this.id = id; }

    public Long getRequesterId() { return requesterId; }

    public void setRequesterId(Long requesterId) { this.requesterId = requesterId; }

    public String getRequesterName() { return requesterName; }

    public void setRequesterName(String requesterName) { this.requesterName = requesterName; }

    public String getPurpose() { return purpose; }

    public void setPurpose(String purpose) { this.purpose = purpose; }

    public LocalDateTime getExpectedReturnAt() { return expectedReturnAt; }

    public void setExpectedReturnAt(LocalDateTime expectedReturnAt) { this.expectedReturnAt = expectedReturnAt; }

    public LocalDateTime getCheckedOutAt() { return checkedOutAt; }

    public void setCheckedOutAt(LocalDateTime checkedOutAt) { this.checkedOutAt = checkedOutAt; }

    public LocalDateTime getActualReturnAt() { return actualReturnAt; }

    public void setActualReturnAt(LocalDateTime actualReturnAt) { this.actualReturnAt = actualReturnAt; }

    public Long getProcessedById() { return processedById; }

    public void setProcessedById(Long processedById) { this.processedById = processedById; }

    public String getProcessedByName() { return processedByName; }

    public void setProcessedByName(String processedByName) { this.processedByName = processedByName; }

    public BorrowStatus getStatus() { return status; }

    public void setStatus(BorrowStatus status) { this.status = status; }

    public String getDecisionNote() { return decisionNote; }

    public void setDecisionNote(String decisionNote) { this.decisionNote = decisionNote; }

    public List<BorrowItemDto> getItems() { return items; }

    public void setItems(List<BorrowItemDto> items) { this.items = items; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

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

        public BorrowItemDto() { }

        public Long getId() { return id; }

        public void setId(Long id) { this.id = id; }

        public Long getEquipmentId() { return equipmentId; }

        public void setEquipmentId(Long equipmentId) { this.equipmentId = equipmentId; }

        public String getEquipmentName() { return equipmentName; }

        public void setEquipmentName(String equipmentName) { this.equipmentName = equipmentName; }

        public String getAssetCode() { return assetCode; }

        public void setAssetCode(String assetCode) { this.assetCode = assetCode; }

        public EquipmentCondition getConditionAtCheckout() { return conditionAtCheckout; }

        public void setConditionAtCheckout(EquipmentCondition conditionAtCheckout) {
            this.conditionAtCheckout = conditionAtCheckout;
        }

        public EquipmentCondition getConditionAtReturn() { return conditionAtReturn; }

        public void setConditionAtReturn(EquipmentCondition conditionAtReturn) {
            this.conditionAtReturn = conditionAtReturn;
        }

        public String getNotes() { return notes; }

        public void setNotes(String notes) { this.notes = notes; }
    }
}
