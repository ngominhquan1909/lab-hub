package com.labhub.common.dto;

import com.labhub.common.enums.EquipmentCondition;
import com.labhub.common.enums.EquipmentStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Data transfer object representing a piece of equipment in the inventory.
 */
public class EquipmentDto {

    private Long id;
    private String assetCode;
    private String name;
    private Long categoryId;
    private String categoryName;
    private String manufacturer;
    private String model;
    private String serialNumber;
    private Long labId;
    private String labName;
    private LocalDate acquisitionDate;
    private BigDecimal purchaseCost;
    private EquipmentCondition condition;
    private EquipmentStatus status;
    private String notes;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** Required by Jackson. */
    public EquipmentDto() { }

    // ── Getters / setters ──────────────────────────────────────────────────

    /** @return internal ID */
    public Long getId() { return id; }

    /** @param id internal ID */
    public void setId(Long id) { this.id = id; }

    /** @return unique asset code, e.g. "DELL-WS-001" */
    public String getAssetCode() { return assetCode; }

    /** @param assetCode unique asset code */
    public void setAssetCode(String assetCode) { this.assetCode = assetCode; }

    /** @return equipment name */
    public String getName() { return name; }

    /** @param name equipment name */
    public void setName(String name) { this.name = name; }

    /** @return category ID */
    public Long getCategoryId() { return categoryId; }

    /** @param categoryId category ID */
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }

    /** @return category display name (denormalized for UI convenience) */
    public String getCategoryName() { return categoryName; }

    /** @param categoryName category display name */
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    /** @return manufacturer name */
    public String getManufacturer() { return manufacturer; }

    /** @param manufacturer manufacturer name */
    public void setManufacturer(String manufacturer) { this.manufacturer = manufacturer; }

    /** @return model identifier */
    public String getModel() { return model; }

    /** @param model model identifier */
    public void setModel(String model) { this.model = model; }

    /** @return serial number (may be null if not tracked) */
    public String getSerialNumber() { return serialNumber; }

    /** @param serialNumber serial number */
    public void setSerialNumber(String serialNumber) { this.serialNumber = serialNumber; }

    /** @return ID of the lab this equipment is assigned to */
    public Long getLabId() { return labId; }

    /** @param labId lab ID */
    public void setLabId(Long labId) { this.labId = labId; }

    /** @return lab display name (denormalized for UI convenience) */
    public String getLabName() { return labName; }

    /** @param labName lab display name */
    public void setLabName(String labName) { this.labName = labName; }

    /** @return date equipment was acquired */
    public LocalDate getAcquisitionDate() { return acquisitionDate; }

    /** @param acquisitionDate acquisition date */
    public void setAcquisitionDate(LocalDate acquisitionDate) { this.acquisitionDate = acquisitionDate; }

    /** @return purchase cost (optional) */
    public BigDecimal getPurchaseCost() { return purchaseCost; }

    /** @param purchaseCost purchase cost */
    public void setPurchaseCost(BigDecimal purchaseCost) { this.purchaseCost = purchaseCost; }

    /** @return physical condition */
    public EquipmentCondition getCondition() { return condition; }

    /** @param condition physical condition */
    public void setCondition(EquipmentCondition condition) { this.condition = condition; }

    /** @return operational status */
    public EquipmentStatus getStatus() { return status; }

    /** @param status operational status */
    public void setStatus(EquipmentStatus status) { this.status = status; }

    /** @return additional notes */
    public String getNotes() { return notes; }

    /** @param notes additional notes */
    public void setNotes(String notes) { this.notes = notes; }

    /** @return whether this equipment record is active (not soft-deleted) */
    public boolean isActive() { return active; }

    /** @param active active flag */
    public void setActive(boolean active) { this.active = active; }

    /** @return creation timestamp */
    public LocalDateTime getCreatedAt() { return createdAt; }

    /** @param createdAt creation timestamp */
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    /** @return last update timestamp */
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    /** @param updatedAt last update timestamp */
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
