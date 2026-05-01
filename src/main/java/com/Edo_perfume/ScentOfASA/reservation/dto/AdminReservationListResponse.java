package com.Edo_perfume.ScentOfASA.reservation.dto;

import java.util.List;

public class AdminReservationListResponse {

    private int totalCount;
    private List<AdminReservationResponse> reservations;

    public AdminReservationListResponse() {
    }

    public AdminReservationListResponse(int totalCount, List<AdminReservationResponse> reservations) {
        this.totalCount = totalCount;
        this.reservations = reservations;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }

    public List<AdminReservationResponse> getReservations() {
        return reservations;
    }

    public void setReservations(List<AdminReservationResponse> reservations) {
        this.reservations = reservations;
    }
}
