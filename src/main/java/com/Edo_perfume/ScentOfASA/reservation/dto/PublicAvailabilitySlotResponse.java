package com.Edo_perfume.ScentOfASA.reservation.dto;

public class PublicAvailabilitySlotResponse {

    private String timeSlot;
    private String status;
    private boolean available;
    private int remainingCapacity;
    private int reservedGuestCount;

    public PublicAvailabilitySlotResponse() {
    }

    public PublicAvailabilitySlotResponse(String timeSlot, String status, boolean available) {
        this(timeSlot, status, available, 0, 0);
    }

    public PublicAvailabilitySlotResponse(String timeSlot, String status, boolean available,
                                          int remainingCapacity, int reservedGuestCount) {
        this.timeSlot = timeSlot;
        this.status = status;
        this.available = available;
        this.remainingCapacity = remainingCapacity;
        this.reservedGuestCount = reservedGuestCount;
    }

    public String getTimeSlot() {
        return timeSlot;
    }

    public void setTimeSlot(String timeSlot) {
        this.timeSlot = timeSlot;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public int getRemainingCapacity() {
        return remainingCapacity;
    }

    public void setRemainingCapacity(int remainingCapacity) {
        this.remainingCapacity = remainingCapacity;
    }

    public int getReservedGuestCount() {
        return reservedGuestCount;
    }

    public void setReservedGuestCount(int reservedGuestCount) {
        this.reservedGuestCount = reservedGuestCount;
    }
}
