package com.labhub.common.dto;

/**
 * Data transfer object representing an equipment category.
 */
public class CategoryDto {

    private Long id;
    private String name;
    private String description;
    private int equipmentCount;

    public CategoryDto() { }

    public Long getId() { return id; }

    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }

    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }

    public void setDescription(String description) { this.description = description; }

    public int getEquipmentCount() { return equipmentCount; }

    public void setEquipmentCount(int equipmentCount) { this.equipmentCount = equipmentCount; }
}
