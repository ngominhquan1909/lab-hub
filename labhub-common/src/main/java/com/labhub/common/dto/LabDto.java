package com.labhub.common.dto;

import com.labhub.common.enums.LabStatus;

import java.time.LocalDateTime;

/**
 * Data transfer object representing a laboratory room.
 */
public class LabDto {

    private Long id;
    private String labCode;
    private String name;
    private String building;
    private Integer floor;
    private Integer capacity;
    private String roomType;
    private String description;
    private LabStatus status;
    private Integer equipmentCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public LabDto() { }

    public Long getId() { return id; }

    public void setId(Long id) { this.id = id; }

    public String getLabCode() { return labCode; }

    public void setLabCode(String labCode) { this.labCode = labCode; }

    public String getName() { return name; }

    public void setName(String name) { this.name = name; }

    public String getBuilding() { return building; }

    public void setBuilding(String building) { this.building = building; }

    public Integer getFloor() { return floor; }

    public void setFloor(Integer floor) { this.floor = floor; }

    public Integer getCapacity() { return capacity; }

    public void setCapacity(Integer capacity) { this.capacity = capacity; }

    public String getRoomType() { return roomType; }

    public void setRoomType(String roomType) { this.roomType = roomType; }

    public String getDescription() { return description; }

    public void setDescription(String description) { this.description = description; }

    public LabStatus getStatus() { return status; }

    public void setStatus(LabStatus status) { this.status = status; }

    public Integer getEquipmentCount() { return equipmentCount; }

    public void setEquipmentCount(Integer equipmentCount) { this.equipmentCount = equipmentCount; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
