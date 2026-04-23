package com.Edo_perfume.ScentOfASA.holiday.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.DayOfWeek;
import java.time.LocalDate;
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
    void applyWeeklyRuleSkipsExceptionDates() {
        HolidayRuleApplyRequest request = new HolidayRuleApplyRequest();
        request.setYear(2026);
        request.setMonth(6);
        request.setWeeklyClosedDay(DayOfWeek.MONDAY);
        request.setOpenExceptionDates(List.of(LocalDate.of(2026, 6, 1)));

        when(storeHolidayMapper.findByDateAndLanguage(any(LocalDate.class), any())).thenReturn(null);

        List<HolidayCalendarDayResponse> result = storeHolidayService.applyWeeklyRule(request);

        assertThat(result).extracting(HolidayCalendarDayResponse::getHolidayDate)
                .containsExactly(
                        LocalDate.of(2026, 6, 8),
                        LocalDate.of(2026, 6, 15),
                        LocalDate.of(2026, 6, 22),
                        LocalDate.of(2026, 6, 29)
                );
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
}
