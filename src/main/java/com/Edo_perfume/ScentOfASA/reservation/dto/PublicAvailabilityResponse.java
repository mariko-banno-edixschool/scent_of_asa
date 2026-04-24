package com.Edo_perfume.ScentOfASA.reservation.dto;

import java.util.List;

public class PublicAvailabilityResponse {

    private int year;
    private int month;
    private String language;
    private List<PublicAvailabilityDayResponse> days;

    public PublicAvailabilityResponse() {
    }

    public PublicAvailabilityResponse(int year, int month, String language, List<PublicAvailabilityDayResponse> days) {
        this.year = year;
        this.month = month;
        this.language = language;
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

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public List<PublicAvailabilityDayResponse> getDays() {
        return days;
    }

    public void setDays(List<PublicAvailabilityDayResponse> days) {
        this.days = days;
    }
}
