package com.labhub.common.dto;

import com.labhub.common.enums.BookingStatus;

import java.time.LocalDateTime;

/**
 * Data transfer object representing a room booking request.
 */
public class BookingDto {

    private Long id;
    private Long labId;
    private String labName;
    private String labCode;
    private Long requesterId;
    private String requesterName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String purpose;
    private Integer attendeeCount;
    private String notes;
    private BookingStatus status;
    private Long decidedById;
    private String decidedByName;
    private LocalDateTime decidedAt;
    private String decisionNote;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** Required by Jackson. */
    public BookingDto() { }

    // ── Getters / setters ──────────────────────────────────────────────────

    /** @return internal booking ID */
    public Long getId() { return id; }

    /** @param id internal ID */
    public void setId(Long id) { this.id = id; }

    /** @return ID of the booked lab */
    public Long getLabId() { return labId; }

    /** @param labId lab ID */
    public void setLabId(Long labId) { this.labId = labId; }

    /** @return lab display name (denormalized) */
    public String getLabName() { return labName; }

    /** @param labName lab display name */
    public void setLabName(String labName) { this.labName = labName; }

    /** @return lab code (denormalized) */
    public String getLabCode() { return labCode; }

    /** @param labCode lab code */
    public void setLabCode(String labCode) { this.labCode = labCode; }

    /** @return ID of the user who made this booking */
    public Long getRequesterId() { return requesterId; }

    /** @param requesterId requester ID */
    public void setRequesterId(Long requesterId) { this.requesterId = requesterId; }

    /** @return requester display name (denormalized) */
    public String getRequesterName() { return requesterName; }

    /** @param requesterName requester display name */
    public void setRequesterName(String requesterName) { this.requesterName = requesterName; }

    /** @return session start time (UTC) */
    public LocalDateTime getStartTime() { return startTime; }

    /** @param startTime start time */
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    /** @return session end time (UTC) */
    public LocalDateTime getEndTime() { return endTime; }

    /** @param endTime end time */
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

    /** @return stated purpose of the session */
    public String getPurpose() { return purpose; }

    /** @param purpose session purpose */
    public void setPurpose(String purpose) { this.purpose = purpose; }

    /** @return number of expected attendees */
    public Integer getAttendeeCount() { return attendeeCount; }

    /** @param attendeeCount attendee count */
    public void setAttendeeCount(Integer attendeeCount) { this.attendeeCount = attendeeCount; }

    /** @return optional additional notes */
    public String getNotes() { return notes; }

    /** @param notes additional notes */
    public void setNotes(String notes) { this.notes = notes; }

    /** @return booking status */
    public BookingStatus getStatus() { return status; }

    /** @param status booking status */
    public void setStatus(BookingStatus status) { this.status = status; }

    /** @return ID of the manager/admin who approved or rejected */
    public Long getDecidedById() { return decidedById; }

    /** @param decidedById decision maker ID */
    public void setDecidedById(Long decidedById) { this.decidedById = decidedById; }

    /** @return display name of the decision maker */
    public String getDecidedByName() { return decidedByName; }

    /** @param decidedByName decision maker name */
    public void setDecidedByName(String decidedByName) { this.decidedByName = decidedByName; }

    /** @return timestamp of approval/rejection decision */
    public LocalDateTime getDecidedAt() { return decidedAt; }

    /** @param decidedAt decision timestamp */
    public void setDecidedAt(LocalDateTime decidedAt) { this.decidedAt = decidedAt; }

    /** @return optional note attached to the approval/rejection */
    public String getDecisionNote() { return decisionNote; }

    /** @param decisionNote decision note */
    public void setDecisionNote(String decisionNote) { this.decisionNote = decisionNote; }

    /** @return creation timestamp */
    public LocalDateTime getCreatedAt() { return createdAt; }

    /** @param createdAt creation timestamp */
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    /** @return last update timestamp */
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    /** @param updatedAt update timestamp */
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
