package com.labhub.common.dto;

/**
 * Data transfer object representing an equipment category.
 */
public class CategoryDto {

    private Long id;
    private String name;
    private String description;
    private int equipmentCount;

    /** Required by Jackson. */
    public CategoryDto() { }

    /** @return internal category ID */
    public Long getId() { return id; }

    /** @param id internal ID */
    public void setId(Long id) { this.id = id; }

    /** @return category name */
    public String getName() { return name; }

    /** @param name category name */
    public void setName(String name) { this.name = name; }

    /** @return optional description */
    public String getDescription() { return description; }

    /** @param description category description */
    public void setDescription(String description) { this.description = description; }

    /** @return number of equipment items in this category */
    public int getEquipmentCount() { return equipmentCount; }

    /** @param equipmentCount count */
    public void setEquipmentCount(int equipmentCount) { this.equipmentCount = equipmentCount; }
}
