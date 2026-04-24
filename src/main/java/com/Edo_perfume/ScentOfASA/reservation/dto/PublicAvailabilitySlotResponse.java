package com.Edo_perfume.ScentOfASA.reservation.dto;

public class PublicAvailabilitySlotResponse {

    private String timeSlot;
    private String status;
    private boolean available;

    public PublicAvailabilitySlotResponse() {
    }

    public PublicAvailabilitySlotResponse(String timeSlot, String status, boolean available) {
        this.timeSlot = timeSlot;
        this.status = status;
        this.available = available;
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
}
