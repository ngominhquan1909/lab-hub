package com.labhub.common.enums;

/**
 * Roles available in the LabHub system.
 * Authorization is enforced server-side; client uses roles to hide/show UI elements only.
 */
public enum UserRole {
    /** Full access — manages accounts, roles, system configuration, all records. */
    ADMIN,
    /** Manages labs, equipment, approvals, maintenance workflows, and operational records. */
    LAB_MANAGER,
    /** Views relevant labs/equipment, creates bookings and loan requests. */
    LECTURER,
    /** Views eligible lab/equipment availability, creates permitted booking/loan requests. */
    STUDENT
}
