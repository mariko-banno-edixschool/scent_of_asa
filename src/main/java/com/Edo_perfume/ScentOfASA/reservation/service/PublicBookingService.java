package com.Edo_perfume.ScentOfASA.reservation.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.Edo_perfume.ScentOfASA.holiday.dto.HolidayCalendarDayResponse;
import com.Edo_perfume.ScentOfASA.holiday.service.StoreHolidayService;
import com.Edo_perfume.ScentOfASA.reservation.domain.PublicReservation;
import com.Edo_perfume.ScentOfASA.reservation.dto.PublicAvailabilityDayResponse;
import com.Edo_perfume.ScentOfASA.reservation.dto.PublicAvailabilityResponse;
import com.Edo_perfume.ScentOfASA.reservation.dto.PublicAvailabilitySlotResponse;
import com.Edo_perfume.ScentOfASA.reservation.dto.PublicReservationRequest;
import com.Edo_perfume.ScentOfASA.reservation.dto.PublicReservationResponse;
import com.Edo_perfume.ScentOfASA.reservation.mapper.PublicReservationMapper;
import com.Edo_perfume.ScentOfASA.slot.domain.AdminSlot;
import com.Edo_perfume.ScentOfASA.slot.mapper.AdminSlotMapper;
import com.Edo_perfume.ScentOfASA.slot.service.AdminSlotService;

@Service
@Transactional
public class PublicBookingService {

    private static final int SLOT_CAPACITY = 4;
    private static final List<String> SUPPORTED_TIME_SLOTS = List.of("11:00", "13:00", "15:30");
    private static final Set<String> SUPPORTED_LANGUAGES = Set.of("ja", "en");

    private final PublicReservationMapper publicReservationMapper;
    private final AdminSlotMapper adminSlotMapper;
    private final AdminSlotService adminSlotService;
    private final StoreHolidayService storeHolidayService;

    public PublicBookingService(PublicReservationMapper publicReservationMapper,
                                AdminSlotMapper adminSlotMapper,
                                AdminSlotService adminSlotService,
                                StoreHolidayService storeHolidayService) {
        this.publicReservationMapper = publicReservationMapper;
        this.adminSlotMapper = adminSlotMapper;
        this.adminSlotService = adminSlotService;
        this.storeHolidayService = storeHolidayService;
    }

    @Transactional(readOnly = true)
    public PublicAvailabilityResponse getAvailability(int year, int month, String language) {
        validateYearMonth(year, month);
        String normalizedLanguage = normalizeLanguage(language);
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();
        adminSlotService.ensureMonthlySlots(year, month);

        Map<LocalDate, String> closedReasonByDate = new HashMap<>();
        for (HolidayCalendarDayResponse holiday : storeHolidayService.findMonthlyHolidays(year, month, normalizedLanguage)) {
            if ("CLOSED".equals(holiday.getHolidayType())) {
                closedReasonByDate.put(holiday.getHolidayDate(), holiday.getReason());
            }
        }

        Map<LocalDate, Map<String, Integer>> reservedGuestsByDateAndSlot = new HashMap<>();
        for (PublicReservation reservation : publicReservationMapper.findByMonth(startDate, endDate, normalizedLanguage)) {
            reservedGuestsByDateAndSlot
                    .computeIfAbsent(reservation.getReservationDate(), ignored -> new HashMap<>())
                    .merge(reservation.getTimeSlot(), reservation.getGuestCount(), Integer::sum);
        }

        Map<LocalDate, List<AdminSlot>> slotsByDate = new HashMap<>();
        for (AdminSlot slot : adminSlotMapper.findByMonthAndLanguage(startDate, endDate, normalizedLanguage)) {
            slotsByDate.computeIfAbsent(slot.getSlotDate(), ignored -> new ArrayList<>()).add(slot);
        }

        List<PublicAvailabilityDayResponse> days = new ArrayList<>();
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            boolean closed = closedReasonByDate.containsKey(date);
            Map<String, Integer> reservedGuestsBySlot = reservedGuestsByDateAndSlot.getOrDefault(date, Map.of());
            List<AdminSlot> dailyAdminSlots = new ArrayList<>(slotsByDate.getOrDefault(date, List.of()));
            dailyAdminSlots.sort(Comparator.comparing(AdminSlot::getTimeSlot));
            List<PublicAvailabilitySlotResponse> slots = new ArrayList<>();
            for (AdminSlot adminSlot : dailyAdminSlots) {
                String timeSlot = adminSlot.getTimeSlot();
                int reservedGuestCount = reservedGuestsBySlot.getOrDefault(timeSlot, 0);
                int remainingCapacity = Math.max(0, SLOT_CAPACITY - reservedGuestCount);
                if (closed) {
                    slots.add(new PublicAvailabilitySlotResponse(timeSlot, "CLOSED", false, 0, reservedGuestCount));
                } else {
                    slots.add(toAvailabilitySlot(adminSlot, remainingCapacity, reservedGuestCount));
                }
            }

            boolean dayUnavailable = closed || slots.stream().noneMatch(PublicAvailabilitySlotResponse::isAvailable);
            days.add(new PublicAvailabilityDayResponse(date, dayUnavailable, closedReasonByDate.get(date), slots));
        }

