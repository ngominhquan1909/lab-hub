package com.labhub.server.domain;

import com.labhub.common.dto.UserDto;
import com.labhub.common.enums.UserRole;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Server-side domain model for a user account.
 * Contains the password hash which is NEVER sent to the client.
 * Use {@link #toDto()} to produce a client-safe representation.
 */
public class User {

    private Long id;
    private String username;
    private String email;
    private String fullName;
    private String phoneNumber;
    private String passwordHash;    // BCrypt; never serialized to the client
    private List<UserRole> roles;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastLoginAt;

    // ── Conversion ────────────────────────────────────────────────────────

    /**
     * Produces a client-safe DTO, excluding the password hash.
     *
     * @return UserDto with no sensitive fields
     */
    public UserDto toDto() {
        UserDto dto = new UserDto();
        dto.setId(id);
        dto.setUsername(username);
        dto.setEmail(email);
        dto.setFullName(fullName);
        dto.setPhoneNumber(phoneNumber);
        dto.setRoles(roles);
        dto.setActive(active);
        dto.setCreatedAt(createdAt);
        dto.setUpdatedAt(updatedAt);
        dto.setLastLoginAt(lastLoginAt);
        return dto;
    }

    // ── Getters / setters ──────────────────────────────────────────────────

    /** @return internal user ID */
    public Long getId() { return id; }

    /** @param id internal user ID */
    public void setId(Long id) { this.id = id; }

    /** @return login username */
    public String getUsername() { return username; }

    /** @param username login username */
    public void setUsername(String username) { this.username = username; }

    /** @return email address */
    public String getEmail() { return email; }

    /** @param email email address */
    public void setEmail(String email) { this.email = email; }

    /** @return display name */
    public String getFullName() { return fullName; }

    /** @param fullName display name */
    public void setFullName(String fullName) { this.fullName = fullName; }

    /** @return phone number */
    public String getPhoneNumber() { return phoneNumber; }

    /** @param phoneNumber phone number */
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    /** @return BCrypt password hash — never expose to client */
    public String getPasswordHash() { return passwordHash; }

    /** @param passwordHash BCrypt hash */
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    /** @return assigned roles */
    public List<UserRole> getRoles() { return roles; }

    /** @param roles assigned roles */
    public void setRoles(List<UserRole> roles) { this.roles = roles; }

    /** @return whether this account is enabled */
    public boolean isActive() { return active; }

    /** @param active enable/disable flag */
    public void setActive(boolean active) { this.active = active; }

    /** @return creation timestamp */
    public LocalDateTime getCreatedAt() { return createdAt; }

    /** @param createdAt creation timestamp */
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    /** @return last update timestamp */
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    /** @param updatedAt last update timestamp */
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    /** @return last successful login timestamp */
    public LocalDateTime getLastLoginAt() { return lastLoginAt; }

    /** @param lastLoginAt last login timestamp */
    public void setLastLoginAt(LocalDateTime lastLoginAt) { this.lastLoginAt = lastLoginAt; }
}
