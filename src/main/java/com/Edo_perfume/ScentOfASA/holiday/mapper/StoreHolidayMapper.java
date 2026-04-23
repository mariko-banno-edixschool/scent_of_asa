package com.Edo_perfume.ScentOfASA.holiday.mapper;

import java.time.LocalDate;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.Edo_perfume.ScentOfASA.holiday.domain.StoreHoliday;

@Mapper
public interface StoreHolidayMapper {

    List<StoreHoliday> findByMonth(@Param("startDate") LocalDate startDate,
                                   @Param("endDate") LocalDate endDate,
                                   @Param("appliesToLanguage") String appliesToLanguage);

    StoreHoliday findById(@Param("id") Long id);

    StoreHoliday findByDateAndLanguage(@Param("holidayDate") LocalDate holidayDate,
                                       @Param("appliesToLanguage") String appliesToLanguage);

    boolean existsClosedHoliday(@Param("holidayDate") LocalDate holidayDate,
                                @Param("appliesToLanguage") String appliesToLanguage);

    int insert(StoreHoliday storeHoliday);

    int update(StoreHoliday storeHoliday);

    int delete(@Param("id") Long id);

    int deleteAll();
}
