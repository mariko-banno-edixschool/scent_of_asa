package com.Edo_perfume.ScentOfASA.guide.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.Edo_perfume.ScentOfASA.guide.dto.GuideProfileResponse;
import com.Edo_perfume.ScentOfASA.guide.dto.GuideScheduleMonthResponse;
import com.Edo_perfume.ScentOfASA.guide.dto.GuideSlotAssignmentRequest;
import com.Edo_perfume.ScentOfASA.guide.service.GuideScheduleService;
import com.Edo_perfume.ScentOfASA.slot.dto.AdminSlotResponse;

@RestController
@RequestMapping("/api/guide")
public class GuideScheduleController {

    private final GuideScheduleService guideScheduleService;

    public GuideScheduleController(GuideScheduleService guideScheduleService) {
        this.guideScheduleService = guideScheduleService;
    }

    @GetMapping("/me")
    public GuideProfileResponse getCurrentGuide(@RequestParam String loginId) {
        return guideScheduleService.getCurrentGuide(loginId);
    }

    @GetMapping("/slots")
    public GuideScheduleMonthResponse getMonthlySchedule(@RequestParam String loginId,
                                                         @RequestParam int year,
                                                         @RequestParam int month) {
        return guideScheduleService.getMonthlySchedule(loginId, year, month);
    }

    @PutMapping("/slots/{id}")
    public AdminSlotResponse updateOwnSlot(@PathVariable Long id,
                                           @RequestBody GuideSlotAssignmentRequest request) {
        return guideScheduleService.updateOwnSlot(id, request);
    }
}
