package com.Edo_perfume.ScentOfASA.slot.mapper;

import java.time.LocalDate;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.Edo_perfume.ScentOfASA.slot.domain.AdminSlot;

@Mapper
public interface AdminSlotMapper {

    List<AdminSlot> findByMonth(@Param("startDate") LocalDate startDate,
                                @Param("endDate") LocalDate endDate);

    List<AdminSlot> findByMonthAndLanguage(@Param("startDate") LocalDate startDate,
                                           @Param("endDate") LocalDate endDate,
                                           @Param("guideLanguage") String guideLanguage);

    AdminSlot findById(@Param("id") Long id);

    AdminSlot findByDateTimeAndLanguage(@Param("slotDate") LocalDate slotDate,
                                        @Param("timeSlot") String timeSlot,
                                        @Param("guideLanguage") String guideLanguage);

    int insert(AdminSlot adminSlot);

    int update(AdminSlot adminSlot);
}
