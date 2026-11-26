package com.labhub.common.enums;

/**
 * Priority level of a maintenance ticket.
 */
public enum MaintenancePriority {
    /** Non-urgent; can be addressed in scheduled maintenance cycles. */
    LOW,
    /** Should be addressed within a few days. */
    MEDIUM,
    /** Needs attention within 24 hours; equipment may be impaired. */
    HIGH,
    /** Immediate action required; safety risk or critical equipment down. */
    URGENT
}
