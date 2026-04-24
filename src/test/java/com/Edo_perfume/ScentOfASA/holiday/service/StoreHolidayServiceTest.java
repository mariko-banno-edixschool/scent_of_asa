package com.Edo_perfume.ScentOfASA.holiday.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.Edo_perfume.ScentOfASA.holiday.domain.HolidayType;
import com.Edo_perfume.ScentOfASA.holiday.domain.StoreHoliday;
import com.Edo_perfume.ScentOfASA.holiday.dto.HolidayCalendarDayResponse;
import com.Edo_perfume.ScentOfASA.holiday.dto.HolidayRuleApplyRequest;
import com.Edo_perfume.ScentOfASA.holiday.dto.StoreHolidayRequest;
import com.Edo_perfume.ScentOfASA.holiday.mapper.StoreHolidayMapper;

@ExtendWith(MockitoExtension.class)
class StoreHolidayServiceTest {

    @Mock
    private StoreHolidayMapper storeHolidayMapper;

    @InjectMocks
    private StoreHolidayService storeHolidayService;

    @Test
    void createHolidaySetsTimestampsBeforeInsert() {
        StoreHolidayRequest request = new StoreHolidayRequest();
        request.setHolidayDate(LocalDate.of(2026, 5, 10));
        request.setHolidayType(HolidayType.CLOSED);
        request.setReason("Closed for event");
        request.setAppliesToLanguage(" ja ");
        request.setCreatedByStaffId(2L);

        when(storeHolidayMapper.findByDateAndLanguage(LocalDate.of(2026, 5, 10), "ja")).thenReturn(null);

        HolidayCalendarDayResponse response = storeHolidayService.createHoliday(request);
        ArgumentCaptor<StoreHoliday> captor = ArgumentCaptor.forClass(StoreHoliday.class);

        verify(storeHolidayMapper).insert(captor.capture());
        StoreHoliday inserted = captor.getValue();

        assertThat(inserted.getCreatedAt()).isNotNull();
        assertThat(inserted.getUpdatedAt()).isNotNull();
        assertThat(inserted.getAppliesToLanguage()).isEqualTo("ja");
        assertThat(response.getHolidayDate()).isEqualTo(LocalDate.of(2026, 5, 10));
    }

