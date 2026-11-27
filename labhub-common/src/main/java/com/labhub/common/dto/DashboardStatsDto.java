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

    public DashboardStatsDto() { }

    public int getTotalActiveLabs() { return totalActiveLabs; }

    public void setTotalActiveLabs(int totalActiveLabs) { this.totalActiveLabs = totalActiveLabs; }

    public int getTotalEquipment() { return totalEquipment; }

    public void setTotalEquipment(int totalEquipment) { this.totalEquipment = totalEquipment; }

    public int getAvailableEquipment() { return availableEquipment; }

    public void setAvailableEquipment(int availableEquipment) { this.availableEquipment = availableEquipment; }

    public int getBorrowedEquipment() { return borrowedEquipment; }

    public void setBorrowedEquipment(int borrowedEquipment) { this.borrowedEquipment = borrowedEquipment; }

    public int getMaintenanceEquipment() { return maintenanceEquipment; }

    public void setMaintenanceEquipment(int maintenanceEquipment) { this.maintenanceEquipment = maintenanceEquipment; }

    public int getPendingBookings() { return pendingBookings; }

    public void setPendingBookings(int pendingBookings) { this.pendingBookings = pendingBookings; }

    public int getPendingBorrowRequests() { return pendingBorrowRequests; }

    public void setPendingBorrowRequests(int pendingBorrowRequests) {
        this.pendingBorrowRequests = pendingBorrowRequests;
    }

    public int getTodayBookings() { return todayBookings; }

    public void setTodayBookings(int todayBookings) { this.todayBookings = todayBookings; }

    public int getOverdueItems() { return overdueItems; }

    public void setOverdueItems(int overdueItems) { this.overdueItems = overdueItems; }

    public int getOpenMaintenanceTickets() { return openMaintenanceTickets; }

    public void setOpenMaintenanceTickets(int openMaintenanceTickets) {
        this.openMaintenanceTickets = openMaintenanceTickets;
    }
}
