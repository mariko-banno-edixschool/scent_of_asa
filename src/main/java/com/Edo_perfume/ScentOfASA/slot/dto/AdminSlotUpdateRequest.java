package com.Edo_perfume.ScentOfASA.slot.dto;

public class AdminSlotUpdateRequest {

    private String guideName;
    private String slotStatus;

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
