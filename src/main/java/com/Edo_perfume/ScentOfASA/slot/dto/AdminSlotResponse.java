package com.Edo_perfume.ScentOfASA.slot.dto;

public class AdminSlotResponse {

    private Long id;
    private String timeSlot;
    private String guideLanguage;
    private String guideName;
    private String slotStatus;
    private String effectiveStatus;
    private boolean available;

    public AdminSlotResponse() {
    }

    public AdminSlotResponse(Long id, String timeSlot, String guideLanguage, String guideName,
                             String slotStatus, String effectiveStatus, boolean available) {
        this.id = id;
        this.timeSlot = timeSlot;
        this.guideLanguage = guideLanguage;
        this.guideName = guideName;
        this.slotStatus = slotStatus;
        this.effectiveStatus = effectiveStatus;
        this.available = available;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTimeSlot() {
        return timeSlot;
    }

    public void setTimeSlot(String timeSlot) {
        this.timeSlot = timeSlot;
    }

    public String getGuideLanguage() {
        return guideLanguage;
    }

    public void setGuideLanguage(String guideLanguage) {
        this.guideLanguage = guideLanguage;
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

    public String getEffectiveStatus() {
        return effectiveStatus;
    }

    public void setEffectiveStatus(String effectiveStatus) {
        this.effectiveStatus = effectiveStatus;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }
}
