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

    public AuditLogDto() { }

    public Long getId() { return id; }

    public void setId(Long id) { this.id = id; }

    public Long getActorId() { return actorId; }

    public void setActorId(Long actorId) { this.actorId = actorId; }

    public String getActorName() { return actorName; }

    public void setActorName(String actorName) { this.actorName = actorName; }

    public String getAction() { return action; }

    public void setAction(String action) { this.action = action; }

    public String getTargetType() { return targetType; }

    public void setTargetType(String targetType) { this.targetType = targetType; }

    public Long getTargetId() { return targetId; }

    public void setTargetId(Long targetId) { this.targetId = targetId; }

    public String getDetails() { return details; }

    public void setDetails(String details) { this.details = details; }

    public LocalDateTime getTimestamp() { return timestamp; }

    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
