package com.Edo_perfume.ScentOfASA.guide.dto;

import java.util.List;

import com.Edo_perfume.ScentOfASA.slot.dto.AdminSlotDayResponse;

public class GuideScheduleMonthResponse {

    private int year;
    private int month;
    private GuideProfileResponse guide;
    private List<AdminSlotDayResponse> days;

    public GuideScheduleMonthResponse() {
    }

    public GuideScheduleMonthResponse(int year, int month, GuideProfileResponse guide, List<AdminSlotDayResponse> days) {
        this.year = year;
        this.month = month;
        this.guide = guide;
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

    public GuideProfileResponse getGuide() {
        return guide;
    }

    public void setGuide(GuideProfileResponse guide) {
        this.guide = guide;
    }

    public List<AdminSlotDayResponse> getDays() {
        return days;
    }

    public void setDays(List<AdminSlotDayResponse> days) {
        this.days = days;
    }
}
