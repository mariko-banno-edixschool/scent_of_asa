package com.Edo_perfume.ScentOfASA.guide.service;

import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.Edo_perfume.ScentOfASA.guide.domain.GuideStaff;
import com.Edo_perfume.ScentOfASA.guide.dto.GuideStaffSummaryResponse;
import com.Edo_perfume.ScentOfASA.guide.mapper.GuideStaffMapper;

@Service
@Transactional(readOnly = true)
public class GuideStaffAdminService {

    private final GuideStaffMapper guideStaffMapper;

    public GuideStaffAdminService(GuideStaffMapper guideStaffMapper) {
        this.guideStaffMapper = guideStaffMapper;
    }

    public List<GuideStaffSummaryResponse> getActiveGuides(String language) {
        List<GuideStaff> guides;
        if (language == null || language.isBlank()) {
            guides = guideStaffMapper.findAllActive();
        } else {
            String normalizedLanguage = language.trim().toLowerCase(Locale.ROOT);
            if (!List.of("en", "ja").contains(normalizedLanguage)) {
                throw new IllegalArgumentException("Language must be en or ja.");
            }
            guides = guideStaffMapper.findActiveByLanguage(normalizedLanguage);
        }

        return guides.stream()
                .map(guide -> new GuideStaffSummaryResponse(
                        guide.getId(),
                        guide.getLoginId(),
                        guide.getDisplayName(),
                        guide.getGuideLanguage()))
                .toList();
    }
}
