package com.Edo_perfume.ScentOfASA.guide.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.Edo_perfume.ScentOfASA.guide.dto.GuideStaffSummaryResponse;
import com.Edo_perfume.ScentOfASA.guide.service.GuideStaffAdminService;

@WebMvcTest(GuideStaffAdminController.class)
@Import(GuideScheduleApiExceptionHandler.class)
class GuideStaffAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GuideStaffAdminService guideStaffAdminService;

    @Test
    void getActiveGuidesReturnsGuideList() throws Exception {
        when(guideStaffAdminService.getActiveGuides("ja"))
                .thenReturn(List.of(new GuideStaffSummaryResponse(4L, "guide_ja_1", "Sato", "ja")));

        mockMvc.perform(get("/api/admin/guide-staff").param("language", "ja"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].id").value(4L))
                .andExpect(jsonPath("$[0].loginId").value("guide_ja_1"))
                .andExpect(jsonPath("$[0].guideLanguage").value("ja"));
    }
}
