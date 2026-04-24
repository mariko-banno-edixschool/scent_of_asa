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
        validateYearMonth(year, month);
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
        if (normalizedLanguage == null) {
            removeLanguageSpecificRecordsForSharedDate(request.getHolidayDate(), null);
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

        String normalizedLanguage = normalizeLanguage(request.getAppliesToLanguage());
        StoreHoliday duplicate = storeHolidayMapper.findByDateAndLanguage(request.getHolidayDate(), normalizedLanguage);
        if (duplicate != null && !duplicate.getId().equals(id)) {
            throw new IllegalStateException("A holiday setting already exists for that date and language.");
        }
        if (normalizedLanguage == null) {
            removeLanguageSpecificRecordsForSharedDate(request.getHolidayDate(), id);
        }

        storeHoliday.setHolidayDate(request.getHolidayDate());
        storeHoliday.setHolidayType(request.getHolidayType());
        storeHoliday.setReason(normalizeReason(request.getReason()));
        storeHoliday.setAppliesToLanguage(normalizedLanguage);
        storeHoliday.setCreatedByStaffId(request.getCreatedByStaffId());
        storeHoliday.setUpdatedAt(LocalDateTime.now());

        storeHolidayMapper.update(storeHoliday);
        return toResponse(storeHoliday);
    }

    public void deleteHoliday(Long id) {
        if (storeHolidayMapper.delete(id) == 0) {
            throw new NoSuchElementException("The holiday setting was not found.");
        }
    }

    public List<HolidayCalendarDayResponse> applyWeeklyRule(HolidayRuleApplyRequest request) {
        if (request.getYear() == null || request.getMonth() == null || request.getWeeklyClosedDay() == null) {
            throw new IllegalArgumentException("Year, month, and weekly closed day are required.");
        }
        validateYearMonth(request.getYear(), request.getMonth());

        YearMonth yearMonth = YearMonth.of(request.getYear(), request.getMonth());
        String normalizedLanguage = normalizeLanguage(request.getAppliesToLanguage());
        Set<LocalDate> exceptionDates = new HashSet<>();
        if (request.getOpenExceptionDates() != null) {
            exceptionDates.addAll(request.getOpenExceptionDates());
        }

        List<HolidayCalendarDayResponse> created = new ArrayList<>();
        for (LocalDate date = yearMonth.atDay(1); !date.isAfter(yearMonth.atEndOfMonth()); date = date.plusDays(1)) {
            if (!matches(date, request.getWeeklyClosedDay())) {
                continue;
            }

            if (exceptionDates.contains(date)) {
                created.add(applyOpenException(date, normalizedLanguage, request.getCreatedByStaffId()));
                continue;
            }

            StoreHoliday existing = storeHolidayMapper.findByDateAndLanguage(date, normalizedLanguage);
            if (existing != null) {
                if (existing.getHolidayType() != HolidayType.CLOSED || !request.getWeeklyClosedDay().equals(date.getDayOfWeek())) {
                    existing.setHolidayType(HolidayType.CLOSED);
                    existing.setReason(normalizeReason(request.getReason()));
                    existing.setCreatedByStaffId(request.getCreatedByStaffId());
                    existing.setUpdatedAt(LocalDateTime.now());
                    storeHolidayMapper.update(existing);
                }
                created.add(toResponse(existing));
                continue;
            }

            StoreHoliday holiday = buildHoliday(date, HolidayType.CLOSED, normalizeReason(request.getReason()),
                    normalizedLanguage, request.getCreatedByStaffId());
            storeHolidayMapper.insert(holiday);
            created.add(toResponse(holiday));
        }

        return created;
    }

    private HolidayCalendarDayResponse applyOpenException(LocalDate date, String normalizedLanguage, Long createdByStaffId) {
        StoreHoliday existing = storeHolidayMapper.findByDateAndLanguage(date, normalizedLanguage);
        if (existing == null) {
            StoreHoliday holiday = buildHoliday(date, HolidayType.SPECIAL_OPEN, null, normalizedLanguage, createdByStaffId);
            storeHolidayMapper.insert(holiday);
            return toResponse(holiday);
        }

        existing.setHolidayType(HolidayType.SPECIAL_OPEN);
        existing.setReason(null);
        existing.setCreatedByStaffId(createdByStaffId);
        existing.setUpdatedAt(LocalDateTime.now());
        storeHolidayMapper.update(existing);
        return toResponse(existing);
    }

    private boolean matches(LocalDate date, DayOfWeek weeklyClosedDay) {
        return date.getDayOfWeek() == weeklyClosedDay;
    }

    private void removeLanguageSpecificRecordsForSharedDate(LocalDate holidayDate, Long keepId) {
        for (StoreHoliday holiday : storeHolidayMapper.findByDate(holidayDate)) {
            if (holiday.getAppliesToLanguage() == null) {
                continue;
            }
            if (keepId != null && keepId.equals(holiday.getId())) {
                continue;
            }
            storeHolidayMapper.delete(holiday.getId());
        }
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
        return buildHoliday(request.getHolidayDate(), request.getHolidayType(), normalizeReason(request.getReason()),
                normalizedLanguage, request.getCreatedByStaffId());
    }

    private StoreHoliday buildHoliday(LocalDate date, HolidayType holidayType, String reason,
                                      String normalizedLanguage, Long createdByStaffId) {
        StoreHoliday storeHoliday = new StoreHoliday();
        storeHoliday.setHolidayDate(date);
        storeHoliday.setHolidayType(holidayType);
        storeHoliday.setReason(reason);
        storeHoliday.setAppliesToLanguage(normalizedLanguage);
        storeHoliday.setCreatedByStaffId(createdByStaffId);
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
        return language == null || language.isBlank() ? null : language.trim().toLowerCase();
    }

    private String normalizeReason(String reason) {
        return reason == null || reason.isBlank() ? null : reason.trim();
    }

    private void validateYearMonth(int year, int month) {
        if (year < 2000 || year > 2100) {
            throw new IllegalArgumentException("Year must be between 2000 and 2100.");
        }
        if (month < 1 || month > 12) {
            throw new IllegalArgumentException("Month must be between 1 and 12.");
        }
    }
}
