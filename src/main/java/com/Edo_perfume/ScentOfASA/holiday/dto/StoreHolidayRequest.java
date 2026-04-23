package com.Edo_perfume.ScentOfASA.holiday.dto;

import java.time.LocalDate;

import com.Edo_perfume.ScentOfASA.holiday.domain.HolidayType;

public class StoreHolidayRequest {

    private LocalDate holidayDate;
    private HolidayType holidayType;
    private String reason;
    private String appliesToLanguage;
    private Long createdByStaffId;

    public LocalDate getHolidayDate() {
        return holidayDate;
    }

    public void setHolidayDate(LocalDate holidayDate) {
        this.holidayDate = holidayDate;
    }

    public HolidayType getHolidayType() {
        return holidayType;
    }

    public void setHolidayType(HolidayType holidayType) {
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

    public Long getCreatedByStaffId() {
        return createdByStaffId;
    }

    public void setCreatedByStaffId(Long createdByStaffId) {
        this.createdByStaffId = createdByStaffId;
    }
}
