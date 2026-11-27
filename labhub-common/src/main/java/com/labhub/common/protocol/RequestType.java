package com.labhub.common.protocol;

/**
 * Exhaustive enumeration of all request types supported by the LabHub protocol.
 * Both client and server must agree on this set; unknown types return an error response.
 */
public enum RequestType {

    // Authentication
    LOGIN,
    LOGOUT,
    CHANGE_PASSWORD,

    // Users (admin only)
    USER_LIST,
    USER_GET,
    USER_CREATE,
    USER_UPDATE,
    USER_DISABLE,
    USER_ENABLE,

    // Laboratories
    LAB_LIST,
    LAB_GET,
    LAB_CREATE,
    LAB_UPDATE,
    LAB_DEACTIVATE,

    // Equipment categories
    CATEGORY_LIST,
    CATEGORY_CREATE,

    // Equipment
    EQUIPMENT_LIST,
    EQUIPMENT_GET,
    EQUIPMENT_CREATE,
    EQUIPMENT_UPDATE,
    EQUIPMENT_DEACTIVATE,
    EQUIPMENT_HISTORY,

    // Bookings
    BOOKING_LIST,
    BOOKING_GET,
    BOOKING_CREATE,
    BOOKING_APPROVE,
    BOOKING_REJECT,
    BOOKING_CANCEL,
    BOOKING_SCHEDULE,

    // Borrow requests
    BORROW_LIST,
    BORROW_GET,
    BORROW_CREATE,
    BORROW_APPROVE,
    BORROW_REJECT,
    BORROW_CHECKOUT,
    BORROW_RETURN,
    BORROW_CANCEL,

    // Maintenance
    MAINTENANCE_LIST,
    MAINTENANCE_GET,
    MAINTENANCE_CREATE,
    MAINTENANCE_UPDATE_STATUS,
    MAINTENANCE_ASSIGN,
    MAINTENANCE_RESOLVE,
    MAINTENANCE_CLOSE,

    // Dashboard
    DASHBOARD_STATS,
    DASHBOARD_ACTIVITY,
    DASHBOARD_TODAY_BOOKINGS,

    // Audit
    AUDIT_LIST
}
