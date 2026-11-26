package com.labhub.common.enums;

/**
 * Operational status of a laboratory room.
 */
public enum LabStatus {
    /** Lab is in normal operation and can be booked. */
    ACTIVE,
    /** Lab is temporarily unavailable (renovation, maintenance, etc.). */
    INACTIVE,
    /** Lab is permanently decommissioned. */
    DECOMMISSIONED
}