    @Test
    void updateHolidayRejectsDuplicateDateAndLanguage() {
        StoreHolidayRequest request = new StoreHolidayRequest();
        request.setHolidayDate(LocalDate.of(2026, 5, 11));
        request.setHolidayType(HolidayType.CLOSED);
        request.setAppliesToLanguage(" EN ");

        StoreHoliday existing = new StoreHoliday();
        existing.setId(1L);
        existing.setHolidayDate(LocalDate.of(2026, 5, 10));
        existing.setUpdatedAt(LocalDateTime.of(2026, 4, 23, 12, 0));

        StoreHoliday duplicate = new StoreHoliday();
        duplicate.setId(2L);

        when(storeHolidayMapper.findById(1L)).thenReturn(existing);
        when(storeHolidayMapper.findByDateAndLanguage(LocalDate.of(2026, 5, 11), "en")).thenReturn(duplicate);

        assertThatThrownBy(() -> storeHolidayService.updateHoliday(1L, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("A holiday setting already exists for that date and language.");

        verify(storeHolidayMapper, never()).update(any());
    }

    @Test
    void applyWeeklyRuleCreatesSpecialOpenForExceptionDates() {
        HolidayRuleApplyRequest request = new HolidayRuleApplyRequest();
        request.setYear(2026);
        request.setMonth(6);
        request.setWeeklyClosedDay(DayOfWeek.MONDAY);
        request.setOpenExceptionDates(List.of(LocalDate.of(2026, 6, 1)));

        when(storeHolidayMapper.findByDateAndLanguage(any(LocalDate.class), any())).thenReturn(null);

        List<HolidayCalendarDayResponse> result = storeHolidayService.applyWeeklyRule(request);

        assertThat(result).extracting(HolidayCalendarDayResponse::getHolidayDate)
                .containsExactly(
                        LocalDate.of(2026, 6, 1),
                        LocalDate.of(2026, 6, 8),
                        LocalDate.of(2026, 6, 15),
                        LocalDate.of(2026, 6, 22),
                        LocalDate.of(2026, 6, 29)
                );
        assertThat(result.get(0).getHolidayType()).isEqualTo("SPECIAL_OPEN");
    }

    @Test
    void applyWeeklyRuleTurnsExceptionDateIntoSpecialOpenWhenClosedHolidayAlreadyExists() {
        HolidayRuleApplyRequest request = new HolidayRuleApplyRequest();
        request.setYear(2026);
        request.setMonth(5);
        request.setWeeklyClosedDay(DayOfWeek.TUESDAY);
        request.setOpenExceptionDates(List.of(LocalDate.of(2026, 5, 5)));
        request.setCreatedByStaffId(1L);

        StoreHoliday existingException = new StoreHoliday();
        existingException.setId(10L);
        existingException.setHolidayDate(LocalDate.of(2026, 5, 5));
        existingException.setHolidayType(HolidayType.CLOSED);
        existingException.setReason("Weekly rule closed");

        when(storeHolidayMapper.findByDateAndLanguage(LocalDate.of(2026, 5, 5), null)).thenReturn(existingException);
        when(storeHolidayMapper.findByDateAndLanguage(LocalDate.of(2026, 5, 12), null)).thenReturn(null);
        when(storeHolidayMapper.findByDateAndLanguage(LocalDate.of(2026, 5, 19), null)).thenReturn(null);
        when(storeHolidayMapper.findByDateAndLanguage(LocalDate.of(2026, 5, 26), null)).thenReturn(null);

        List<HolidayCalendarDayResponse> result = storeHolidayService.applyWeeklyRule(request);

        assertThat(existingException.getHolidayType()).isEqualTo(HolidayType.SPECIAL_OPEN);
        assertThat(existingException.getReason()).isNull();
        assertThat(result).extracting(HolidayCalendarDayResponse::getHolidayDate)
                .containsExactly(
                        LocalDate.of(2026, 5, 5),
                        LocalDate.of(2026, 5, 12),
                        LocalDate.of(2026, 5, 19),
                        LocalDate.of(2026, 5, 26)
                );
        assertThat(result.get(0).getHolidayType()).isEqualTo("SPECIAL_OPEN");
        verify(storeHolidayMapper).update(existingException);
    }

    @Test
    void createHolidayRejectsDuplicateDateAndLanguage() {
        StoreHolidayRequest request = new StoreHolidayRequest();
        request.setHolidayDate(LocalDate.of(2026, 5, 10));
        request.setHolidayType(HolidayType.CLOSED);

        when(storeHolidayMapper.findByDateAndLanguage(LocalDate.of(2026, 5, 10), null)).thenReturn(new StoreHoliday());

        assertThatThrownBy(() -> storeHolidayService.createHoliday(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("A holiday setting already exists for that date and language.");

        verify(storeHolidayMapper, never()).insert(any());
    }

    @Test
    void createHolidayForAllReservationsRemovesLanguageSpecificRecordsOnSameDate() {
        StoreHolidayRequest request = new StoreHolidayRequest();
        request.setHolidayDate(LocalDate.of(2026, 5, 15));
        request.setHolidayType(HolidayType.CLOSED);
        request.setReason("Closed for all reservations");

        StoreHoliday japaneseOnly = new StoreHoliday();
        japaneseOnly.setId(11L);
        japaneseOnly.setHolidayDate(LocalDate.of(2026, 5, 15));
        japaneseOnly.setAppliesToLanguage("ja");

        when(storeHolidayMapper.findByDateAndLanguage(LocalDate.of(2026, 5, 15), null)).thenReturn(null);
        when(storeHolidayMapper.findByDate(LocalDate.of(2026, 5, 15))).thenReturn(List.of(japaneseOnly));

        storeHolidayService.createHoliday(request);

        verify(storeHolidayMapper).delete(11L);
        verify(storeHolidayMapper).insert(any());
    }

    @Test
    void deleteHolidayRejectsMissingRecord() {
        when(storeHolidayMapper.delete(99L)).thenReturn(0);

        assertThatThrownBy(() -> storeHolidayService.deleteHoliday(99L))
                .isInstanceOf(java.util.NoSuchElementException.class)
                .hasMessage("The holiday setting was not found.");
    }

    @Test
    void updateHolidayToAllReservationsRemovesOtherLanguageSpecificRecords() {
        StoreHolidayRequest request = new StoreHolidayRequest();
        request.setHolidayDate(LocalDate.of(2026, 5, 15));
        request.setHolidayType(HolidayType.CLOSED);

        StoreHoliday existingShared = new StoreHoliday();
        existingShared.setId(20L);
        existingShared.setHolidayDate(LocalDate.of(2026, 5, 15));

        StoreHoliday japaneseOnly = new StoreHoliday();
        japaneseOnly.setId(21L);
        japaneseOnly.setHolidayDate(LocalDate.of(2026, 5, 15));
        japaneseOnly.setAppliesToLanguage("ja");

        when(storeHolidayMapper.findById(20L)).thenReturn(existingShared);
        when(storeHolidayMapper.findByDateAndLanguage(LocalDate.of(2026, 5, 15), null)).thenReturn(existingShared);
        when(storeHolidayMapper.findByDate(LocalDate.of(2026, 5, 15))).thenReturn(List.of(existingShared, japaneseOnly));

        storeHolidayService.updateHoliday(20L, request);

        verify(storeHolidayMapper).delete(21L);
        verify(storeHolidayMapper, times(1)).update(existingShared);
    }

    @Test
    void findMonthlyHolidaysRejectsInvalidMonth() {
        assertThatThrownBy(() -> storeHolidayService.findMonthlyHolidays(2026, 13, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Month must be between 1 and 12.");
    }
}
