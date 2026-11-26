package com.labhub.common.dto;

/**
 * Data transfer object for dashboard operational summary figures.
 * All values are queried from the database; none are hard-coded.
 */
public class DashboardStatsDto {

    private int totalActiveLabs;
    private int totalEquipment;
    private int availableEquipment;
    private int borrowedEquipment;
    private int maintenanceEquipment;
    private int pendingBookings;
    private int pendingBorrowRequests;
    private int todayBookings;
    private int overdueItems;
    private int openMaintenanceTickets;

    /** Required by Jackson. */
    public DashboardStatsDto() { }

    // ── Getters / setters ──────────────────────────────────────────────────

    /** @return number of labs with ACTIVE status */
    public int getTotalActiveLabs() { return totalActiveLabs; }

    /** @param totalActiveLabs count */
    public void setTotalActiveLabs(int totalActiveLabs) { this.totalActiveLabs = totalActiveLabs; }

    /** @return total equipment count (active records) */
    public int getTotalEquipment() { return totalEquipment; }

    /** @param totalEquipment count */
    public void setTotalEquipment(int totalEquipment) { this.totalEquipment = totalEquipment; }

    /** @return equipment with AVAILABLE status */
    public int getAvailableEquipment() { return availableEquipment; }

    /** @param availableEquipment count */
    public void setAvailableEquipment(int availableEquipment) { this.availableEquipment = availableEquipment; }

    /** @return equipment currently BORROWED */
    public int getBorrowedEquipment() { return borrowedEquipment; }

    /** @param borrowedEquipment count */
    public void setBorrowedEquipment(int borrowedEquipment) { this.borrowedEquipment = borrowedEquipment; }

    /** @return equipment in MAINTENANCE status */
    public int getMaintenanceEquipment() { return maintenanceEquipment; }

    /** @param maintenanceEquipment count */
    public void setMaintenanceEquipment(int maintenanceEquipment) { this.maintenanceEquipment = maintenanceEquipment; }

    /** @return bookings awaiting decision */
    public int getPendingBookings() { return pendingBookings; }

    /** @param pendingBookings count */
    public void setPendingBookings(int pendingBookings) { this.pendingBookings = pendingBookings; }

    /** @return borrow requests awaiting decision */
    public int getPendingBorrowRequests() { return pendingBorrowRequests; }

    /** @param pendingBorrowRequests count */
    public void setPendingBorrowRequests(int pendingBorrowRequests) {
        this.pendingBorrowRequests = pendingBorrowRequests;
    }

    /** @return bookings with start time today */
    public int getTodayBookings() { return todayBookings; }

    /** @param todayBookings count */
    public void setTodayBookings(int todayBookings) { this.todayBookings = todayBookings; }

    /** @return active borrow requests past their due date */
    public int getOverdueItems() { return overdueItems; }

    /** @param overdueItems count */
    public void setOverdueItems(int overdueItems) { this.overdueItems = overdueItems; }

    /** @return maintenance tickets in OPEN, ASSIGNED, or IN_PROGRESS state */
    public int getOpenMaintenanceTickets() { return openMaintenanceTickets; }

    /** @param openMaintenanceTickets count */
    public void setOpenMaintenanceTickets(int openMaintenanceTickets) {
        this.openMaintenanceTickets = openMaintenanceTickets;
    }
}
