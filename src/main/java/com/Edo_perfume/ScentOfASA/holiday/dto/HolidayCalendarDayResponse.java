package com.Edo_perfume.ScentOfASA.holiday.dto;

import java.time.LocalDate;

public class HolidayCalendarDayResponse {

    private Long id;
    private LocalDate holidayDate;
    private String holidayType;
    private String reason;
    private String appliesToLanguage;

    public HolidayCalendarDayResponse() {
    }

    public HolidayCalendarDayResponse(Long id, LocalDate holidayDate, String holidayType, String reason, String appliesToLanguage) {
        this.id = id;
        this.holidayDate = holidayDate;
        this.holidayType = holidayType;
        this.reason = reason;
        this.appliesToLanguage = appliesToLanguage;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getHolidayDate() {
        return holidayDate;
    }

    public void setHolidayDate(LocalDate holidayDate) {
        this.holidayDate = holidayDate;
    }

    public String getHolidayType() {
        return holidayType;
    }

    public void setHolidayType(String holidayType) {
        this.holidayType = holidayType;
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
}
