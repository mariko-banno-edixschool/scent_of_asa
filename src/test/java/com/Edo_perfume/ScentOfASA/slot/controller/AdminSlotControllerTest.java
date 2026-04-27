package com.Edo_perfume.ScentOfASA.slot.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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

import com.Edo_perfume.ScentOfASA.slot.dto.AdminSlotDayResponse;
import com.Edo_perfume.ScentOfASA.slot.dto.AdminSlotMonthResponse;
import com.Edo_perfume.ScentOfASA.slot.dto.AdminSlotResponse;
import com.Edo_perfume.ScentOfASA.slot.service.AdminSlotService;

import static org.mockito.Mockito.when;

@WebMvcTest(AdminSlotController.class)
@Import(AdminSlotApiExceptionHandler.class)
class AdminSlotControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminSlotService adminSlotService;

    @Test
    void getMonthlySlotsReturnsCalendarJson() throws Exception {
        when(adminSlotService.getMonthlySlots(2026, 5))
                .thenReturn(new AdminSlotMonthResponse(
                        2026,
                        5,
                        List.of(new AdminSlotDayResponse(
                                 LocalDate.of(2026, 5, 22),
                                 false,
                                 null,
                                 null,
                                 List.of(new AdminSlotResponse(1L, "11:00", "ja", 4L, "Sato", "OPEN", "CLOSED", false))
                         ))
                 ));

        mockMvc.perform(get("/api/admin/slots")
                        .param("year", "2026")
                        .param("month", "5"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.days[0].date").value("2026-05-22"))
                .andExpect(jsonPath("$.days[0].slots[0].effectiveStatus").value("CLOSED"));
    }

    @Test
    void updateSlotReturnsUpdatedJson() throws Exception {
        when(adminSlotService.updateSlot(any(), any()))
                .thenReturn(new AdminSlotResponse(1L, "11:00", "ja", 4L, "Sato", "LIMITED", "LIMITED", true));

        mockMvc.perform(put("/api/admin/slots/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "guideStaffId": 4,
                                  "guideName": "Sato",
                                  "slotStatus": "LIMITED"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.guideName").value("Sato"))
                .andExpect(jsonPath("$.slotStatus").value("LIMITED"));
    }

    @Test
    void updateSlotReturnsBadRequestMessage() throws Exception {
        when(adminSlotService.updateSlot(any(), any()))
                .thenThrow(new IllegalArgumentException("Slot status must be OPEN, LIMITED, FULL, or STOPPED."));

        mockMvc.perform(put("/api/admin/slots/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "slotStatus": "BAD"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Slot status must be OPEN, LIMITED, FULL, or STOPPED."));
    }
}
