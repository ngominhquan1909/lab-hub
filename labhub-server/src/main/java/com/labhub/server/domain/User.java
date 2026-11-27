package com.labhub.server.domain;

import com.labhub.common.dto.UserDto;
import com.labhub.common.enums.UserRole;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Server-side domain model for a user account.
 * Contains the password hash which is not sent to the client.
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

    public Long getId() { return id; }

    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }

    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }

    public void setEmail(String email) { this.email = email; }

    public String getFullName() { return fullName; }

    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getPhoneNumber() { return phoneNumber; }

    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getPasswordHash() { return passwordHash; }

    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public List<UserRole> getRoles() { return roles; }

    public void setRoles(List<UserRole> roles) { this.roles = roles; }

    public boolean isActive() { return active; }

    public void setActive(boolean active) { this.active = active; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public LocalDateTime getLastLoginAt() { return lastLoginAt; }

    public void setLastLoginAt(LocalDateTime lastLoginAt) { this.lastLoginAt = lastLoginAt; }
}
