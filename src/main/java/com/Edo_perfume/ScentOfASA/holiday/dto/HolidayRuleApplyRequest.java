package com.Edo_perfume.ScentOfASA.holiday.dto;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

public class HolidayRuleApplyRequest {

    private Integer year;
    private Integer month;
    private DayOfWeek weeklyClosedDay;
    private String reason;
    private String appliesToLanguage;
    private Long createdByStaffId;
    private List<LocalDate> openExceptionDates;

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public Integer getMonth() {
        return month;
    }

    public void setMonth(Integer month) {
        this.month = month;
    }

    public DayOfWeek getWeeklyClosedDay() {
        return weeklyClosedDay;
    }

    public void setWeeklyClosedDay(DayOfWeek weeklyClosedDay) {
        this.weeklyClosedDay = weeklyClosedDay;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getAppliesToLanguage() {
        return appliesToLanguage;
    }

    public void setAppliesToLanguage(String appliesToLanguage) {
        this.appliesToLanguage = appliesToLanguage;
    }

    public Long getCreatedByStaffId() {
        return createdByStaffId;
    }

    public void setCreatedByStaffId(Long createdByStaffId) {
        this.createdByStaffId = createdByStaffId;
    }

    public List<LocalDate> getOpenExceptionDates() {
        return openExceptionDates;
    }

    public void setOpenExceptionDates(List<LocalDate> openExceptionDates) {
        this.openExceptionDates = openExceptionDates;
    }
}