        return new PublicAvailabilityResponse(year, month, normalizedLanguage, days);
    }

    public PublicReservationResponse createReservation(PublicReservationRequest request) {
        validateReservationRequest(request);

        String normalizedLanguage = normalizeLanguage(request.getGuideLanguage());
        String normalizedTimeSlot = normalizeTimeSlot(request.getTimeSlot());
        LocalDate reservationDate = request.getReservationDate();

        if (storeHolidayService.isHoliday(reservationDate, normalizedLanguage)) {
            throw new IllegalStateException("The selected date is closed for reservations.");
        }

        AdminSlot adminSlot = adminSlotMapper.findByDateTimeAndLanguage(reservationDate, normalizedTimeSlot, normalizedLanguage);
        if (!isSlotReservable(adminSlot)) {
            throw new IllegalStateException("The selected slot is no longer available.");
        }

        int reservedGuestCount = safeGuestCount(publicReservationMapper
                .sumGuestCountByDateAndTime(reservationDate, normalizedTimeSlot, normalizedLanguage));
        int remainingCapacity = SLOT_CAPACITY - reservedGuestCount;
        if (remainingCapacity <= 0 || request.getGuestCount() > remainingCapacity) {
            throw new IllegalStateException("The selected slot is no longer available.");
        }

        PublicReservation reservation = new PublicReservation();
        reservation.setReservationDate(reservationDate);
        reservation.setTimeSlot(normalizedTimeSlot);
        reservation.setGuideLanguage(normalizedLanguage);
        reservation.setGuestCount(request.getGuestCount());
        reservation.setCustomerName(request.getCustomerName().trim());
        reservation.setCustomerEmail(request.getCustomerEmail().trim().toLowerCase(Locale.ROOT));
        reservation.setCustomerPhone(normalizeOptional(request.getCustomerPhone()));
        reservation.setNotes(normalizeOptional(request.getNotes()));
        reservation.setReservationStatus("PENDING");
        LocalDateTime now = LocalDateTime.now();
        reservation.setCreatedAt(now);
        reservation.setUpdatedAt(now);

        try {
            publicReservationMapper.insert(reservation);
        } catch (DuplicateKeyException ex) {
            throw new IllegalStateException("The selected slot is no longer available.");
        }

        return new PublicReservationResponse(
                reservation.getId(),
                "SOA-" + reservation.getId(),
                reservation.getReservationStatus(),
                reservation.getReservationDate(),
                reservation.getTimeSlot(),
                reservation.getGuideLanguage(),
                reservation.getGuestCount(),
                reservation.getCustomerName()
        );
    }

    private PublicAvailabilitySlotResponse toAvailabilitySlot(AdminSlot adminSlot,
                                                              int remainingCapacity,
                                                              int reservedGuestCount) {
        if (adminSlot == null) {
            return new PublicAvailabilitySlotResponse(null, "STOPPED", false, 0, reservedGuestCount);
        }
        if ("FULL".equals(adminSlot.getSlotStatus()) || remainingCapacity <= 0) {
            return new PublicAvailabilitySlotResponse(adminSlot.getTimeSlot(), "FULL", false, 0, reservedGuestCount);
        }
        if (!isSlotReservable(adminSlot)) {
            return new PublicAvailabilitySlotResponse(adminSlot.getTimeSlot(), "STOPPED", false, 0, reservedGuestCount);
        }
        if ("LIMITED".equals(adminSlot.getSlotStatus()) || remainingCapacity <= 2) {
            return new PublicAvailabilitySlotResponse(adminSlot.getTimeSlot(), "LIMITED", true, remainingCapacity, reservedGuestCount);
        }
        return new PublicAvailabilitySlotResponse(adminSlot.getTimeSlot(), "OPEN", true, remainingCapacity, reservedGuestCount);
    }

    private boolean isSlotReservable(AdminSlot adminSlot) {
        if (adminSlot == null) {
            return false;
        }
        if (adminSlot.getGuideStaffId() == null) {
            return false;
        }
        return !"STOPPED".equals(adminSlot.getSlotStatus()) && !"FULL".equals(adminSlot.getSlotStatus());
    }

    private int safeGuestCount(Integer value) {
        return value == null ? 0 : value;
    }

    private void validateReservationRequest(PublicReservationRequest request) {
        if (request.getReservationDate() == null) {
            throw new IllegalArgumentException("Reservation date is required.");
        }
        if (request.getGuestCount() == null || request.getGuestCount() < 1 || request.getGuestCount() > 4) {
            throw new IllegalArgumentException("Guest count must be between 1 and 4.");
        }
        if (request.getCustomerName() == null || request.getCustomerName().isBlank()) {
            throw new IllegalArgumentException("Customer name is required.");
        }
        if (request.getCustomerEmail() == null || request.getCustomerEmail().isBlank()) {
            throw new IllegalArgumentException("Customer email is required.");
        }
        normalizeLanguage(request.getGuideLanguage());
        normalizeTimeSlot(request.getTimeSlot());
    }

    private void validateYearMonth(int year, int month) {
        if (year < 2000 || year > 2100) {
            throw new IllegalArgumentException("Year must be between 2000 and 2100.");
        }
        if (month < 1 || month > 12) {
            throw new IllegalArgumentException("Month must be between 1 and 12.");
        }
    }

    private String normalizeLanguage(String language) {
        if (language == null || language.isBlank()) {
            return "en";
        }
        String normalizedLanguage = language.trim().toLowerCase(Locale.ROOT);
        if (!SUPPORTED_LANGUAGES.contains(normalizedLanguage)) {
            throw new IllegalArgumentException("Language must be either 'ja' or 'en'.");
        }
        return normalizedLanguage;
    }

    private String normalizeTimeSlot(String timeSlot) {
        if (timeSlot == null || timeSlot.isBlank()) {
            throw new IllegalArgumentException("Time slot is required.");
        }
        String normalizedTimeSlot = timeSlot.trim();
        if (!SUPPORTED_TIME_SLOTS.contains(normalizedTimeSlot)) {
            throw new IllegalArgumentException("Time slot must be one of 11:00, 13:00, or 15:30.");
        }
        return normalizedTimeSlot;
    }

    private String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
