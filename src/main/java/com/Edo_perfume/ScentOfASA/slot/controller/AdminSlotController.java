package com.Edo_perfume.ScentOfASA.slot.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.Edo_perfume.ScentOfASA.slot.dto.AdminSlotMonthResponse;
import com.Edo_perfume.ScentOfASA.slot.dto.AdminSlotResponse;
import com.Edo_perfume.ScentOfASA.slot.dto.AdminSlotUpdateRequest;
import com.Edo_perfume.ScentOfASA.slot.service.AdminSlotService;

@RestController
@RequestMapping("/api/admin/slots")
public class AdminSlotController {

    private final AdminSlotService adminSlotService;

    public AdminSlotController(AdminSlotService adminSlotService) {
        this.adminSlotService = adminSlotService;
    }

    @GetMapping
    public AdminSlotMonthResponse getMonthlySlots(@RequestParam int year,
                                                  @RequestParam int month) {
        return adminSlotService.getMonthlySlots(year, month);
    }

    @PutMapping("/{id}")
    public AdminSlotResponse updateSlot(@PathVariable Long id,
                                        @RequestBody AdminSlotUpdateRequest request) {
        return adminSlotService.updateSlot(id, request);
    }
}
