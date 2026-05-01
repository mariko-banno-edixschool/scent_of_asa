package com.Edo_perfume.ScentOfASA.guide.service;

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
import com.Edo_perfume.ScentOfASA.guide.dto.GuideProfileResponse;
import com.Edo_perfume.ScentOfASA.guide.dto.GuideScheduleMonthResponse;
import com.Edo_perfume.ScentOfASA.guide.dto.GuideSlotAssignmentRequest;
import com.Edo_perfume.ScentOfASA.guide.mapper.GuideStaffMapper;
import com.Edo_perfume.ScentOfASA.holiday.dto.HolidayCalendarDayResponse;
import com.Edo_perfume.ScentOfASA.holiday.service.StoreHolidayService;
import com.Edo_perfume.ScentOfASA.reservation.domain.PublicReservation;
import com.Edo_perfume.ScentOfASA.reservation.mapper.PublicReservationMapper;
import com.Edo_perfume.ScentOfASA.slot.domain.AdminSlot;
import com.Edo_perfume.ScentOfASA.slot.dto.AdminSlotDayResponse;
import com.Edo_perfume.ScentOfASA.slot.dto.AdminSlotResponse;
import com.Edo_perfume.ScentOfASA.slot.mapper.AdminSlotMapper;
import com.Edo_perfume.ScentOfASA.slot.service.AdminSlotService;

@Service
@Transactional
public class GuideScheduleService {

    private static final int SLOT_CAPACITY = 4;
    private static final List<String> SUPPORTED_SLOT_STATUSES = List.of("OPEN", "LIMITED", "FULL", "STOPPED");

    private final GuideStaffMapper guideStaffMapper;
    private final AdminSlotMapper adminSlotMapper;
    private final AdminSlotService adminSlotService;
    private final StoreHolidayService storeHolidayService;
    private final PublicReservationMapper publicReservationMapper;

    public GuideScheduleService(GuideStaffMapper guideStaffMapper,
                                AdminSlotMapper adminSlotMapper,
                                AdminSlotService adminSlotService,
                                StoreHolidayService storeHolidayService,
                                PublicReservationMapper publicReservationMapper) {
        this.guideStaffMapper = guideStaffMapper;
        this.adminSlotMapper = adminSlotMapper;
        this.adminSlotService = adminSlotService;
        this.storeHolidayService = storeHolidayService;
        this.publicReservationMapper = publicReservationMapper;
    }

    @Transactional(readOnly = true)
    public GuideProfileResponse getCurrentGuide(String loginId) {
        return toProfile(findActiveGuide(loginId));
    }

    public GuideScheduleMonthResponse getMonthlySchedule(String loginId, int year, int month) {
        validateYearMonth(year, month);
        GuideStaff guide = findActiveGuide(loginId);
        adminSlotService.ensureMonthlySlots(year, month);

        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        List<AdminSlot> slots = adminSlotMapper.findByMonthAndLanguage(startDate, endDate, guide.getGuideLanguage());
        Map<String, Integer> reservedGuestCountBySlotKey = buildReservedGuestCountBySlotKey(startDate, endDate, guide.getGuideLanguage());
        Map<LocalDate, List<AdminSlot>> slotsByDate = new HashMap<>();
        for (AdminSlot slot : slots) {
            slotsByDate.computeIfAbsent(slot.getSlotDate(), ignored -> new ArrayList<>()).add(slot);
        }

        Map<LocalDate, Map<String, HolidayCalendarDayResponse>> holidayMap = buildHolidayMap(year, month);
        List<AdminSlotDayResponse> days = new ArrayList<>();
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            List<AdminSlot> dailySlots = new ArrayList<>(slotsByDate.getOrDefault(date, List.of()));
            dailySlots.sort(Comparator.comparing(AdminSlot::getTimeSlot));

            Map<String, HolidayCalendarDayResponse> dayHolidayMap = holidayMap.getOrDefault(date, Map.of());
            List<AdminSlotResponse> responses = dailySlots.stream()
                    .map(slot -> toResponse(
                            slot,
                            resolveEffectiveStatus(slot, dayHolidayMap),
                            reservedGuestCountBySlotKey.getOrDefault(toSlotKey(slot), 0)))
                    .toList();

            HolidayCalendarDayResponse languageHoliday = dayHolidayMap.get(guide.getGuideLanguage());
            HolidayCalendarDayResponse sharedHoliday = dayHolidayMap.get("all");
            HolidayCalendarDayResponse displayHoliday = languageHoliday != null ? languageHoliday : sharedHoliday;
            boolean dayClosed = !responses.isEmpty()
                    && responses.stream().allMatch(slot -> "CLOSED".equals(slot.getEffectiveStatus()));
            boolean bookingClosed = !dayClosed && isBookingClosedDate(date);

            days.add(new AdminSlotDayResponse(
                    date,
                    dayClosed,
                    bookingClosed,
                    displayHoliday != null ? displayHoliday.getHolidayType() : null,
                    displayHoliday != null ? displayHoliday.getReason() : null,
                    responses
            ));
        }

