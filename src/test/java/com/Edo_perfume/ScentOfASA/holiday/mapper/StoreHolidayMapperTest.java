package com.Edo_perfume.ScentOfASA.holiday.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;

import com.Edo_perfume.ScentOfASA.holiday.domain.HolidayType;
import com.Edo_perfume.ScentOfASA.holiday.domain.StoreHoliday;

@SpringBootTest
@ActiveProfiles("h2")
class StoreHolidayMapperTest {

    @Autowired
    private StoreHolidayMapper storeHolidayMapper;

    @BeforeEach
    void setUp() {
        storeHolidayMapper.deleteAll();
    }

    @Test
    void insertAndFindByMonthReturnsPersistedHoliday() {
        StoreHoliday holiday = new StoreHoliday();
        holiday.setHolidayDate(LocalDate.of(2026, 5, 7));
        holiday.setHolidayType(HolidayType.CLOSED);
        holiday.setReason("Regular maintenance");
        holiday.setAppliesToLanguage("ja");
        holiday.setCreatedByStaffId(1L);
        holiday.setCreatedAt(LocalDateTime.of(2026, 4, 23, 12, 0));
        holiday.setUpdatedAt(LocalDateTime.of(2026, 4, 23, 12, 0));

        int inserted = storeHolidayMapper.insert(holiday);
        List<StoreHoliday> holidays = storeHolidayMapper.findByMonth(
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 31),
                "ja"
        );

        assertThat(inserted).isEqualTo(1);
        assertThat(holiday.getId()).isNotNull();
        assertThat(holidays).hasSize(1);
        assertThat(holidays.get(0).getReason()).isEqualTo("Regular maintenance");
        assertThat(holidays.get(0).getCreatedAt()).isEqualTo(LocalDateTime.of(2026, 4, 23, 12, 0));
    }

    @Test
    void existsClosedHolidayMatchesSharedLanguageRecords() {
        StoreHoliday holiday = new StoreHoliday();
        holiday.setHolidayDate(LocalDate.of(2026, 5, 8));
        holiday.setHolidayType(HolidayType.CLOSED);
        holiday.setReason("Shared holiday");
        holiday.setCreatedAt(LocalDateTime.of(2026, 4, 23, 12, 0));
        holiday.setUpdatedAt(LocalDateTime.of(2026, 4, 23, 12, 0));

        storeHolidayMapper.insert(holiday);

        boolean closedForJa = storeHolidayMapper.existsClosedHoliday(LocalDate.of(2026, 5, 8), "ja");
        boolean closedForEn = storeHolidayMapper.existsClosedHoliday(LocalDate.of(2026, 5, 8), "en");

        assertThat(closedForJa).isTrue();
        assertThat(closedForEn).isTrue();
    }
}
