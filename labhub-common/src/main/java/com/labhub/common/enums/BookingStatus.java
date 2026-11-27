package com.labhub.common.enums;

/**
 * State machine for a room booking request.
 *
 * <p>Valid transitions:
 * <pre>
 *   PENDING  -> APPROVED | REJECTED | CANCELLED
 *   APPROVED -> CANCELLED | COMPLETED
 *   REJECTED -> (terminal)
 *   CANCELLED-> (terminal)
 *   COMPLETED-> (terminal)
 * </pre>
 */
public enum BookingStatus {
    /** Awaiting manager/admin decision. */
    PENDING,
    /** Booking has been approved; slot is reserved. */
    APPROVED,
    /** Booking was rejected by a manager or admin. */
    REJECTED,
    /** Cancelled by the requester or an authorised manager. */
    CANCELLED,
    /** Session has ended and the room was used. */
    COMPLETED
}