        return new GuideScheduleMonthResponse(year, month, toProfile(guide), days);
    }

    public AdminSlotResponse updateOwnSlot(Long slotId, GuideSlotAssignmentRequest request) {
        if (request.getAssigned() == null) {
            throw new IllegalArgumentException("Assigned flag is required.");
        }

        GuideStaff guide = findActiveGuide(request.getLoginId());
        AdminSlot slot = adminSlotMapper.findById(slotId);
        if (slot == null) {
            throw new NoSuchElementException("The slot was not found.");
        }
        if (!guide.getGuideLanguage().equals(slot.getGuideLanguage())) {
            throw new IllegalArgumentException("The selected slot does not match your guide language.");
        }
        if (slot.getGuideStaffId() != null && !slot.getGuideStaffId().equals(guide.getId())) {
            throw new IllegalArgumentException("This slot is already assigned to another guide.");
        }

        Map<LocalDate, Map<String, HolidayCalendarDayResponse>> holidayMap =
                buildHolidayMap(slot.getSlotDate().getYear(), slot.getSlotDate().getMonthValue());
        String currentEffectiveStatus = resolveEffectiveStatus(slot, holidayMap.getOrDefault(slot.getSlotDate(), Map.of()));
        if ("CLOSED".equals(currentEffectiveStatus)) {
            throw new IllegalArgumentException("This slot is closed by the holiday control.");
        }
        if (isBookingClosedDate(slot.getSlotDate())) {
            throw new IllegalArgumentException("This slot can no longer be changed because reservations have closed for this date.");
        }

        if (request.getAssigned()) {
            slot.setGuideStaffId(guide.getId());
            slot.setGuideName(guide.getDisplayName());
            slot.setSlotStatus(normalizeGuideSlotStatus(request.getSlotStatus(), slot.getSlotStatus()));
        } else {
            slot.setGuideStaffId(null);
            slot.setGuideName(null);
            slot.setSlotStatus("STOPPED");
        }

        slot.setUpdatedAt(LocalDateTime.now());
        adminSlotMapper.update(slot);
        int reservedGuestCount = safeGuestCount(publicReservationMapper
                .sumGuestCountByDateAndTime(slot.getSlotDate(), slot.getTimeSlot(), slot.getGuideLanguage()));
        return toResponse(slot, resolveEffectiveStatus(slot, holidayMap.getOrDefault(slot.getSlotDate(), Map.of())), reservedGuestCount);
    }

    private GuideStaff findActiveGuide(String loginId) {
        if (loginId == null || loginId.isBlank()) {
            throw new IllegalArgumentException("Login ID is required.");
        }

        GuideStaff guideStaff = guideStaffMapper.findByLoginId(loginId.trim());
        if (guideStaff == null || !guideStaff.isActive()) {
            throw new NoSuchElementException("The guide staff was not found.");
        }
        return guideStaff;
    }

    private GuideProfileResponse toProfile(GuideStaff guideStaff) {
        return new GuideProfileResponse(
                guideStaff.getId(),
                guideStaff.getLoginId(),
                guideStaff.getDisplayName(),
                guideStaff.getGuideLanguage()
        );
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
        return slot.getGuideStaffId() == null ? "STOPPED" : slot.getSlotStatus();
    }

    private AdminSlotResponse toResponse(AdminSlot slot, String effectiveStatus, int reservedGuestCount) {
        int remainingCapacity = "OPEN".equals(effectiveStatus) || "LIMITED".equals(effectiveStatus)
                ? Math.max(0, SLOT_CAPACITY - reservedGuestCount)
                : 0;
        String displayStatus = effectiveStatus;
        if ("OPEN".equals(effectiveStatus) && reservedGuestCount > 0) {
            displayStatus = "LIMITED";
        } else if ("LIMITED".equals(effectiveStatus) && remainingCapacity <= 0) {
            displayStatus = "FULL";
            remainingCapacity = 0;
        }
        return new AdminSlotResponse(
                slot.getId(),
                slot.getTimeSlot(),
                slot.getGuideLanguage(),
                slot.getGuideStaffId(),
                slot.getGuideStaffId() == null ? null : slot.getGuideName(),
                slot.getSlotStatus(),
                displayStatus,
                "OPEN".equals(displayStatus) || "LIMITED".equals(displayStatus),
                remainingCapacity,
                reservedGuestCount
        );
    }

    private String normalizeGuideSlotStatus(String requestedStatus, String currentStatus) {
        if (requestedStatus == null || requestedStatus.isBlank()) {
            return "STOPPED".equals(currentStatus) ? "OPEN" : currentStatus;
        }

        String normalizedStatus = requestedStatus.trim().toUpperCase(Locale.ROOT);
        if (!SUPPORTED_SLOT_STATUSES.contains(normalizedStatus)) {
            throw new IllegalArgumentException("Slot status must be OPEN, LIMITED, FULL, or STOPPED.");
        }
        return normalizedStatus;
    }

    private void validateYearMonth(int year, int month) {
        if (year < 2000 || year > 2100) {
            throw new IllegalArgumentException("Year must be between 2000 and 2100.");
        }
        if (month < 1 || month > 12) {
            throw new IllegalArgumentException("Month must be between 1 and 12.");
        }
    }

    private boolean isBookingClosedDate(LocalDate slotDate) {
        return !slotDate.isAfter(LocalDate.now().plusDays(1));
    }

    private Map<String, Integer> buildReservedGuestCountBySlotKey(LocalDate startDate, LocalDate endDate, String guideLanguage) {
        Map<String, Integer> result = new HashMap<>();
        for (PublicReservation reservation : publicReservationMapper.findByMonth(startDate, endDate, guideLanguage)) {
            result.merge(toSlotKey(reservation.getReservationDate(), reservation.getTimeSlot(), reservation.getGuideLanguage()),
                    reservation.getGuestCount(),
                    Integer::sum);
        }
        return result;
    }

    private String toSlotKey(AdminSlot slot) {
        return toSlotKey(slot.getSlotDate(), slot.getTimeSlot(), slot.getGuideLanguage());
    }

    private String toSlotKey(LocalDate slotDate, String timeSlot, String guideLanguage) {
        return slotDate + "|" + timeSlot + "|" + guideLanguage;
    }

    private int safeGuestCount(Integer value) {
        return value == null ? 0 : value;
    }
}
