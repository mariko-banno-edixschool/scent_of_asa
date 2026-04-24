package com.Edo_perfume.ScentOfASA.slot.dto;

import java.util.List;

public class AdminSlotMonthResponse {

    private int year;
    private int month;
    private List<AdminSlotDayResponse> days;

    public AdminSlotMonthResponse() {
    }

    public AdminSlotMonthResponse(int year, int month, List<AdminSlotDayResponse> days) {
        this.year = year;
        this.month = month;
        this.days = days;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public int getMonth() {
        return month;
    }

    public void setMonth(int month) {
        this.month = month;
    }

    public List<AdminSlotDayResponse> getDays() {
        return days;
    }

    public void setDays(List<AdminSlotDayResponse> days) {
        this.days = days;
    }
}
