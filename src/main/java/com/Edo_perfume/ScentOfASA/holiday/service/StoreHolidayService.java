package com.Edo_perfume.ScentOfASA.holiday.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.Edo_perfume.ScentOfASA.holiday.domain.HolidayType;
import com.Edo_perfume.ScentOfASA.holiday.domain.StoreHoliday;
import com.Edo_perfume.ScentOfASA.holiday.dto.HolidayCalendarDayResponse;
import com.Edo_perfume.ScentOfASA.holiday.dto.HolidayRuleApplyRequest;
import com.Edo_perfume.ScentOfASA.holiday.dto.StoreHolidayRequest;
import com.Edo_perfume.ScentOfASA.holiday.mapper.StoreHolidayMapper;

@Service
@Transactional
public class StoreHolidayService {

    private final StoreHolidayMapper storeHolidayMapper;

    public StoreHolidayService(StoreHolidayMapper storeHolidayMapper) {
        this.storeHolidayMapper = storeHolidayMapper;
    }

    @Transactional(readOnly = true)
    public List<HolidayCalendarDayResponse> findMonthlyHolidays(int year, int month, String appliesToLanguage) {
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        return storeHolidayMapper.findByMonth(startDate, endDate, normalizeLanguage(appliesToLanguage))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public boolean isHoliday(LocalDate holidayDate, String language) {
        return storeHolidayMapper.existsClosedHoliday(holidayDate, normalizeLanguage(language));
    }

    public HolidayCalendarDayResponse createHoliday(StoreHolidayRequest request) {
        validateRequest(request);

        String normalizedLanguage = normalizeLanguage(request.getAppliesToLanguage());
        StoreHoliday existing = storeHolidayMapper.findByDateAndLanguage(request.getHolidayDate(), normalizedLanguage);
        if (existing != null) {
            throw new IllegalStateException("A holiday setting already exists for that date and language.");
        }

        StoreHoliday storeHoliday = toEntity(request, normalizedLanguage);
        storeHolidayMapper.insert(storeHoliday);
        return toResponse(storeHoliday);
    }

    public HolidayCalendarDayResponse updateHoliday(Long id, StoreHolidayRequest request) {
        validateRequest(request);

        StoreHoliday storeHoliday = storeHolidayMapper.findById(id);
        if (storeHoliday == null) {
            throw new NoSuchElementException("The holiday setting was not found.");
        }

        storeHoliday.setHolidayDate(request.getHolidayDate());
        storeHoliday.setHolidayType(request.getHolidayType());
        storeHoliday.setReason(request.getReason());
        storeHoliday.setAppliesToLanguage(normalizeLanguage(request.getAppliesToLanguage()));
        storeHoliday.setCreatedByStaffId(request.getCreatedByStaffId());
        storeHoliday.setUpdatedAt(LocalDateTime.now());

        storeHolidayMapper.update(storeHoliday);
        return toResponse(storeHoliday);
    }

    public void deleteHoliday(Long id) {
        storeHolidayMapper.delete(id);
    }

    public List<HolidayCalendarDayResponse> applyWeeklyRule(HolidayRuleApplyRequest request) {
        if (request.getYear() == null || request.getMonth() == null || request.getWeeklyClosedDay() == null) {
            throw new IllegalArgumentException("Year, month, and weekly closed day are required.");
        }

        YearMonth yearMonth = YearMonth.of(request.getYear(), request.getMonth());
        String normalizedLanguage = normalizeLanguage(request.getAppliesToLanguage());
        Set<LocalDate> exceptionDates = new HashSet<>();
        if (request.getOpenExceptionDates() != null) {
            exceptionDates.addAll(request.getOpenExceptionDates());
        }

        List<HolidayCalendarDayResponse> created = new ArrayList<>();
        for (LocalDate date = yearMonth.atDay(1); !date.isAfter(yearMonth.atEndOfMonth()); date = date.plusDays(1)) {
            if (!matches(date, request.getWeeklyClosedDay()) || exceptionDates.contains(date)) {
                continue;
            }

            StoreHoliday existing = storeHolidayMapper.findByDateAndLanguage(date, normalizedLanguage);
            if (existing != null) {
                created.add(toResponse(existing));
                continue;
            }

            StoreHoliday holiday = new StoreHoliday();
            holiday.setHolidayDate(date);
            holiday.setHolidayType(HolidayType.CLOSED);
            holiday.setReason(request.getReason());
            holiday.setAppliesToLanguage(normalizedLanguage);
            holiday.setCreatedByStaffId(request.getCreatedByStaffId());
            LocalDateTime now = LocalDateTime.now();
            holiday.setCreatedAt(now);
            holiday.setUpdatedAt(now);
            storeHolidayMapper.insert(holiday);
            created.add(toResponse(holiday));
        }

        return created;
    }

    private boolean matches(LocalDate date, DayOfWeek weeklyClosedDay) {
        return date.getDayOfWeek() == weeklyClosedDay;
    }

    private void validateRequest(StoreHolidayRequest request) {
        if (request.getHolidayDate() == null) {
            throw new IllegalArgumentException("Holiday date is required.");
        }
        if (request.getHolidayType() == null) {
            throw new IllegalArgumentException("Holiday type is required.");
        }
    }

    private StoreHoliday toEntity(StoreHolidayRequest request, String normalizedLanguage) {
        StoreHoliday storeHoliday = new StoreHoliday();
        storeHoliday.setHolidayDate(request.getHolidayDate());
        storeHoliday.setHolidayType(request.getHolidayType());
        storeHoliday.setReason(request.getReason());
        storeHoliday.setAppliesToLanguage(normalizedLanguage);
        storeHoliday.setCreatedByStaffId(request.getCreatedByStaffId());
        LocalDateTime now = LocalDateTime.now();
        storeHoliday.setCreatedAt(now);
        storeHoliday.setUpdatedAt(now);
        return storeHoliday;
    }

    private HolidayCalendarDayResponse toResponse(StoreHoliday storeHoliday) {
        return new HolidayCalendarDayResponse(
                storeHoliday.getId(),
                storeHoliday.getHolidayDate(),
                storeHoliday.getHolidayType() != null ? storeHoliday.getHolidayType().name() : null,
                storeHoliday.getReason(),
                storeHoliday.getAppliesToLanguage()
        );
    }

    private String normalizeLanguage(String language) {
        return language == null || language.isBlank() ? null : language.trim();
    }
}
