package com.Edo_perfume.ScentOfASA.holiday.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.Edo_perfume.ScentOfASA.holiday.dto.HolidayCalendarDayResponse;
import com.Edo_perfume.ScentOfASA.holiday.service.StoreHolidayService;

@WebMvcTest(StoreHolidayAdminController.class)
@Import(GlobalApiExceptionHandler.class)
class StoreHolidayAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StoreHolidayService storeHolidayService;

    @Test
    void getMonthlyHolidaysReturnsJsonArray() throws Exception {
        when(storeHolidayService.findMonthlyHolidays(2026, 5, "ja"))
                .thenReturn(List.of(
                        new HolidayCalendarDayResponse(1L, LocalDate.of(2026, 5, 13), "CLOSED", "社内研修", "ja")
                ));

        mockMvc.perform(get("/api/admin/holidays")
                        .param("year", "2026")
                        .param("month", "5")
                        .param("language", "ja"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].holidayDate").value("2026-05-13"))
                .andExpect(jsonPath("$[0].holidayType").value("CLOSED"))
                .andExpect(jsonPath("$[0].reason").value("社内研修"));
    }

    @Test
    void getMonthlyHolidaysReturnsBadRequestMessageWhenMonthInvalid() throws Exception {
        when(storeHolidayService.findMonthlyHolidays(2026, 13, null))
                .thenThrow(new IllegalArgumentException("Month must be between 1 and 12."));

        mockMvc.perform(get("/api/admin/holidays")
                        .param("year", "2026")
                        .param("month", "13"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Month must be between 1 and 12."));
    }

    @Test
    void createHolidayReturnsConflictMessage() throws Exception {
        when(storeHolidayService.createHoliday(any()))
                .thenThrow(new IllegalStateException("A holiday setting already exists for that date and language."));

        mockMvc.perform(post("/api/admin/holidays")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "holidayDate": "2026-05-13",
                                  "holidayType": "CLOSED",
                                  "reason": "社内研修",
                                  "appliesToLanguage": "ja",
                                  "createdByStaffId": 1
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("A holiday setting already exists for that date and language."));
    }

    @Test
    void deleteHolidayReturnsNotFoundMessage() throws Exception {
        doThrow(new NoSuchElementException("The holiday setting was not found."))
                .when(storeHolidayService)
                .deleteHoliday(99L);

        mockMvc.perform(delete("/api/admin/holidays/99"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("The holiday setting was not found."));
    }

    @Test
    void applyRuleReturnsCreatedDays() throws Exception {
        when(storeHolidayService.applyWeeklyRule(any()))
                .thenReturn(List.of(
                        new HolidayCalendarDayResponse(3L, LocalDate.of(2026, 5, 12), "CLOSED", "定休日ルールで休業", null),
                        new HolidayCalendarDayResponse(4L, LocalDate.of(2026, 5, 19), "CLOSED", "定休日ルールで休業", null)
                ));

        mockMvc.perform(post("/api/admin/holidays/apply-rule")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "year": 2026,
                                  "month": 5,
                                  "weeklyClosedDay": "%s",
                                  "reason": "定休日ルールで休業",
                                  "createdByStaffId": 1,
                                  "openExceptionDates": ["2026-05-05"]
                                }
                                """.formatted(DayOfWeek.TUESDAY.name())))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].holidayDate").value("2026-05-12"))
                .andExpect(jsonPath("$[1].holidayDate").value("2026-05-19"));
    }
}
