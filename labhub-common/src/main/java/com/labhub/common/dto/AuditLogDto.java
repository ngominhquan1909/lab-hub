package com.labhub.common.dto;

import java.time.LocalDateTime;

/**
 * Data transfer object representing a single audit log entry.
 * Never contains passwords, tokens, or sensitive secrets.
 */
public class AuditLogDto {

    private Long id;
    private Long actorId;
    private String actorName;
    private String action;
    private String targetType;
    private Long targetId;
    private String details;
    private LocalDateTime timestamp;

    /** Required by Jackson. */
    public AuditLogDto() { }

    // ── Getters / setters ──────────────────────────────────────────────────

    /** @return internal log entry ID */
    public Long getId() { return id; }

    /** @param id internal ID */
    public void setId(Long id) { this.id = id; }

    /** @return ID of the user who performed the action */
    public Long getActorId() { return actorId; }

    /** @param actorId actor ID */
    public void setActorId(Long actorId) { this.actorId = actorId; }

    /** @return actor display name */
    public String getActorName() { return actorName; }

    /** @param actorName actor name */
    public void setActorName(String actorName) { this.actorName = actorName; }

    /** @return action name, e.g. "BOOKING_APPROVED", "EQUIPMENT_CREATED" */
    public String getAction() { return action; }

    /** @param action action name */
    public void setAction(String action) { this.action = action; }

    /** @return type of the target entity, e.g. "BOOKING", "EQUIPMENT", "USER" */
    public String getTargetType() { return targetType; }

    /** @param targetType target entity type */
    public void setTargetType(String targetType) { this.targetType = targetType; }

    /** @return ID of the target entity */
    public Long getTargetId() { return targetId; }

    /** @param targetId target entity ID */
    public void setTargetId(Long targetId) { this.targetId = targetId; }

    /** @return concise details about what changed; never contains secrets */
    public String getDetails() { return details; }

    /** @param details change details */
    public void setDetails(String details) { this.details = details; }

    /** @return when this action occurred (UTC) */
    public LocalDateTime getTimestamp() { return timestamp; }

    /** @param timestamp action timestamp */
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
