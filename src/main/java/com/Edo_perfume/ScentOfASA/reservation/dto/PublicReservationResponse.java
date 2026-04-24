package com.Edo_perfume.ScentOfASA.reservation.dto;

import java.time.LocalDate;

public class PublicReservationResponse {

    private Long reservationId;
    private String reservationCode;
    private String status;
    private LocalDate reservationDate;
    private String timeSlot;
    private String guideLanguage;
    private Integer guestCount;
    private String customerName;

    public PublicReservationResponse() {
    }

    public PublicReservationResponse(Long reservationId, String reservationCode, String status, LocalDate reservationDate,
                                     String timeSlot, String guideLanguage, Integer guestCount, String customerName) {
        this.reservationId = reservationId;
        this.reservationCode = reservationCode;
        this.status = status;
        this.reservationDate = reservationDate;
        this.timeSlot = timeSlot;
        this.guideLanguage = guideLanguage;
        this.guestCount = guestCount;
        this.customerName = customerName;
    }

    public Long getReservationId() {
        return reservationId;
    }

    public void setReservationId(Long reservationId) {
        this.reservationId = reservationId;
    }

    public String getReservationCode() {
        return reservationCode;
    }

    public void setReservationCode(String reservationCode) {
        this.reservationCode = reservationCode;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDate getReservationDate() {
        return reservationDate;
    }

    public void setReservationDate(LocalDate reservationDate) {
        this.reservationDate = reservationDate;
    }

    public String getTimeSlot() {
        return timeSlot;
    }

    public void setTimeSlot(String timeSlot) {
        this.timeSlot = timeSlot;
    }

    public String getGuideLanguage() {
        return guideLanguage;
    }

    public void setGuideLanguage(String guideLanguage) {
        this.guideLanguage = guideLanguage;
    }

    public Integer getGuestCount() {
        return guestCount;
    }

    public void setGuestCount(Integer guestCount) {
        this.guestCount = guestCount;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }
}
