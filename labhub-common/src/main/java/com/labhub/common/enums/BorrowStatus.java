package com.labhub.common.enums;

/**
 * State machine for an equipment borrow request.
 *
 * <p>Valid transitions:
 * <pre>
 *   PENDING   -> APPROVED | REJECTED | CANCELLED
 *   APPROVED  -> ACTIVE (checkout recorded) | CANCELLED
 *   ACTIVE    -> RETURNED | OVERDUE (computed when past due date)
 *   OVERDUE   -> RETURNED
 *   REJECTED  -> (terminal)
 *   CANCELLED -> (terminal)
 *   RETURNED  -> (terminal)
 * </pre>
 *
 * <p>OVERDUE is a computed overlay: items can still be returned from OVERDUE state.
 */
public enum BorrowStatus {
    /** Awaiting manager/admin decision. */
    PENDING,
    /** Approved; equipment not yet collected by borrower. */
    APPROVED,
    /** Rejected by a manager or admin. */
    REJECTED,
    /** Equipment checked out; borrower has the items. */
    ACTIVE,
    /** All items have been returned and processed. */
    RETURNED,
    /** Cancelled before checkout. */
    CANCELLED,
    /** Past due date and not yet returned (computed state). */
    OVERDUE
}
