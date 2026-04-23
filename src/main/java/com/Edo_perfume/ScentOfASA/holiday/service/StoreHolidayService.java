package com.Edo_perfume.ScentOfASA.holiday.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
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

        StoreHoliday existing = storeHolidayMapper.findByDateAndLanguage(
                request.getHolidayDate(),
                normalizeLanguage(request.getAppliesToLanguage())
        );

        if (existing != null) {
            throw new IllegalStateException("同じ日付・言語条件の休業日設定がすでに存在します。");
        }

        StoreHoliday storeHoliday = toEntity(request);
        storeHolidayMapper.insert(storeHoliday);
        return toResponse(storeHoliday);
    }

    public HolidayCalendarDayResponse updateHoliday(Long id, StoreHolidayRequest request) {
        validateRequest(request);

        StoreHoliday storeHoliday = storeHolidayMapper.findById(id);
        if (storeHoliday == null) {
            throw new NoSuchElementException("対象の休業日が見つかりません。");
        }

        storeHoliday.setHolidayDate(request.getHolidayDate());
        storeHoliday.setHolidayType(request.getHolidayType());
        storeHoliday.setReason(request.getReason());
        storeHoliday.setAppliesToLanguage(normalizeLanguage(request.getAppliesToLanguage()));
        storeHoliday.setCreatedByStaffId(request.getCreatedByStaffId());

        storeHolidayMapper.update(storeHoliday);
        return toResponse(storeHoliday);
    }

    public void deleteHoliday(Long id) {
        storeHolidayMapper.delete(id);
    }

    public List<HolidayCalendarDayResponse> applyWeeklyRule(HolidayRuleApplyRequest request) {
        if (request.getYear() == null || request.getMonth() == null || request.getWeeklyClosedDay() == null) {
            throw new IllegalArgumentException("年・月・定休日ルールは必須です。");
        }

        YearMonth yearMonth = YearMonth.of(request.getYear(), request.getMonth());
        Set<LocalDate> exceptionDates = new HashSet<>();
        if (request.getOpenExceptionDates() != null) {
            exceptionDates.addAll(request.getOpenExceptionDates());
        }

        List<HolidayCalendarDayResponse> created = new ArrayList<>();
        for (LocalDate date = yearMonth.atDay(1); !date.isAfter(yearMonth.atEndOfMonth()); date = date.plusDays(1)) {
            if (!matches(date, request.getWeeklyClosedDay()) || exceptionDates.contains(date)) {
                continue;
            }

            StoreHoliday existing = storeHolidayMapper.findByDateAndLanguage(
                    date,
                    normalizeLanguage(request.getAppliesToLanguage())
            );
            if (existing != null) {
                created.add(toResponse(existing));
                continue;
            }

            StoreHoliday holiday = new StoreHoliday();
            holiday.setHolidayDate(date);
            holiday.setHolidayType(HolidayType.CLOSED);
            holiday.setReason(request.getReason());
            holiday.setAppliesToLanguage(normalizeLanguage(request.getAppliesToLanguage()));
            holiday.setCreatedByStaffId(request.getCreatedByStaffId());
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
            throw new IllegalArgumentException("休業日は必須です。");
        }
        if (request.getHolidayType() == null) {
            throw new IllegalArgumentException("休業日種別は必須です。");
        }
    }

    private StoreHoliday toEntity(StoreHolidayRequest request) {
        StoreHoliday storeHoliday = new StoreHoliday();
        storeHoliday.setHolidayDate(request.getHolidayDate());
        storeHoliday.setHolidayType(request.getHolidayType());
        storeHoliday.setReason(request.getReason());
        storeHoliday.setAppliesToLanguage(normalizeLanguage(request.getAppliesToLanguage()));
        storeHoliday.setCreatedByStaffId(request.getCreatedByStaffId());
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
