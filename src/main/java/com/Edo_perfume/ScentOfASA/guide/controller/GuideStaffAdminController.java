package com.Edo_perfume.ScentOfASA.guide.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.Edo_perfume.ScentOfASA.guide.dto.GuideStaffSummaryResponse;
import com.Edo_perfume.ScentOfASA.guide.service.GuideStaffAdminService;

@RestController
@RequestMapping("/api/admin/guide-staff")
public class GuideStaffAdminController {

    private final GuideStaffAdminService guideStaffAdminService;

    public GuideStaffAdminController(GuideStaffAdminService guideStaffAdminService) {
        this.guideStaffAdminService = guideStaffAdminService;
    }

    @GetMapping
    public List<GuideStaffSummaryResponse> getActiveGuides(@RequestParam(required = false) String language) {
        return guideStaffAdminService.getActiveGuides(language);
    }
}
