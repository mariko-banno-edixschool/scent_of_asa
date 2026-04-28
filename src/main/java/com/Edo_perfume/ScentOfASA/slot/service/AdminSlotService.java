package com.Edo_perfume.ScentOfASA.slot.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.Edo_perfume.ScentOfASA.guide.domain.GuideStaff;
import com.Edo_perfume.ScentOfASA.guide.mapper.GuideStaffMapper;
import com.Edo_perfume.ScentOfASA.holiday.dto.HolidayCalendarDayResponse;
import com.Edo_perfume.ScentOfASA.holiday.service.StoreHolidayService;
import com.Edo_perfume.ScentOfASA.slot.domain.AdminSlot;
import com.Edo_perfume.ScentOfASA.slot.dto.AdminSlotDayResponse;
import com.Edo_perfume.ScentOfASA.slot.dto.AdminSlotMonthResponse;
import com.Edo_perfume.ScentOfASA.slot.dto.AdminSlotResponse;
import com.Edo_perfume.ScentOfASA.slot.dto.AdminSlotUpdateRequest;
import com.Edo_perfume.ScentOfASA.slot.mapper.AdminSlotMapper;

@Service
@Transactional
public class AdminSlotService {

    private static final List<String> SUPPORTED_TIME_SLOTS = List.of("11:00", "13:00", "15:30");
    private static final List<String> SUPPORTED_LANGUAGES = List.of("en", "ja");
    private static final List<String> SUPPORTED_SLOT_STATUSES = List.of("OPEN", "LIMITED", "FULL", "STOPPED");

    private final AdminSlotMapper adminSlotMapper;
    private final GuideStaffMapper guideStaffMapper;
    private final StoreHolidayService storeHolidayService;

    public AdminSlotService(AdminSlotMapper adminSlotMapper,
                            GuideStaffMapper guideStaffMapper,
                            StoreHolidayService storeHolidayService) {
        this.adminSlotMapper = adminSlotMapper;
        this.guideStaffMapper = guideStaffMapper;
        this.storeHolidayService = storeHolidayService;
    }

    public AdminSlotMonthResponse getMonthlySlots(int year, int month) {
        validateYearMonth(year, month);
        YearMonth yearMonth = prepareMonthlySlots(year, month);

        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();
        List<AdminSlot> slots = adminSlotMapper.findByMonth(startDate, endDate);
        Map<LocalDate, List<AdminSlot>> slotsByDate = new HashMap<>();
        for (AdminSlot slot : slots) {
            slotsByDate.computeIfAbsent(slot.getSlotDate(), ignored -> new ArrayList<>()).add(slot);
        }

        Map<LocalDate, Map<String, HolidayCalendarDayResponse>> holidayByDateAndLanguage = buildHolidayMap(year, month);
        List<AdminSlotDayResponse> days = new ArrayList<>();
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            List<AdminSlot> dailySlots = new ArrayList<>(slotsByDate.getOrDefault(date, List.of()));
            dailySlots.sort(Comparator
                    .comparing(AdminSlot::getTimeSlot)
                    .thenComparing(AdminSlot::getGuideLanguage));

            Map<String, HolidayCalendarDayResponse> dayHolidayMap = holidayByDateAndLanguage.getOrDefault(date, Map.of());
            List<AdminSlotResponse> responses = dailySlots.stream()
                    .map(slot -> toResponse(slot, resolveEffectiveStatus(slot, dayHolidayMap)))
                    .toList();
            boolean dayClosed = !responses.isEmpty()
                    && responses.stream().allMatch(slot -> "CLOSED".equals(slot.getEffectiveStatus()));
            HolidayCalendarDayResponse sharedHoliday = dayHolidayMap.get("all");

            String holidayType = sharedHoliday != null ? sharedHoliday.getHolidayType() : null;
            String holidayReason = sharedHoliday != null ? sharedHoliday.getReason() : null;
            days.add(new AdminSlotDayResponse(date, dayClosed, holidayType, holidayReason, responses));
        }

