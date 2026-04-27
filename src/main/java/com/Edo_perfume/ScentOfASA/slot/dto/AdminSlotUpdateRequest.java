package com.Edo_perfume.ScentOfASA.slot.dto;

public class AdminSlotUpdateRequest {

    private Long guideStaffId;
    private String guideName;
    private String slotStatus;

    public Long getGuideStaffId() {
        return guideStaffId;
    }

    public void setGuideStaffId(Long guideStaffId) {
        this.guideStaffId = guideStaffId;
    }

    public String getGuideName() {
        return guideName;
    }

    public void setGuideName(String guideName) {
        this.guideName = guideName;
    }

    public String getSlotStatus() {
        return slotStatus;
    }

    public void setSlotStatus(String slotStatus) {
        this.slotStatus = slotStatus;
    }
}
