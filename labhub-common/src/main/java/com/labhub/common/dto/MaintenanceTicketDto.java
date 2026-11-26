package com.labhub.common.dto;

import com.labhub.common.enums.MaintenancePriority;
import com.labhub.common.enums.MaintenanceStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Data transfer object representing a maintenance ticket.
 */
public class MaintenanceTicketDto {

    private Long id;
    private Long equipmentId;
    private String equipmentName;
    private String assetCode;
    private Long reporterId;
    private String reporterName;
    private String description;
    private MaintenancePriority priority;
    private Long assigneeId;
    private String assigneeName;
    private MaintenanceStatus status;
    private LocalDateTime openedAt;
    private LocalDateTime updatedAt;
    private LocalDateTime resolvedAt;
    private String resolutionNotes;
    private BigDecimal repairCost;

    /** Required by Jackson. */
    public MaintenanceTicketDto() { }

    // ── Getters / setters ──────────────────────────────────────────────────

    /** @return internal ticket ID */
    public Long getId() { return id; }

    /** @param id internal ID */
    public void setId(Long id) { this.id = id; }

    /** @return equipment this ticket concerns */
    public Long getEquipmentId() { return equipmentId; }

    /** @param equipmentId equipment ID */
    public void setEquipmentId(Long equipmentId) { this.equipmentId = equipmentId; }

    /** @return equipment display name */
    public String getEquipmentName() { return equipmentName; }

    /** @param equipmentName equipment name */
    public void setEquipmentName(String equipmentName) { this.equipmentName = equipmentName; }

    /** @return asset code of the affected equipment */
    public String getAssetCode() { return assetCode; }

    /** @param assetCode asset code */
    public void setAssetCode(String assetCode) { this.assetCode = assetCode; }

    /** @return user who filed this ticket */
    public Long getReporterId() { return reporterId; }

    /** @param reporterId reporter ID */
    public void setReporterId(Long reporterId) { this.reporterId = reporterId; }

    /** @return reporter display name */
    public String getReporterName() { return reporterName; }

    /** @param reporterName reporter name */
    public void setReporterName(String reporterName) { this.reporterName = reporterName; }

    /** @return description of the fault or issue */
    public String getDescription() { return description; }

    /** @param description issue description */
    public void setDescription(String description) { this.description = description; }

    /** @return priority level */
    public MaintenancePriority getPriority() { return priority; }

    /** @param priority priority */
    public void setPriority(MaintenancePriority priority) { this.priority = priority; }

    /** @return ID of assigned technician or manager */
    public Long getAssigneeId() { return assigneeId; }

    /** @param assigneeId assignee ID */
    public void setAssigneeId(Long assigneeId) { this.assigneeId = assigneeId; }

    /** @return assignee display name */
    public String getAssigneeName() { return assigneeName; }

    /** @param assigneeName assignee display name */
    public void setAssigneeName(String assigneeName) { this.assigneeName = assigneeName; }

    /** @return current ticket status */
    public MaintenanceStatus getStatus() { return status; }

    /** @param status status */
    public void setStatus(MaintenanceStatus status) { this.status = status; }

    /** @return timestamp ticket was opened */
    public LocalDateTime getOpenedAt() { return openedAt; }

    /** @param openedAt opened timestamp */
    public void setOpenedAt(LocalDateTime openedAt) { this.openedAt = openedAt; }

    /** @return last status change timestamp */
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    /** @param updatedAt update timestamp */
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    /** @return timestamp issue was resolved */
    public LocalDateTime getResolvedAt() { return resolvedAt; }

    /** @param resolvedAt resolution timestamp */
    public void setResolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; }

    /** @return notes describing how the issue was resolved */
    public String getResolutionNotes() { return resolutionNotes; }

    /** @param resolutionNotes resolution notes */
    public void setResolutionNotes(String resolutionNotes) { this.resolutionNotes = resolutionNotes; }

    /** @return cost of repair if applicable */
    public BigDecimal getRepairCost() { return repairCost; }

    /** @param repairCost repair cost */
    public void setRepairCost(BigDecimal repairCost) { this.repairCost = repairCost; }
}