        return new AdminSlotMonthResponse(year, month, days);
    }

    public AdminSlotResponse updateSlot(Long id, AdminSlotUpdateRequest request) {
        if (request.getSlotStatus() == null || request.getSlotStatus().isBlank()) {
            throw new IllegalArgumentException("Slot status is required.");
        }

        AdminSlot slot = adminSlotMapper.findById(id);
        if (slot == null) {
            throw new NoSuchElementException("The slot was not found.");
        }

        String normalizedStatus = request.getSlotStatus().trim().toUpperCase(Locale.ROOT);
        if (!SUPPORTED_SLOT_STATUSES.contains(normalizedStatus)) {
            throw new IllegalArgumentException("Slot status must be OPEN, LIMITED, FULL, or STOPPED.");
        }

        applyGuideAssignment(slot, request, normalizedStatus);
        slot.setUpdatedAt(LocalDateTime.now());
        adminSlotMapper.update(slot);

        Map<LocalDate, Map<String, HolidayCalendarDayResponse>> holidayMap =
                buildHolidayMap(slot.getSlotDate().getYear(), slot.getSlotDate().getMonthValue());
        return toResponse(slot, resolveEffectiveStatus(slot, holidayMap.getOrDefault(slot.getSlotDate(), Map.of())));
    }

    public void ensureMonthlySlots(int year, int month) {
        prepareMonthlySlots(year, month);
    }

    private YearMonth prepareMonthlySlots(int year, int month) {
        YearMonth yearMonth = YearMonth.of(year, month);
        ensureMonthlySlots(yearMonth);
        return yearMonth;
    }

    private void ensureMonthlySlots(YearMonth yearMonth) {
        for (LocalDate date = yearMonth.atDay(1); !date.isAfter(yearMonth.atEndOfMonth()); date = date.plusDays(1)) {
            for (String guideLanguage : SUPPORTED_LANGUAGES) {
                for (String timeSlot : SUPPORTED_TIME_SLOTS) {
                    if (adminSlotMapper.findByDateTimeAndLanguage(date, timeSlot, guideLanguage) != null) {
                        continue;
                    }
                    AdminSlot slot = new AdminSlot();
                    slot.setSlotDate(date);
                    slot.setTimeSlot(timeSlot);
                    slot.setGuideLanguage(guideLanguage);
                    slot.setGuideStaffId(null);
                    slot.setGuideName(null);
                    slot.setSlotStatus("STOPPED");
                    LocalDateTime now = LocalDateTime.now();
                    slot.setCreatedAt(now);
                    slot.setUpdatedAt(now);
                    adminSlotMapper.insert(slot);
                }
            }
        }
    }

    private Map<LocalDate, Map<String, HolidayCalendarDayResponse>> buildHolidayMap(int year, int month) {
        Map<LocalDate, Map<String, HolidayCalendarDayResponse>> result = new HashMap<>();
        for (HolidayCalendarDayResponse holiday : storeHolidayService.findMonthlyHolidays(year, month, null)) {
            String languageKey = holiday.getAppliesToLanguage() == null ? "all" : holiday.getAppliesToLanguage();
            result.computeIfAbsent(holiday.getHolidayDate(), ignored -> new HashMap<>())
                    .put(languageKey, holiday);
        }
        return result;
    }

    private String resolveEffectiveStatus(AdminSlot slot, Map<String, HolidayCalendarDayResponse> holidayMap) {
        HolidayCalendarDayResponse languageHoliday = holidayMap.get(slot.getGuideLanguage());
        HolidayCalendarDayResponse sharedHoliday = holidayMap.get("all");

        if (languageHoliday != null) {
            if ("SPECIAL_OPEN".equals(languageHoliday.getHolidayType())) {
                return slot.getGuideStaffId() == null ? "STOPPED" : slot.getSlotStatus();
            }
            if ("CLOSED".equals(languageHoliday.getHolidayType())) {
                return "CLOSED";
            }
        }
        if (sharedHoliday != null) {
            if ("SPECIAL_OPEN".equals(sharedHoliday.getHolidayType())) {
                return slot.getGuideStaffId() == null ? "STOPPED" : slot.getSlotStatus();
            }
            if ("CLOSED".equals(sharedHoliday.getHolidayType())) {
                return "CLOSED";
            }
        }
        if (slot.getGuideStaffId() == null) {
            return "STOPPED";
        }
        return slot.getSlotStatus();
    }

    private AdminSlotResponse toResponse(AdminSlot slot, String effectiveStatus) {
        return new AdminSlotResponse(
                slot.getId(),
                slot.getTimeSlot(),
                slot.getGuideLanguage(),
                slot.getGuideStaffId(),
                slot.getGuideStaffId() == null ? null : slot.getGuideName(),
                slot.getSlotStatus(),
                effectiveStatus,
                "OPEN".equals(effectiveStatus) || "LIMITED".equals(effectiveStatus)
        );
    }

    private void validateYearMonth(int year, int month) {
        if (year < 2000 || year > 2100) {
            throw new IllegalArgumentException("Year must be between 2000 and 2100.");
        }
        if (month < 1 || month > 12) {
            throw new IllegalArgumentException("Month must be between 1 and 12.");
        }
    }

    private void applyGuideAssignment(AdminSlot slot, AdminSlotUpdateRequest request, String normalizedStatus) {
        if (request.getGuideStaffId() == null) {
            if (!"STOPPED".equals(normalizedStatus)) {
                throw new IllegalArgumentException("A guide staff assignment is required unless the slot is STOPPED.");
            }
            slot.setGuideStaffId(null);
            slot.setGuideName(null);
            slot.setSlotStatus(normalizedStatus);
            return;
        }

        GuideStaff guideStaff = guideStaffMapper.findById(request.getGuideStaffId());
        if (guideStaff == null || !guideStaff.isActive()) {
            throw new NoSuchElementException("The guide staff was not found.");
        }
        if (!slot.getGuideLanguage().equals(guideStaff.getGuideLanguage())) {
            throw new IllegalArgumentException("The selected guide staff language does not match the slot language.");
        }

        slot.setGuideStaffId(guideStaff.getId());
        slot.setGuideName(guideStaff.getDisplayName());
        slot.setSlotStatus(normalizedStatus);
    }
}
