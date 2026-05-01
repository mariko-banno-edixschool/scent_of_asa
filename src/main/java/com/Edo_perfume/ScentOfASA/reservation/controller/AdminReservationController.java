package com.Edo_perfume.ScentOfASA.reservation.controller;

import java.time.LocalDate;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.Edo_perfume.ScentOfASA.reservation.dto.AdminReservationListResponse;
import com.Edo_perfume.ScentOfASA.reservation.dto.AdminReservationResponse;
import com.Edo_perfume.ScentOfASA.reservation.dto.AdminReservationStatusUpdateRequest;
import com.Edo_perfume.ScentOfASA.reservation.service.AdminReservationService;

@RestController
@RequestMapping("/api/admin/reservations")
public class AdminReservationController {

    private final AdminReservationService adminReservationService;

    public AdminReservationController(AdminReservationService adminReservationService) {
        this.adminReservationService = adminReservationService;
    }

    @GetMapping
    public AdminReservationListResponse searchReservations(@RequestParam(required = false) LocalDate date,
                                                           @RequestParam(required = false) String customerName,
                                                           @RequestParam(required = false) String guideLanguage) {
        return adminReservationService.searchReservations(date, customerName, guideLanguage);
    }

    @PutMapping("/{id}/status")
    public AdminReservationResponse updateReservationStatus(@PathVariable Long id,
                                                            @RequestBody AdminReservationStatusUpdateRequest request) {
        return adminReservationService.updateReservationStatus(id, request.getReservationStatus());
    }
}
