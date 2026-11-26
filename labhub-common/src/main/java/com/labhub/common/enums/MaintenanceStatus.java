package com.labhub.common.enums;

/**
 * Lifecycle states for a maintenance ticket.
 *
 * <p>Valid transitions:
 * <pre>
 *   OPEN       → ASSIGNED | IN_PROGRESS | RESOLVED
 *   ASSIGNED   → IN_PROGRESS | RESOLVED
 *   IN_PROGRESS→ RESOLVED
 *   RESOLVED   → CLOSED | IN_PROGRESS (re-opened if fix did not hold)
 *   CLOSED     → (terminal)
 * </pre>
 */
public enum MaintenanceStatus {
    /** Ticket filed; not yet assigned to anyone. */
    OPEN,
    /** Assigned to a technician; work not yet started. */
    ASSIGNED,
    /** Work is actively in progress. */
    IN_PROGRESS,
    /** Fix applied; awaiting verification. */
    RESOLVED,
    /** Verified fixed; ticket archived. */
    CLOSED
}
