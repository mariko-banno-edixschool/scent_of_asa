package com.Edo_perfume.ScentOfASA.reservation.mapper;

import java.time.LocalDate;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.Edo_perfume.ScentOfASA.reservation.domain.PublicReservation;

@Mapper
public interface PublicReservationMapper {

    List<PublicReservation> findByMonth(@Param("startDate") LocalDate startDate,
                                        @Param("endDate") LocalDate endDate,
                                        @Param("guideLanguage") String guideLanguage);

    boolean existsByDateAndTime(@Param("reservationDate") LocalDate reservationDate,
                                @Param("timeSlot") String timeSlot,
                                @Param("guideLanguage") String guideLanguage);

    int insert(PublicReservation reservation);
}
