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

    public MaintenanceTicketDto() { }

    public Long getId() { return id; }

    public void setId(Long id) { this.id = id; }

    public Long getEquipmentId() { return equipmentId; }

    public void setEquipmentId(Long equipmentId) { this.equipmentId = equipmentId; }

    public String getEquipmentName() { return equipmentName; }

    public void setEquipmentName(String equipmentName) { this.equipmentName = equipmentName; }

    public String getAssetCode() { return assetCode; }

    public void setAssetCode(String assetCode) { this.assetCode = assetCode; }

    public Long getReporterId() { return reporterId; }

    public void setReporterId(Long reporterId) { this.reporterId = reporterId; }

    public String getReporterName() { return reporterName; }

    public void setReporterName(String reporterName) { this.reporterName = reporterName; }

    public String getDescription() { return description; }

    public void setDescription(String description) { this.description = description; }

    public MaintenancePriority getPriority() { return priority; }

    public void setPriority(MaintenancePriority priority) { this.priority = priority; }

    public Long getAssigneeId() { return assigneeId; }

    public void setAssigneeId(Long assigneeId) { this.assigneeId = assigneeId; }

    public String getAssigneeName() { return assigneeName; }

    public void setAssigneeName(String assigneeName) { this.assigneeName = assigneeName; }

    public MaintenanceStatus getStatus() { return status; }

    public void setStatus(MaintenanceStatus status) { this.status = status; }

    public LocalDateTime getOpenedAt() { return openedAt; }

    public void setOpenedAt(LocalDateTime openedAt) { this.openedAt = openedAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public LocalDateTime getResolvedAt() { return resolvedAt; }

    public void setResolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; }

    public String getResolutionNotes() { return resolutionNotes; }

    public void setResolutionNotes(String resolutionNotes) { this.resolutionNotes = resolutionNotes; }

    public BigDecimal getRepairCost() { return repairCost; }

    public void setRepairCost(BigDecimal repairCost) { this.repairCost = repairCost; }
}
