package com.Edo_perfume.ScentOfASA.slot.dto;

import java.time.LocalDate;
import java.util.List;

public class AdminSlotDayResponse {

    private LocalDate date;
    private boolean closed;
    private String holidayType;
    private String holidayReason;
    private List<AdminSlotResponse> slots;

    public AdminSlotDayResponse() {
    }

    public AdminSlotDayResponse(LocalDate date, boolean closed, String holidayType, String holidayReason,
                                List<AdminSlotResponse> slots) {
        this.date = date;
        this.closed = closed;
        this.holidayType = holidayType;
        this.holidayReason = holidayReason;
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

    public String getHolidayType() {
        return holidayType;
    }

    public void setHolidayType(String holidayType) {
        this.holidayType = holidayType;
    }

    public String getHolidayReason() {
        return holidayReason;
    }

    public void setHolidayReason(String holidayReason) {
        this.holidayReason = holidayReason;
    }

    public List<AdminSlotResponse> getSlots() {
        return slots;
    }

    public void setSlots(List<AdminSlotResponse> slots) {
        this.slots = slots;
    }
}
