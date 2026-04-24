package com.Edo_perfume.ScentOfASA.reservation.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
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

@Service
@Transactional
public class PublicBookingService {

    private static final List<String> SUPPORTED_TIME_SLOTS = List.of("11:00", "13:00", "15:30");
    private static final Set<String> SUPPORTED_LANGUAGES = Set.of("ja", "en");

    private final PublicReservationMapper publicReservationMapper;
    private final StoreHolidayService storeHolidayService;

    public PublicBookingService(PublicReservationMapper publicReservationMapper,
                                StoreHolidayService storeHolidayService) {
        this.publicReservationMapper = publicReservationMapper;
        this.storeHolidayService = storeHolidayService;
    }

    @Transactional(readOnly = true)
    public PublicAvailabilityResponse getAvailability(int year, int month, String language) {
        validateYearMonth(year, month);
        String normalizedLanguage = normalizeLanguage(language);
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        Map<LocalDate, String> closedReasonByDate = new HashMap<>();
        for (HolidayCalendarDayResponse holiday : storeHolidayService.findMonthlyHolidays(year, month, normalizedLanguage)) {
            if ("CLOSED".equals(holiday.getHolidayType())) {
                closedReasonByDate.put(holiday.getHolidayDate(), holiday.getReason());
            }
        }

        Map<LocalDate, Set<String>> bookedSlotsByDate = new HashMap<>();
        for (PublicReservation reservation : publicReservationMapper.findByMonth(startDate, endDate, normalizedLanguage)) {
            bookedSlotsByDate.computeIfAbsent(reservation.getReservationDate(), ignored -> new HashSet<>())
                    .add(reservation.getTimeSlot());
        }

        List<PublicAvailabilityDayResponse> days = new ArrayList<>();
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            boolean closed = closedReasonByDate.containsKey(date);
            Set<String> bookedSlots = bookedSlotsByDate.getOrDefault(date, Set.of());
            List<PublicAvailabilitySlotResponse> slots = new ArrayList<>();
            for (String timeSlot : SUPPORTED_TIME_SLOTS) {
                if (closed) {
                    slots.add(new PublicAvailabilitySlotResponse(timeSlot, "CLOSED", false));
                } else if (bookedSlots.contains(timeSlot)) {
                    slots.add(new PublicAvailabilitySlotResponse(timeSlot, "BOOKED", false));
                } else {
                    slots.add(new PublicAvailabilitySlotResponse(timeSlot, "OPEN", true));
                }
            }
            days.add(new PublicAvailabilityDayResponse(date, closed, closedReasonByDate.get(date), slots));
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
        if (publicReservationMapper.existsByDateAndTime(reservationDate, normalizedTimeSlot, normalizedLanguage)) {
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
