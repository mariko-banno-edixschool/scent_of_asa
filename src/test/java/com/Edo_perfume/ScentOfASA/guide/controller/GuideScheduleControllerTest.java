package com.Edo_perfume.ScentOfASA.guide.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
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

import com.Edo_perfume.ScentOfASA.guide.dto.GuideProfileResponse;
import com.Edo_perfume.ScentOfASA.guide.dto.GuideScheduleMonthResponse;
import com.Edo_perfume.ScentOfASA.guide.service.GuideScheduleService;
import com.Edo_perfume.ScentOfASA.slot.dto.AdminSlotDayResponse;
import com.Edo_perfume.ScentOfASA.slot.dto.AdminSlotResponse;

@WebMvcTest(GuideScheduleController.class)
@Import(GuideScheduleApiExceptionHandler.class)
class GuideScheduleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GuideScheduleService guideScheduleService;

    @Test
    void getCurrentGuideReturnsProfileJson() throws Exception {
        when(guideScheduleService.getCurrentGuide("guide_ja_1"))
                .thenReturn(new GuideProfileResponse(1L, "guide_ja_1", "Sato", "ja"));

        mockMvc.perform(get("/api/guide/me").param("loginId", "guide_ja_1"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.displayName").value("Sato"))
                .andExpect(jsonPath("$.guideLanguage").value("ja"));
    }

    @Test
    void getMonthlyScheduleReturnsJson() throws Exception {
        when(guideScheduleService.getMonthlySchedule("guide_en_1", 2026, 5))
                .thenReturn(new GuideScheduleMonthResponse(
                        2026,
                        5,
                        new GuideProfileResponse(1L, "guide_en_1", "Alice", "en"),
                        List.of(new AdminSlotDayResponse(
                                LocalDate.of(2026, 5, 22),
                                false,
                                null,
                                null,
                                List.of(new AdminSlotResponse(1L, "11:00", "en", 1L, "Alice", "OPEN", "OPEN", true))
                        ))
                ));

        mockMvc.perform(get("/api/guide/slots")
                        .param("loginId", "guide_en_1")
                        .param("year", "2026")
                        .param("month", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.guide.loginId").value("guide_en_1"))
                .andExpect(jsonPath("$.days[0].slots[0].guideStaffId").value(1L));
    }

    @Test
    void updateOwnSlotReturnsUpdatedJson() throws Exception {
        when(guideScheduleService.updateOwnSlot(any(), any()))
                .thenReturn(new AdminSlotResponse(1L, "11:00", "ja", 1L, "Sato", "OPEN", "OPEN", true));

        mockMvc.perform(put("/api/guide/slots/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "loginId": "guide_ja_1",
                                  "assigned": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.guideName").value("Sato"))
                .andExpect(jsonPath("$.effectiveStatus").value("OPEN"));
    }
}
