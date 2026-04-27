package com.Edo_perfume.ScentOfASA.guide.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.Edo_perfume.ScentOfASA.guide.domain.GuideStaff;

@Mapper
public interface GuideStaffMapper {

    GuideStaff findById(@Param("id") Long id);

    GuideStaff findByLoginId(@Param("loginId") String loginId);

    List<GuideStaff> findAllActive();

    List<GuideStaff> findActiveByLanguage(@Param("guideLanguage") String guideLanguage);
}
