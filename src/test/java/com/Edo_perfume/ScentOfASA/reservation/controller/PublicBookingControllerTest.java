package com.Edo_perfume.ScentOfASA.reservation.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.Edo_perfume.ScentOfASA.reservation.dto.PublicAvailabilityDayResponse;
import com.Edo_perfume.ScentOfASA.reservation.dto.PublicAvailabilityResponse;
import com.Edo_perfume.ScentOfASA.reservation.dto.PublicAvailabilitySlotResponse;
import com.Edo_perfume.ScentOfASA.reservation.dto.PublicReservationResponse;
import com.Edo_perfume.ScentOfASA.reservation.service.PublicBookingService;

@WebMvcTest(PublicBookingController.class)
@Import(PublicBookingApiExceptionHandler.class)
class PublicBookingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PublicBookingService publicBookingService;

    @Test
    void getAvailabilityReturnsCalendarJson() throws Exception {
        when(publicBookingService.getAvailability(2026, 5, "ja"))
                .thenReturn(new PublicAvailabilityResponse(
                        2026,
                        5,
                        "ja",
                        List.of(new PublicAvailabilityDayResponse(
                                LocalDate.of(2026, 5, 12),
                                false,
                                null,
                                List.of(
                                        new PublicAvailabilitySlotResponse("11:00", "OPEN", true),
                                        new PublicAvailabilitySlotResponse("13:00", "BOOKED", false)
                                )
                        ))
                ));

        mockMvc.perform(get("/api/public/availability")
                        .param("year", "2026")
                        .param("month", "5")
                        .param("language", "ja"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.language").value("ja"))
                .andExpect(jsonPath("$.days[0].date").value("2026-05-12"))
                .andExpect(jsonPath("$.days[0].slots[1].status").value("BOOKED"));
    }

    @Test
    void createReservationReturnsCreatedResponse() throws Exception {
        when(publicBookingService.createReservation(any()))
                .thenReturn(new PublicReservationResponse(
                        12L,
                        "SOA-12",
                        "PENDING",
                        LocalDate.of(2026, 5, 12),
                        "13:00",
                        "ja",
                        2,
                        "山田花子"
                ));

        mockMvc.perform(post("/api/public/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reservationDate": "2026-05-12",
                                  "timeSlot": "13:00",
                                  "guideLanguage": "ja",
                                  "guestCount": 2,
                                  "customerName": "山田花子",
                                  "customerEmail": "hanako@example.com"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.reservationCode").value("SOA-12"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void createReservationReturnsConflictMessage() throws Exception {
        when(publicBookingService.createReservation(any()))
                .thenThrow(new IllegalStateException("The selected slot is no longer available."));

        mockMvc.perform(post("/api/public/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reservationDate": "2026-05-12",
                                  "timeSlot": "13:00",
                                  "guideLanguage": "ja",
                                  "guestCount": 2,
                                  "customerName": "山田花子",
                                  "customerEmail": "hanako@example.com"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("The selected slot is no longer available."));
    }
}
