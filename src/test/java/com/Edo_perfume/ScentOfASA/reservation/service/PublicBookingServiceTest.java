package com.Edo_perfume.ScentOfASA.reservation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.Edo_perfume.ScentOfASA.holiday.dto.HolidayCalendarDayResponse;
import com.Edo_perfume.ScentOfASA.holiday.service.StoreHolidayService;
import com.Edo_perfume.ScentOfASA.reservation.domain.PublicReservation;
import com.Edo_perfume.ScentOfASA.reservation.dto.PublicAvailabilityResponse;
import com.Edo_perfume.ScentOfASA.reservation.dto.PublicReservationRequest;
import com.Edo_perfume.ScentOfASA.reservation.dto.PublicReservationResponse;
import com.Edo_perfume.ScentOfASA.reservation.mapper.PublicReservationMapper;

@ExtendWith(MockitoExtension.class)
class PublicBookingServiceTest {

    @Mock
    private PublicReservationMapper publicReservationMapper;

    @Mock
    private StoreHolidayService storeHolidayService;

    @InjectMocks
    private PublicBookingService publicBookingService;

    @Test
    void getAvailabilityMarksClosedDaysAndBookedSlots() {
        when(storeHolidayService.findMonthlyHolidays(2026, 5, "ja"))
                .thenReturn(List.of(new HolidayCalendarDayResponse(1L, LocalDate.of(2026, 5, 13), "CLOSED", "社内研修", "ja")));

        PublicReservation bookedReservation = new PublicReservation();
        bookedReservation.setReservationDate(LocalDate.of(2026, 5, 12));
        bookedReservation.setTimeSlot("13:00");
        when(publicReservationMapper.findByMonth(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31), "ja"))
                .thenReturn(List.of(bookedReservation));

        PublicAvailabilityResponse response = publicBookingService.getAvailability(2026, 5, "ja");

        assertThat(response.getLanguage()).isEqualTo("ja");
        assertThat(response.getDays()).hasSize(31);
        assertThat(response.getDays().stream()
                .filter(day -> day.getDate().equals(LocalDate.of(2026, 5, 13)))
                .findFirst()
                .orElseThrow()
                .isClosed()).isTrue();
        assertThat(response.getDays().stream()
                .filter(day -> day.getDate().equals(LocalDate.of(2026, 5, 12)))
                .findFirst()
                .orElseThrow()
                .getSlots())
                .anySatisfy(slot -> {
                    assertThat(slot.getTimeSlot()).isEqualTo("13:00");
                    assertThat(slot.getStatus()).isEqualTo("BOOKED");
                    assertThat(slot.isAvailable()).isFalse();
                });
    }

    @Test
    void createReservationRejectsClosedDates() {
        PublicReservationRequest request = new PublicReservationRequest();
        request.setReservationDate(LocalDate.of(2026, 5, 13));
        request.setTimeSlot("13:00");
        request.setGuideLanguage("ja");
        request.setGuestCount(2);
        request.setCustomerName("山田花子");
        request.setCustomerEmail("hanako@example.com");

        when(storeHolidayService.isHoliday(LocalDate.of(2026, 5, 13), "ja")).thenReturn(true);

        assertThatThrownBy(() -> publicBookingService.createReservation(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("The selected date is closed for reservations.");

        verify(publicReservationMapper, never()).insert(any());
    }

    @Test
    void createReservationNormalizesFieldsBeforeInsert() {
        PublicReservationRequest request = new PublicReservationRequest();
        request.setReservationDate(LocalDate.of(2026, 5, 12));
        request.setTimeSlot(" 13:00 ");
        request.setGuideLanguage(" JA ");
        request.setGuestCount(3);
        request.setCustomerName(" 山田花子 ");
        request.setCustomerEmail(" HANAKO@EXAMPLE.COM ");
        request.setCustomerPhone(" 090-1234-5678 ");
        request.setNotes(" 初回来店 ");

        when(storeHolidayService.isHoliday(LocalDate.of(2026, 5, 12), "ja")).thenReturn(false);
        when(publicReservationMapper.existsByDateAndTime(LocalDate.of(2026, 5, 12), "13:00", "ja")).thenReturn(false);

        PublicReservationResponse response = publicBookingService.createReservation(request);
        ArgumentCaptor<PublicReservation> captor = ArgumentCaptor.forClass(PublicReservation.class);

        verify(publicReservationMapper).insert(captor.capture());
        PublicReservation inserted = captor.getValue();
        inserted.setId(7L);

        assertThat(inserted.getGuideLanguage()).isEqualTo("ja");
        assertThat(inserted.getTimeSlot()).isEqualTo("13:00");
        assertThat(inserted.getCustomerEmail()).isEqualTo("hanako@example.com");
        assertThat(inserted.getCustomerPhone()).isEqualTo("090-1234-5678");
        assertThat(inserted.getNotes()).isEqualTo("初回来店");
        assertThat(response.getStatus()).isEqualTo("PENDING");
    }
}
