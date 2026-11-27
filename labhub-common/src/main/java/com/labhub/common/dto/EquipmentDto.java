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

    public EquipmentDto() { }

    public Long getId() { return id; }

    public void setId(Long id) { this.id = id; }

    public String getAssetCode() { return assetCode; }

    public void setAssetCode(String assetCode) { this.assetCode = assetCode; }

    public String getName() { return name; }

    public void setName(String name) { this.name = name; }

    public Long getCategoryId() { return categoryId; }

    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }

    public String getCategoryName() { return categoryName; }

    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    public String getManufacturer() { return manufacturer; }

    public void setManufacturer(String manufacturer) { this.manufacturer = manufacturer; }

    public String getModel() { return model; }

    public void setModel(String model) { this.model = model; }

    public String getSerialNumber() { return serialNumber; }

    public void setSerialNumber(String serialNumber) { this.serialNumber = serialNumber; }

    public Long getLabId() { return labId; }

    public void setLabId(Long labId) { this.labId = labId; }

    public String getLabName() { return labName; }

    public void setLabName(String labName) { this.labName = labName; }

    public LocalDate getAcquisitionDate() { return acquisitionDate; }

    public void setAcquisitionDate(LocalDate acquisitionDate) { this.acquisitionDate = acquisitionDate; }

    public BigDecimal getPurchaseCost() { return purchaseCost; }

    public void setPurchaseCost(BigDecimal purchaseCost) { this.purchaseCost = purchaseCost; }

    public EquipmentCondition getCondition() { return condition; }

    public void setCondition(EquipmentCondition condition) { this.condition = condition; }

    public EquipmentStatus getStatus() { return status; }

    public void setStatus(EquipmentStatus status) { this.status = status; }

    public String getNotes() { return notes; }

    public void setNotes(String notes) { this.notes = notes; }

    public boolean isActive() { return active; }

    public void setActive(boolean active) { this.active = active; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
