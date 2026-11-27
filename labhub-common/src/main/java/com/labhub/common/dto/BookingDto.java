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

    public BookingDto() { }

    public Long getId() { return id; }

    public void setId(Long id) { this.id = id; }

    public Long getLabId() { return labId; }

    public void setLabId(Long labId) { this.labId = labId; }

    public String getLabName() { return labName; }

    public void setLabName(String labName) { this.labName = labName; }

    public String getLabCode() { return labCode; }

    public void setLabCode(String labCode) { this.labCode = labCode; }

    public Long getRequesterId() { return requesterId; }

    public void setRequesterId(Long requesterId) { this.requesterId = requesterId; }

    public String getRequesterName() { return requesterName; }

    public void setRequesterName(String requesterName) { this.requesterName = requesterName; }

    public LocalDateTime getStartTime() { return startTime; }

    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public LocalDateTime getEndTime() { return endTime; }

    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

    public String getPurpose() { return purpose; }

    public void setPurpose(String purpose) { this.purpose = purpose; }

    public Integer getAttendeeCount() { return attendeeCount; }

    public void setAttendeeCount(Integer attendeeCount) { this.attendeeCount = attendeeCount; }

    public String getNotes() { return notes; }

    public void setNotes(String notes) { this.notes = notes; }

    public BookingStatus getStatus() { return status; }

    public void setStatus(BookingStatus status) { this.status = status; }

    public Long getDecidedById() { return decidedById; }

    public void setDecidedById(Long decidedById) { this.decidedById = decidedById; }

    public String getDecidedByName() { return decidedByName; }

    public void setDecidedByName(String decidedByName) { this.decidedByName = decidedByName; }

    public LocalDateTime getDecidedAt() { return decidedAt; }

    public void setDecidedAt(LocalDateTime decidedAt) { this.decidedAt = decidedAt; }

    public String getDecisionNote() { return decisionNote; }

    public void setDecisionNote(String decisionNote) { this.decisionNote = decisionNote; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
