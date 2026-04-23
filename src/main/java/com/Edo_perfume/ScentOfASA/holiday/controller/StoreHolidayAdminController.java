package com.Edo_perfume.ScentOfASA.holiday.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.Edo_perfume.ScentOfASA.holiday.dto.HolidayCalendarDayResponse;
import com.Edo_perfume.ScentOfASA.holiday.dto.HolidayRuleApplyRequest;
import com.Edo_perfume.ScentOfASA.holiday.dto.StoreHolidayRequest;
import com.Edo_perfume.ScentOfASA.holiday.service.StoreHolidayService;

@RestController
@RequestMapping("/api/admin/holidays")
public class StoreHolidayAdminController {

    private final StoreHolidayService storeHolidayService;

    public StoreHolidayAdminController(StoreHolidayService storeHolidayService) {
        this.storeHolidayService = storeHolidayService;
    }

    @GetMapping
    public List<HolidayCalendarDayResponse> getMonthlyHolidays(@RequestParam int year,
                                                               @RequestParam int month,
                                                               @RequestParam(required = false) String language) {
        return storeHolidayService.findMonthlyHolidays(year, month, language);
    }

    @GetMapping("/closed")
    public boolean isClosed(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                            @RequestParam(required = false) String language) {
        return storeHolidayService.isHoliday(date, language);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public HolidayCalendarDayResponse createHoliday(@RequestBody StoreHolidayRequest request) {
        return storeHolidayService.createHoliday(request);
    }

    @PutMapping("/{id}")
    public HolidayCalendarDayResponse updateHoliday(@PathVariable Long id,
                                                    @RequestBody StoreHolidayRequest request) {
        return storeHolidayService.updateHoliday(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteHoliday(@PathVariable Long id) {
        storeHolidayService.deleteHoliday(id);
    }

    @PostMapping("/apply-rule")
    public List<HolidayCalendarDayResponse> applyWeeklyRule(@RequestBody HolidayRuleApplyRequest request) {
        return storeHolidayService.applyWeeklyRule(request);
    }
}
