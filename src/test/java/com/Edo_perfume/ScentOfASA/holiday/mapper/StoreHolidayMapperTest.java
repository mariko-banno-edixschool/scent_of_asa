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

    @Test
    void findByMonthWithAllReservationsIncludesLanguageSpecificRecords() {
        StoreHoliday englishOnly = new StoreHoliday();
        englishOnly.setHolidayDate(LocalDate.of(2026, 5, 13));
        englishOnly.setHolidayType(HolidayType.CLOSED);
        englishOnly.setReason("English booking only");
        englishOnly.setAppliesToLanguage("en");
        englishOnly.setCreatedAt(LocalDateTime.of(2026, 4, 23, 12, 0));
        englishOnly.setUpdatedAt(LocalDateTime.of(2026, 4, 23, 12, 0));

        StoreHoliday japaneseOnly = new StoreHoliday();
        japaneseOnly.setHolidayDate(LocalDate.of(2026, 5, 14));
        japaneseOnly.setHolidayType(HolidayType.CLOSED);
        japaneseOnly.setReason("Japanese booking only");
        japaneseOnly.setAppliesToLanguage("ja");
        japaneseOnly.setCreatedAt(LocalDateTime.of(2026, 4, 23, 12, 5));
        japaneseOnly.setUpdatedAt(LocalDateTime.of(2026, 4, 23, 12, 5));

        storeHolidayMapper.insert(englishOnly);
        storeHolidayMapper.insert(japaneseOnly);

        List<StoreHoliday> holidays = storeHolidayMapper.findByMonth(
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 31),
                null
        );

        assertThat(holidays)
                .extracting(StoreHoliday::getAppliesToLanguage)
                .contains("en", "ja");
    }
}
