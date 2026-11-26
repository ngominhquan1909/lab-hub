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

    /** Required by Jackson. */
    public LabDto() { }

    // ── Getters / setters ──────────────────────────────────────────────────

    /** @return internal ID */
    public Long getId() { return id; }

    /** @param id internal ID */
    public void setId(Long id) { this.id = id; }

    /** @return unique lab code, e.g. "CS-LAB-A" */
    public String getLabCode() { return labCode; }

    /** @param labCode unique lab code */
    public void setLabCode(String labCode) { this.labCode = labCode; }

    /** @return human-readable lab name */
    public String getName() { return name; }

    /** @param name human-readable lab name */
    public void setName(String name) { this.name = name; }

    /** @return building identifier */
    public String getBuilding() { return building; }

    /** @param building building identifier */
    public void setBuilding(String building) { this.building = building; }

    /** @return floor number */
    public Integer getFloor() { return floor; }

    /** @param floor floor number */
    public void setFloor(Integer floor) { this.floor = floor; }

    /** @return maximum attendee count */
    public Integer getCapacity() { return capacity; }

    /** @param capacity maximum attendee count */
    public void setCapacity(Integer capacity) { this.capacity = capacity; }

    /** @return room type, e.g. "Computer Lab", "Networking Lab" */
    public String getRoomType() { return roomType; }

    /** @param roomType room type */
    public void setRoomType(String roomType) { this.roomType = roomType; }

    /** @return optional description */
    public String getDescription() { return description; }

    /** @param description optional description */
    public void setDescription(String description) { this.description = description; }

    /** @return operational status */
    public LabStatus getStatus() { return status; }

    /** @param status operational status */
    public void setStatus(LabStatus status) { this.status = status; }

    /** @return number of equipment items assigned to this lab (read-only, computed by server) */
    public Integer getEquipmentCount() { return equipmentCount; }

    /** @param equipmentCount computed count */
    public void setEquipmentCount(Integer equipmentCount) { this.equipmentCount = equipmentCount; }

    /** @return creation timestamp */
    public LocalDateTime getCreatedAt() { return createdAt; }

    /** @param createdAt creation timestamp */
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    /** @return last update timestamp */
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    /** @param updatedAt last update timestamp */
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
