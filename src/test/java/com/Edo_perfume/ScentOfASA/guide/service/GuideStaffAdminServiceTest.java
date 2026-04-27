package com.Edo_perfume.ScentOfASA.guide.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.Edo_perfume.ScentOfASA.guide.domain.GuideStaff;
import com.Edo_perfume.ScentOfASA.guide.dto.GuideStaffSummaryResponse;
import com.Edo_perfume.ScentOfASA.guide.mapper.GuideStaffMapper;

@ExtendWith(MockitoExtension.class)
class GuideStaffAdminServiceTest {

    @Mock
    private GuideStaffMapper guideStaffMapper;

    @InjectMocks
    private GuideStaffAdminService guideStaffAdminService;

    @Test
    void getActiveGuidesReturnsAllWhenLanguageIsBlank() {
        when(guideStaffMapper.findAllActive()).thenReturn(List.of(
                createGuideStaff(1L, "guide_en_1", "Alice", "en"),
                createGuideStaff(4L, "guide_ja_1", "Sato", "ja")));

        List<GuideStaffSummaryResponse> response = guideStaffAdminService.getActiveGuides(null);

        assertThat(response).hasSize(2);
        assertThat(response).extracting(GuideStaffSummaryResponse::getLoginId)
                .containsExactly("guide_en_1", "guide_ja_1");
    }

    @Test
    void getActiveGuidesFiltersByLanguage() {
        when(guideStaffMapper.findActiveByLanguage("ja")).thenReturn(List.of(
                createGuideStaff(4L, "guide_ja_1", "Sato", "ja")));

        List<GuideStaffSummaryResponse> response = guideStaffAdminService.getActiveGuides("ja");

        assertThat(response).singleElement()
                .extracting(GuideStaffSummaryResponse::getGuideLanguage)
                .isEqualTo("ja");
    }

    @Test
    void getActiveGuidesRejectsUnsupportedLanguage() {
        assertThatThrownBy(() -> guideStaffAdminService.getActiveGuides("fr"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Language must be en or ja.");
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
}
