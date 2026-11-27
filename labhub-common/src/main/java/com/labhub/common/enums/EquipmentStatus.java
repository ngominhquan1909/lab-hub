package com.labhub.common.enums;

/**
 * Lifecycle states for a piece of equipment.
 *
 * <p>Valid transitions:
 * <pre>
 *   AVAILABLE  -> IN_USE | BORROWED | MAINTENANCE | RETIRED
 *   IN_USE     -> AVAILABLE | MAINTENANCE
 *   BORROWED   -> AVAILABLE | MAINTENANCE
 *   MAINTENANCE-> AVAILABLE | RETIRED
 *   LOST       -> (terminal, admin override only)
 *   RETIRED    -> (terminal)
 * </pre>
 */
public enum EquipmentStatus {
    /** Ready to be borrowed or used in-lab. */
    AVAILABLE,
    /** Currently in active in-lab use (not via a formal borrow request). */
    IN_USE,
    /** Checked out via an approved borrow request. */
    BORROWED,
    /** Undergoing maintenance; must not be borrowed or reserved. */
    MAINTENANCE,
    /** Cannot be located; set by admin after investigation. */
    LOST,
    /** Permanently decommissioned. */
    RETIRED
}
