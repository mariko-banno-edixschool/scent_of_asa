package com.Edo_perfume.ScentOfASA.guide.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.Edo_perfume.ScentOfASA.guide.domain.GuideStaff;
import com.Edo_perfume.ScentOfASA.guide.dto.GuideScheduleMonthResponse;
import com.Edo_perfume.ScentOfASA.guide.dto.GuideSlotAssignmentRequest;
import com.Edo_perfume.ScentOfASA.guide.mapper.GuideStaffMapper;
import com.Edo_perfume.ScentOfASA.holiday.dto.HolidayCalendarDayResponse;
import com.Edo_perfume.ScentOfASA.holiday.service.StoreHolidayService;
import com.Edo_perfume.ScentOfASA.slot.domain.AdminSlot;
import com.Edo_perfume.ScentOfASA.slot.dto.AdminSlotResponse;
import com.Edo_perfume.ScentOfASA.slot.mapper.AdminSlotMapper;
import com.Edo_perfume.ScentOfASA.slot.service.AdminSlotService;

@ExtendWith(MockitoExtension.class)
class GuideScheduleServiceTest {

    @Mock
    private GuideStaffMapper guideStaffMapper;

    @Mock
    private AdminSlotMapper adminSlotMapper;

    @Mock
    private AdminSlotService adminSlotService;

    @Mock
    private StoreHolidayService storeHolidayService;

    @InjectMocks
    private GuideScheduleService guideScheduleService;

    @Test
    void getMonthlyScheduleReturnsOnlyOwnLanguageSlots() {
        GuideStaff guide = createGuideStaff(1L, "guide_en_1", "Alice", "en");
        AdminSlot slot = createSlot(10L, LocalDate.of(2026, 5, 22), "11:00", "en", null, null, "STOPPED");

        when(guideStaffMapper.findByLoginId("guide_en_1")).thenReturn(guide);
        when(adminSlotMapper.findByMonthAndLanguage(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31), "en"))
                .thenReturn(List.of(slot));
        when(storeHolidayService.findMonthlyHolidays(2026, 5, null)).thenReturn(List.of());

        GuideScheduleMonthResponse response = guideScheduleService.getMonthlySchedule("guide_en_1", 2026, 5);

        verify(adminSlotService).ensureMonthlySlots(2026, 5);
        assertThat(response.getGuide().getLoginId()).isEqualTo("guide_en_1");
        assertThat(response.getDays()).hasSize(31);
        assertThat(response.getDays().stream()
                .filter(day -> day.getDate().equals(LocalDate.of(2026, 5, 22)))
                .findFirst()
                .orElseThrow()
                .getSlots())
                .extracting(AdminSlotResponse::getGuideLanguage)
                .containsOnly("en");
    }

    @Test
    void updateOwnSlotAssignsGuideToSlot() {
        GuideStaff guide = createGuideStaff(1L, "guide_ja_1", "Sato", "ja");
        AdminSlot slot = createSlot(10L, LocalDate.of(2026, 5, 22), "11:00", "ja", null, null, "STOPPED");
        GuideSlotAssignmentRequest request = new GuideSlotAssignmentRequest();
        request.setLoginId("guide_ja_1");
        request.setAssigned(true);

        when(guideStaffMapper.findByLoginId("guide_ja_1")).thenReturn(guide);
        when(adminSlotMapper.findById(10L)).thenReturn(slot);
        when(storeHolidayService.findMonthlyHolidays(2026, 5, null)).thenReturn(List.of());

        AdminSlotResponse response = guideScheduleService.updateOwnSlot(10L, request);

        assertThat(slot.getGuideStaffId()).isEqualTo(1L);
        assertThat(slot.getGuideName()).isEqualTo("Sato");
        assertThat(slot.getSlotStatus()).isEqualTo("OPEN");
        assertThat(response.getEffectiveStatus()).isEqualTo("OPEN");
        verify(adminSlotMapper).update(eq(slot));
    }

    @Test
    void updateOwnSlotRejectsOtherGuidesAssignment() {
        GuideStaff guide = createGuideStaff(1L, "guide_ja_1", "Sato", "ja");
        AdminSlot slot = createSlot(10L, LocalDate.of(2026, 5, 22), "11:00", "ja", 2L, "Tanaka", "OPEN");
        GuideSlotAssignmentRequest request = new GuideSlotAssignmentRequest();
        request.setLoginId("guide_ja_1");
        request.setAssigned(true);

        when(guideStaffMapper.findByLoginId("guide_ja_1")).thenReturn(guide);
        when(adminSlotMapper.findById(10L)).thenReturn(slot);

        assertThatThrownBy(() -> guideScheduleService.updateOwnSlot(10L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("This slot is already assigned to another guide.");
    }

    @Test
    void updateOwnSlotClearsAssignmentAndStopsSlot() {
        GuideStaff guide = createGuideStaff(1L, "guide_ja_1", "Sato", "ja");
        AdminSlot slot = createSlot(10L, LocalDate.of(2026, 5, 22), "11:00", "ja", 1L, "Sato", "OPEN");
        GuideSlotAssignmentRequest request = new GuideSlotAssignmentRequest();
        request.setLoginId("guide_ja_1");
        request.setAssigned(false);

        when(guideStaffMapper.findByLoginId("guide_ja_1")).thenReturn(guide);
        when(adminSlotMapper.findById(10L)).thenReturn(slot);
        when(storeHolidayService.findMonthlyHolidays(2026, 5, null)).thenReturn(List.of());

        AdminSlotResponse response = guideScheduleService.updateOwnSlot(10L, request);

        assertThat(slot.getGuideStaffId()).isNull();
        assertThat(slot.getGuideName()).isNull();
        assertThat(slot.getSlotStatus()).isEqualTo("STOPPED");
        assertThat(response.getEffectiveStatus()).isEqualTo("STOPPED");
    }

    private GuideStaff createGuideStaff(Long id, String loginId, String displayName, String guideLanguage) {
        GuideStaff guideStaff = new GuideStaff();
        guideStaff.setId(id);
        guideStaff.setLoginId(loginId);
        guideStaff.setDisplayName(displayName);
        guideStaff.setGuideLanguage(guideLanguage);
        guideStaff.setActive(true);
        return guideStaff;
    }

    private AdminSlot createSlot(Long id, LocalDate date, String timeSlot, String language,
                                 Long guideStaffId, String guideName, String status) {
        AdminSlot slot = new AdminSlot();
        slot.setId(id);
        slot.setSlotDate(date);
        slot.setTimeSlot(timeSlot);
        slot.setGuideLanguage(language);
        slot.setGuideStaffId(guideStaffId);
        slot.setGuideName(guideName);
        slot.setSlotStatus(status);
        slot.setCreatedAt(LocalDateTime.of(2026, 4, 27, 12, 0));
        slot.setUpdatedAt(LocalDateTime.of(2026, 4, 27, 12, 0));
        return slot;
    }
}
