package com.Edo_perfume.ScentOfASA.reservation.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.Edo_perfume.ScentOfASA.reservation.dto.PublicAvailabilityResponse;
import com.Edo_perfume.ScentOfASA.reservation.dto.PublicReservationRequest;
import com.Edo_perfume.ScentOfASA.reservation.dto.PublicReservationResponse;
import com.Edo_perfume.ScentOfASA.reservation.service.PublicBookingService;

@RestController
@RequestMapping("/api/public")
public class PublicBookingController {

    private final PublicBookingService publicBookingService;

    public PublicBookingController(PublicBookingService publicBookingService) {
        this.publicBookingService = publicBookingService;
    }

    @GetMapping("/availability")
    public PublicAvailabilityResponse getAvailability(@RequestParam int year,
                                                      @RequestParam int month,
                                                      @RequestParam(required = false) String language) {
        return publicBookingService.getAvailability(year, month, language);
    }

    @PostMapping("/reservations")
    @ResponseStatus(HttpStatus.CREATED)
    public PublicReservationResponse createReservation(@RequestBody PublicReservationRequest request) {
        return publicBookingService.createReservation(request);
    }
}
