package com.Edo_perfume.ScentOfASA.reservation.dto;

import java.time.LocalDate;
import java.util.List;

public class PublicAvailabilityDayResponse {

    private LocalDate date;
    private boolean closed;
    private String reason;
    private List<PublicAvailabilitySlotResponse> slots;

    public PublicAvailabilityDayResponse() {
    }

    public PublicAvailabilityDayResponse(LocalDate date, boolean closed, String reason,
                                         List<PublicAvailabilitySlotResponse> slots) {
        this.date = date;
        this.closed = closed;
        this.reason = reason;
        this.slots = slots;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public boolean isClosed() {
        return closed;
    }

    public void setClosed(boolean closed) {
        this.closed = closed;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public List<PublicAvailabilitySlotResponse> getSlots() {
        return slots;
    }

    public void setSlots(List<PublicAvailabilitySlotResponse> slots) {
        this.slots = slots;
    }
}
